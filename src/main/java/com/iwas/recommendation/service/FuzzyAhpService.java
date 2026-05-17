package com.iwas.recommendation.service;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import com.iwas.recommendation.dto.TfnComparison;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Pure, stateless implementation of Fuzzy AHP using Chang's extent analysis.
 *
 * <p>Inputs are Triangular Fuzzy Numbers (TFN) on the Saaty scale (each component
 * in {@code [1/9, 9]}, ordered {@code l <= m <= u}). The service:
 * <ul>
 *   <li>builds full reciprocal n×n matrices from the user's pairwise inputs,</li>
 *   <li>derives a normalized weight vector via Chang's extent analysis,</li>
 *   <li>computes a Consistency Ratio on the defuzzified crisp matrix.</li>
 * </ul>
 *
 * <p>All public methods are deterministic and do not touch the database.
 */
@Service
public class FuzzyAhpService {

    private static final BigDecimal MIN_TFN = new BigDecimal("0.1111");
    private static final BigDecimal MAX_TFN = new BigDecimal("9.0");

    // ─── public types ─────────────────────────────────────────────────────────

    /** Internal TFN representation using primitives for speed. */
    public record Tfn(double l, double m, double u) {
        public Tfn reciprocal() {
            return new Tfn(1.0 / u, 1.0 / m, 1.0 / l);
        }
        public static Tfn one() {
            return new Tfn(1.0, 1.0, 1.0);
        }
        public static Tfn fromDto(TfnComparison t) {
            return new Tfn(t.getL().doubleValue(),
                           t.getM().doubleValue(),
                           t.getU().doubleValue());
        }
    }

    // ─── validation ───────────────────────────────────────────────────────────

    /**
     * Verifies the cross-field invariant {@code l <= m <= u} and that each
     * component is within the Saaty range {@code [1/9, 9]}.
     */
    public void validateTfn(TfnComparison t) {
        if (t == null || t.getL() == null || t.getM() == null || t.getU() == null) {
            throw new AppException(ErrorCode.AHP_INVALID_TFN);
        }
        if (t.getL().compareTo(t.getM()) > 0 || t.getM().compareTo(t.getU()) > 0) {
            throw new AppException(ErrorCode.AHP_INVALID_TFN);
        }
        if (t.getL().compareTo(MIN_TFN) < 0 || t.getU().compareTo(MAX_TFN) > 0) {
            throw new AppException(ErrorCode.AHP_INVALID_TFN);
        }
    }

    // ─── matrix builders ──────────────────────────────────────────────────────

    /**
     * Build the top-level 3×3 reciprocal matrix in order
     * {@code [Skill, Workload, On-time]}.
     */
    public Tfn[][] buildTopMatrix(TfnComparison sw, TfnComparison so, TfnComparison wo) {
        Tfn tSw = Tfn.fromDto(sw);
        Tfn tSo = Tfn.fromDto(so);
        Tfn tWo = Tfn.fromDto(wo);
        Tfn[][] m = new Tfn[3][3];
        m[0][0] = Tfn.one();
        m[1][1] = Tfn.one();
        m[2][2] = Tfn.one();
        m[0][1] = tSw;  m[1][0] = tSw.reciprocal();
        m[0][2] = tSo;  m[2][0] = tSo.reciprocal();
        m[1][2] = tWo;  m[2][1] = tWo.reciprocal();
        return m;
    }

    /**
     * Build the Skill 2×2 reciprocal sub-matrix in order {@code [Coverage, Level]}.
     */
    public Tfn[][] buildSkillMatrix(TfnComparison cl) {
        Tfn tCl = Tfn.fromDto(cl);
        Tfn[][] m = new Tfn[2][2];
        m[0][0] = Tfn.one();
        m[1][1] = Tfn.one();
        m[0][1] = tCl;
        m[1][0] = tCl.reciprocal();
        return m;
    }

    // ─── Chang's extent analysis ──────────────────────────────────────────────

