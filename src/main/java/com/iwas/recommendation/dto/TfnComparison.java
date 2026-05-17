package com.iwas.recommendation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Triangular Fuzzy Number used as a pairwise comparison input in Fuzzy AHP.
 * <p>The Saaty fuzzy scale puts each component in {@code [1/9, 9]}; the
 * cross-field invariant {@code l <= m <= u} is verified service-side.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TfnComparison {

    @NotNull
    @DecimalMin(value = "0.1111", message = "l must be >= 1/9")
    @DecimalMax(value = "9.0", message = "l must be <= 9")
    private BigDecimal l;

    @NotNull
    @DecimalMin(value = "0.1111", message = "m must be >= 1/9")
    @DecimalMax(value = "9.0", message = "m must be <= 9")
    private BigDecimal m;

    @NotNull
    @DecimalMin(value = "0.1111", message = "u must be >= 1/9")
    @DecimalMax(value = "9.0", message = "u must be <= 9")
    private BigDecimal u;
}
