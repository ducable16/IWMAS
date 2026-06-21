# Đo độ trễ tìm kiếm theo từng tầng (Redis / Elasticsearch / Database) cho Chương 5.
#
# Cách dùng (backend + Redis + Elasticsearch + DB đang chạy):
#   .\scripts\measure-search-latency.ps1 -Email pm@example.com -Password secret
#
# Đo tầng DB fallback: tắt Elasticsearch rồi chạy lại —
#   docker stop <tên-container-es>
#   .\scripts\measure-search-latency.ps1 -Email ... -Password ...
#
# Cơ chế: với mỗi prefix, lần gọi đầu tiên là cache-miss (source=elasticsearch,
# hoặc database khi ES tắt), các lần sau trúng cache (source=redis). Script gọi
# mỗi prefix nhiều lần rồi gom số liệu theo trường `source` mà API trả về, nên
# một lần chạy cho ra số liệu của cả tầng cache lẫn tầng engine.
#
# Kết quả: p50/p95 của tookMs (thời gian xử lý phía server, loại trừ mạng)
# và của clientMs (round-trip đầy đủ phía client) cho từng source.

param(
    [string]$BaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)][string]$Email,
    [Parameter(Mandatory = $true)][string]$Password,
    [string[]]$Queries = @("ng", "tr", "le", "ph", "ho", "da", "an", "minh", "thu", "qu"),
    [int]$Iterations = 30
)

$ErrorActionPreference = "Stop"

# ── login ────────────────────────────────────────────────────────────────────
$loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json
$auth = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" `
    -ContentType "application/json" -Body $loginBody
$headers = @{ Authorization = "Bearer $($auth.accessToken)" }
Write-Host "Đăng nhập OK ($Email)`n"

# ── đo ───────────────────────────────────────────────────────────────────────
$samples = New-Object System.Collections.Generic.List[object]

foreach ($q in $Queries) {
    for ($i = 0; $i -lt $Iterations; $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $resp = Invoke-RestMethod -Uri "$BaseUrl/api/autocomplete?q=$q" -Headers $headers
            $sw.Stop()
            $samples.Add([pscustomobject]@{
                Query    = $q
                Source   = $resp.source
                TookMs   = [long]$resp.tookMs
                ClientMs = $sw.Elapsed.TotalMilliseconds
            })
        } catch {
            $sw.Stop()
            Write-Warning "q='$q' lần $($i+1) lỗi: $($_.Exception.Message)"
        }
    }
}

if ($samples.Count -eq 0) { Write-Error "Không thu được mẫu nào."; exit 1 }

# ── thống kê ─────────────────────────────────────────────────────────────────
function Percentile([double[]]$values, [double]$p) {
    $sorted = $values | Sort-Object
    $idx = [math]::Ceiling($p / 100.0 * $sorted.Count) - 1
    if ($idx -lt 0) { $idx = 0 }
    [math]::Round($sorted[$idx], 1)
}

Write-Host ("{0,-15} {1,6} {2,12} {3,12} {4,12} {5,12}" -f `
    "source", "n", "took p50", "took p95", "client p50", "client p95")
Write-Host ("-" * 75)
foreach ($group in ($samples | Group-Object Source | Sort-Object Name)) {
    $took   = [double[]]($group.Group | ForEach-Object { $_.TookMs })
    $client = [double[]]($group.Group | ForEach-Object { $_.ClientMs })
    Write-Host ("{0,-15} {1,6} {2,10}ms {3,10}ms {4,10}ms {5,10}ms" -f `
        $group.Name, $group.Count,
        (Percentile $took 50), (Percentile $took 95),
        (Percentile $client 50), (Percentile $client 95))
}

Write-Host "`nTổng: $($samples.Count) mẫu / $($Queries.Count) prefix x $Iterations lần."
Write-Host "Lưu ý: muốn có dòng 'database', tắt Elasticsearch rồi chạy lại script."