    /**
     * Chang's extent analysis. Returns a length-n weight vector summing to 1.0.
     */
    public double[] chang(Tfn[][] m) {
        int n = m.length;

        // Step 1-2: fuzzy row sums and grand sum
        double[] rsL = new double[n];
        double[] rsM = new double[n];
        double[] rsU = new double[n];
        double sumL = 0, sumM = 0, sumU = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                rsL[i] += m[i][j].l();
                rsM[i] += m[i][j].m();
                rsU[i] += m[i][j].u();
            }
            sumL += rsL[i];
            sumM += rsM[i];
            sumU += rsU[i];
        }

        // Step 3: synthetic extent S_i = (rsL/sumU, rsM/sumM, rsU/sumL)
        Tfn[] s = new Tfn[n];
        for (int i = 0; i < n; i++) {
            s[i] = new Tfn(rsL[i] / sumU, rsM[i] / sumM, rsU[i] / sumL);
        }

        // Step 4-5: degree of possibility V(S_i >= S_j); d'(A_i) = min over j != i
        double[] d = new double[n];
        Arrays.fill(d, Double.POSITIVE_INFINITY);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double v = possibility(s[i], s[j]);
                if (v < d[i]) d[i] = v;
            }
        }

        // Step 6: normalize
        double total = 0;
        for (double v : d) total += v;
        double[] w = new double[n];
        if (total > 0) {
            for (int i = 0; i < n; i++) w[i] = d[i] / total;
        } else {
            // Degenerate: every d'(A_i) = 0. Fall back to equal weights.
            Arrays.fill(w, 1.0 / n);
        }
        return w;
    }

    /**
     * Degree of possibility V(A >= B) for two TFNs.
     * <pre>
     *   1                                            if m_A >= m_B
     *   0                                            if l_B >= u_A
     *   (l_B - u_A) / ((m_A - u_A) - (m_B - l_B))    otherwise
     * </pre>
     */
    private static double possibility(Tfn a, Tfn b) {
        if (a.m() >= b.m()) return 1.0;
        if (b.l() >= a.u()) return 0.0;
        double denom = (a.m() - a.u()) - (b.m() - b.l());
        if (denom == 0.0) return 0.0;
        return (b.l() - a.u()) / denom;
    }

    // ─── Consistency Ratio ────────────────────────────────────────────────────

    /**
     * Consistency Ratio of a TFN matrix, defuzzified by taking the {@code m}
     * component. Returns 0 for n <= 2 since any 2×2 reciprocal matrix is
     * perfectly consistent by construction.
     */
    public double consistencyRatio(Tfn[][] m) {
        int n = m.length;
        if (n <= 2) return 0.0;

        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = m[i][j].m();
            }
        }

        // Priority vector via geometric mean of rows
        double[] w = new double[n];
        double wSum = 0;
        for (int i = 0; i < n; i++) {
            double prod = 1.0;
            for (int j = 0; j < n; j++) prod *= a[i][j];
            w[i] = Math.pow(prod, 1.0 / n);
            wSum += w[i];
        }
        for (int i = 0; i < n; i++) w[i] /= wSum;

        // λ_max = (1/n) Σ (Aw)_i / w_i
        double lambdaMax = 0;
        for (int i = 0; i < n; i++) {
            double aw = 0;
            for (int j = 0; j < n; j++) aw += a[i][j] * w[j];
            lambdaMax += aw / w[i];
        }
        lambdaMax /= n;

        double ci = (lambdaMax - n) / (n - 1.0);
        double ri = randomIndex(n);
        return ri == 0.0 ? 0.0 : ci / ri;
    }

    /** Saaty's random index table; values past n=10 are treated as 1.49. */
    private static double randomIndex(int n) {
        return switch (n) {
            case 1, 2 -> 0.0;
            case 3 -> 0.58;
            case 4 -> 0.90;
            case 5 -> 1.12;
            case 6 -> 1.24;
            case 7 -> 1.32;
            case 8 -> 1.41;
            case 9 -> 1.45;
            default -> 1.49;
        };
    }
}
