# Task Arrangement Module — Implementation Spec

> Module gợi ý **thứ tự xử lý task** cho từng member, tối ưu theo tổng tardiness có trọng số, dùng heuristic **ATC (Apparent Tardiness Cost)**.
> Tài liệu này là spec để implement. Mọi quyết định thiết kế đã chốt đều ghi rõ trạng thái (CORE / OUT-OF-SCOPE / OPTIONAL EXTENSION).

---

## 1. Mục tiêu & phạm vi

Cho hàng đợi task của **một** member, sinh ra một **thứ tự xử lý** (sequence) nhằm tối thiểu hóa tổng độ trễ có trọng số.

- **Bài toán (ký hiệu Graham):** `1 ‖ Σ wⱼTⱼ` — single machine, total weighted tardiness.
- **Độ phức tạp:** strongly NP-hard (Lawler 1977; Lenstra, Rinnooy Kan & Brucker 1977) → KHÔNG giải tối ưu, dùng heuristic.
- **Phương pháp:** luật ưu tiên ATC (Vepsalainen & Morton, 1987) — greedy construction heuristic.

**Đây KHÔNG phải lời giải tối ưu.** Là heuristic hiệu năng cao. Mọi tên hàm/biến/comment phải phản ánh đúng điều này (không dùng từ "optimal").

---

## 2. Giả định & ràng buộc (CORE — không thương lượng ở bản đầu)

| Giả định | Giá trị | Hệ quả |
|---|---|---|
| Số máy | 1 member = 1 hàng đợi độc lập | Không có assignment giữa members |
| Preemption | **Non-preemptive** | Task đã bắt đầu thì chạy liên tục tới xong |
| Idling | **Non-idling** | Member rảnh + có task eligible → làm ngay, không cố tình chờ |
| Release date | **Không có** (mọi task sẵn sàng ngay) | `start_date` KHÔNG vào engine (xem §4) |
| Processing time | Biết trước qua `estimate` | Giả định `pⱼ` cho trước (có thể sai số — xem §10) |

---

## 3. Input — trường ĐƯA VÀO engine

Ba trường, ánh xạ trực tiếp sang tham số scheduling:

| Trường nghiệp vụ | Ký hiệu | Kiểu | Ghi chú |
|---|---|---|---|
| `due_date` | `dⱼ` | timestamp | Deadline |
| `estimate` | `pⱼ` | số giờ (>0) | Thời lượng xử lý dự kiến |
| `priority` | `wⱼ` | enum → số | Tra qua bảng trọng số bên dưới |

### Bảng trọng số priority → `wⱼ` (cấu hình được)

```
WEIGHTS = {
  CRITICAL: 8,
  HIGH:     4,
  MEDIUM:   2,
  LOW:      1,
}
```

> **QUAN TRỌNG — đây là núm điều khiển hành vi:** *tỉ lệ* giữa các trọng số quyết định hệ nghiêng về lexicographic hay weighted.
> - Tỉ lệ lớn (vd CRITICAL=1000) → CRITICAL áp đảo, hành xử như **lexicographic cứng** (không bao giờ đánh đổi).
> - Tỉ lệ vừa (vd 8/4/2/1) → cho phép **trade-off mềm** (task gấp + nhanh có thể vượt task quan trọng còn nhiều slack).
> Bảng này PHẢI để cấu hình được (config/DB), không hard-code, để chạy sensitivity analysis.

---

## 4. Trường TỒN TẠI nhưng KHÔNG vào engine (OUT-OF-SCOPE của thuật toán)

| Trường | Vai trò | Vì sao ngoài engine |
|---|---|---|
| `start_date` | Planned/expected start; phục vụ Gantt, timeline, đo trượt tiến độ | Không phải release constraint. Hàm mục tiêu `ΣwⱼTⱼ` chỉ phụ thuộc `due_date`, không phụ thuộc ngày bắt đầu. Engine có thể *sinh ra* planned start như OUTPUT (xem §7). |
| `actualHours` | Log time burn cuối ngày, báo cáo tiến độ | Non-preemptive nên không đổi quyết định chọn task. Dùng cho forecast/đánh giá (xem §10). |

Engine **không đọc** hai trường này trong logic xếp thứ tự.

---

## 5. Thuật toán lõi — ATC

### 5.1. Chỉ số ưu tiên

Tại thời điểm `t` (số giờ đã trôi kể từ mốc bắt đầu xếp lịch `t0`), với mỗi task `j` chưa xử lý:

```
I_j(t) = (w_j / p_j) * exp( -max(0, slack_j) / (k * p_bar) )

trong đó  slack_j = d_j - p_j - t
```

| Ký hiệu | Ý nghĩa | Nguồn |
|---|---|---|
| `w_j / p_j` | Mật độ giá trị (value density) — thừa số WSPT | Smith's rule (1956), tối ưu cho `ΣwⱼCⱼ` |
| `slack_j` | Quỹ thời gian dư trước thời điểm muộn nhất phải bắt đầu để kịp hạn | |
| `exp(...)` | Hệ số khẩn ∈ (0,1]: =1 khi `slack ≤ 0` (phải làm ngay), →0 khi slack lớn | |
| `k` | Lookahead — núm trượt giữa "thiên giá trị" (k lớn → WSPT) và "thiên khẩn" (k nhỏ → EDD) | |
| `p_bar` | Estimate trung bình, chuẩn hóa thang slack | |

Chọn task có `I_j(t)` **lớn nhất**.

### 5.2. Quy đổi đơn vị (BẮT BUỘC làm rõ khi implement)

Mọi đại lượng thời gian phải cùng đơn vị (**giờ**), quy về mốc `t0` (thời điểm gọi thuật toán):

```
d_j = giờ_giữa(t0, due_date_j)     # due_date quy ra "số giờ kể từ t0"
p_j = estimate_j                    # đã là giờ
t   = số giờ đã trôi trong sequence đang xây
```

Nếu hệ thống tính theo ngày làm việc (8h/ngày, trừ cuối tuần/nghỉ lễ) thì `giờ_giữa()` phải dùng **lịch làm việc**, không phải giờ đồng hồ thô. Ghi rõ lựa chọn này trong code.

### 5.3. Tham số

| Tham số | Mặc định | Ghi chú |
|---|---|---|
| `k` | `2.0` | Tinh chỉnh bằng grid search (xem §9). Khoảng hợp lý 0.5–4.5 (Vepsalainen & Morton: 1.5–4.5; Holsenback: 0.5–2.0) |
| `p_bar` | mean(estimate) over toàn bộ input tại lúc gọi (**static**) | Tính 1 lần, không tính lại mỗi vòng. Guard: nếu `p_bar == 0` → đặt `= 1` |

### 5.4. Pseudocode — construction heuristic (chế độ STATIC, deliverable chính)

```
function arrange(tasks, config):
    # tasks: [{ id, due_date, estimate, priority, ... }]
    # config: { weights, k }

    EPS = 1e-9
    U = copy(tasks)
    p_bar = mean(task.estimate for task in U)
    if p_bar <= 0: p_bar = 1
    t = 0.0                          # giờ đã trôi
    sequence = []

    while U not empty:
        for j in U:
            w_j = config.weights[j.priority]
            p_j = max(j.estimate, EPS)              # guard chia 0
            d_j = hours_from(t0, j.due_date)        # có thể âm nếu đã quá hạn
            slack = d_j - p_j - t
            urgency = exp(-max(0.0, slack) / (config.k * p_bar))
            j._index = (w_j / p_j) * urgency

        j_star = select_max(U)                       # xem tie-break §6

        # ghi lại thông tin để giải thích (explainability)
        j_star.projected_start    = t0 + hours(t)
        t = t + j_star.estimate                      # non-preemptive: chạy tới xong
        j_star.projected_finish   = t0 + hours(t)
        d_star = hours_from(t0, j_star.due_date)
        j_star.projected_tardiness = max(0.0, t - d_star)
        j_star.position = len(sequence)
        j_star.reason = build_reason(j_star, slack_of(j_star), ...)  # §7

        sequence.append(j_star)
        remove(U, j_star)

    return sequence
```

### 5.5. Chế độ DISPATCHING (online — cho hệ live)

Cùng chỉ số `I_j(t)`, nhưng thay vì xây cả chuỗi một lần, **tính lại khi có sự kiện**:

- Trigger: member làm xong 1 task / task mới được giao / priority đổi.
- Tập eligible `E = { j : pending, không bị block }`. (Hiện chưa có dependency — nếu thêm sau, lọc ở đây. Xem §11.)
- `t` = giờ hiện tại quy về `t0`.
- Tính `I_j(t)` trên `E`, trả về task `argmax` làm gợi ý "task kế tiếp".

Hai chế độ dùng chung một hàm tính `I_j`. Tách `compute_index(task, t, p_bar, config)` thành hàm riêng để cả hai gọi.

---

## 6. Tie-breaking (BẮT BUỘC — để kết quả deterministic)

Khi nhiều task cùng `I_j` (hoặc chênh < EPS), phá hòa theo thứ tự:

1. `due_date` sớm hơn trước (EDD).
2. `priority` cao hơn trước.
3. `id` nhỏ hơn trước (chốt hạ, đảm bảo deterministic tuyệt đối).

---

## 7. Output

Trả về danh sách đã sắp xếp; mỗi phần tử kèm dữ liệu giải thích:

```
{
  task_id,
  position,             # 0-based
  priority_index,       # I_j tại lúc được chọn
  projected_start,      # timestamp — dùng được làm "planned start" hiển thị
  projected_finish,     # timestamp
  projected_tardiness,  # giờ trễ dự kiến (0 nếu kịp)
  reason                # chuỗi giải thích, vd: "value density cao (w/p=4.0) và khẩn (slack=-2h)"
}
```

`projected_start` chính là cách `start_date` quay lại như **OUTPUT** (không phải input) — xem §4.

**Explainability** là yêu cầu, không phải nice-to-have: `reason` phải nêu được 2 yếu tố chi phối (value density và độ khẩn/slack) để PM/member tin gợi ý.

---

## 8. Edge cases (BẮT BUỘC xử lý)

| Tình huống | Xử lý |
|---|---|
| Hàng đợi rỗng | Trả về `[]` |
| `estimate` = 0 / null | Dùng giá trị tối thiểu mặc định (vd `0.25h`) + gắn cờ cảnh báo; KHÔNG để chia 0 |
| `due_date` = null (không deadline) | Coi `d_j = +∞` → `slack` rất lớn → `urgency → 0`; task vẫn được xếp theo `w/p`, rơi về cuối trong nhóm tương đương |
| Task đã quá hạn (`d_j < t`) | `slack < 0` → `urgency = 1` (khẩn tối đa). Đúng hành vi mong muốn |
| Tất cả task đã quá hạn | Mọi `urgency = 1` → sắp xếp thuần theo `w/p` (WSPT). Đúng |
| `priority` không nằm trong bảng | Mặc định về `LOW` (hoặc throw config error) — chọn 1, ghi rõ |

---

## 9. Kiểm thử & validation

### 9.1. Unit tests (oracle thủ công)

Test case kinh điển — `k=2`, mọi task ready tại `t=0`:

```
A: w=8, p=4, d=6
B: w=4, p=2, d=3
C: w=1, p=1, d=10
```

`p_bar = (4+2+1)/3 ≈ 2.333`, `k*p_bar ≈ 4.667`.

```
I_A = (8/4) * exp(-max(0, 6-4-0)/4.667) = 2 * exp(-2/4.667)  ≈ 2 * 0.652 = 1.30
I_B = (4/2) * exp(-max(0, 3-2-0)/4.667) = 2 * exp(-1/4.667)  ≈ 2 * 0.807 = 1.61
I_C = (1/1) * exp(-max(0, 10-1-0)/4.667)= 1 * exp(-9/4.667)  ≈ 1 * 0.145 = 0.15
```

**Kỳ vọng:** thứ tự `B → A → C`. (B vượt A dù A là CRITICAL — vì A còn slack.)

Thêm test: đặt `CRITICAL=100` → kỳ vọng A luôn đầu (kiểm chứng núm lexicographic).

### 9.2. Property tests

- Output là một hoán vị của input (không mất/nhân bản task).
- Deterministic: cùng input + config → cùng output.
- Mọi `urgency ∈ (0, 1]`.

### 9.3. Validation experiment (đo độ tin — phục vụ đồ án)

- **Brute force** (thử mọi `n!` thứ tự, tính `ΣwⱼTⱼ`) cho `n ≤ 10` → nghiệm tối ưu thật.
- Đo **optimality gap** = `(Z_ATC - Z_opt) / Z_opt` trên nhiều instance sinh ngẫu nhiên.
- So với baseline: **EDD** (sort theo `d_j`), **WSPT** (sort theo `w_j/p_j`), random.
- Báo cáo gap trung bình + đường cong `k` vs `ΣwⱼTⱼ` để chọn `k*`.

Sinh instance có kiểm soát 2 tham số: độ chặt deadline (tightness factor) và độ phân tán trọng số. (Tham khảo cách sinh của bộ benchmark OR-Library weighted tardiness.)

---

## 10. Liên quan `actualHours` (forecast & đánh giá, KHÔNG vào logic chọn)

- **Forecast:** task đang chạy có thời lượng còn lại `max(0, estimate - actualHours)` → cải thiện độ chính xác của `projected_start` các task phía sau. Chỉ ảnh hưởng hiển thị, không đổi thứ tự.
- **Đánh giá:** `actualHours / estimate` cho phân phối sai số ước lượng → đưa vào sensitivity analysis (chất lượng lịch thay đổi ra sao khi `p_j` lệch khỏi thực tế).

---

## 11. OPTIONAL EXTENSIONS (KHÔNG implement ở bản lõi — chỉ làm khi bản core đã chạy + đo gap xong)

Mỗi mục là một nâng cấp độc lập, để sau:

1. **Local search đánh bóng:** sau khi ATC ra chuỗi, thử các phép adjacent swap / insertion; giữ nếu `ΣwⱼTⱼ` giảm; lặp tới khi không cải thiện. Vá điểm yếu cận thị của greedy, thu hẹp gap.
2. **Release date (ATCR):** nếu sau này một số task thực sự không được bắt đầu trước mốc nào đó → đưa `start_date` thành `r_j`, thêm số hạng mũ thứ hai phạt theo `max(0, r_j - t)`, lọc eligible theo `t ≥ r_j`. Bài toán thành `1|r_j|ΣwⱼTⱼ`. (Tham khảo Morton & Pentico 1993 cho công thức ATCR chính xác.)
3. **Penalty shape không đồng nhất:** task SLA-breach (chi phí cố định, binary) thêm số hạng `v_j·U_j`; task escalating dùng phạt phi tuyến. Mục tiêu mở rộng `Σ(wⱼTⱼ + vⱼUⱼ)`.
4. **Aging / anti-starvation:** task chờ quá lâu được cộng điểm tuổi (cần mốc `created_at` hoặc dùng `planned start`). Cho phép task LOW chờ lâu vượt ngưỡng class cao hơn.
5. **Dependency / blocked_by:** lọc task bị block khỏi tập eligible cho tới khi được giải phóng.
6. **`k` thích nghi theo trạng thái:** tính `k` từ độ chặt deadline của hàng đợi hiện tại thay vì cố định.

---

## 12. Đề xuất cấu trúc module

```
task-arrangement/
  config.ts          # WEIGHTS, k, p_bar mode — tất cả cấu hình được
  index-fn.ts        # compute_index(task, t, p_bar, config) — dùng chung
  arrange.ts         # construction heuristic (static)        — §5.4
  dispatch.ts        # online dispatching (next-task)          — §5.5
  time-utils.ts      # hours_from(t0, ts) theo lịch làm việc   — §5.2
  types.ts           # Task, ArrangedTask, Config
  __tests__/         # unit + property + validation experiment — §9
```

> Ngôn ngữ/stack: implement theo repo hiện có. Pseudocode ở trên là language-neutral.

---

## 13. Tài liệu tham khảo (cho đồ án)

- Graham, R.L., Lawler, E.L., Lenstra, J.K., Rinnooy Kan, A.H.G. (1979). *Optimization and approximation in deterministic sequencing and scheduling: a survey.* Annals of Discrete Mathematics 5, 287–326. — ký hiệu α|β|γ.
- Smith, W.E. (1956). *Various optimizers for single-stage production.* Naval Research Logistics Quarterly 3(1–2), 59–66. — WSPT, tối ưu cho `ΣwⱼCⱼ`.
- Lawler, E.L. (1977). *A 'pseudopolynomial' algorithm for sequencing jobs to minimize total tardiness.* Annals of Discrete Mathematics 1, 331–342.
- Lenstra, J.K., Rinnooy Kan, A.H.G., Brucker, P. (1977). *Complexity of machine scheduling problems.* Annals of Discrete Mathematics 1, 343–362. — NP-hardness.
- Vepsalainen, A.P.J., Morton, T.E. (1987). *Priority rules for job shops with weighted tardiness costs.* Management Science 33(8), 1035–1047. — luật ATC.
- Morton, T.E., Pentico, D.W. (1993). *Heuristic Scheduling Systems.* Wiley. — ATC/ATCR chi tiết.

> Lưu ý epistemic khi viết đồ án: WSPT/EDD = **định lý tối ưu**; NP-hardness = **định lý**; ATC = **heuristic hiệu năng cao (KHÔNG tối ưu)**, độ tin chứng minh qua optimality gap thực nghiệm.
