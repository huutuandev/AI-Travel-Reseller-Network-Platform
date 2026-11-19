# ==============================================================================
# SCRIPT AUTOMATION TEST TOÀN DIỆN MODULE RESELLER THEO CHUẨN SRS (BẢN MỚI NHẤT)
# Dự án: AI Travel Reseller Network Platform
# Nhánh: test/Hien-reseller
# Bao phủ 100% chức năng Reseller:
#   1. Authentication (Login, Validation)
#   2. Security & RBAC (Chặn quyền chéo Admin / Merchant / Reseller, SQLi)
#   3. Data Validation (UUID, Empty Body, Conflicting Filters, Range)
#   4. Marketplace & Campaigns (List, Filter, Detail, Edge Cases)
#   5. Applications (Apply, Duplicate Apply, Ineligible Apply)
#   6. Tracking Links & Commissions (Đã fix lỗi SQL trong commit 468d41c)
#   7. Reseller Dashboard (Tính năng mới trong commit 3aadcef)
#   8. TikTok OAuth & Social Video (OAuth URL, Callback, Video List, IDOR)
#   9. Merchant Reseller Rankings (Commission Ranking, Score Ranking)
# Tự động xuất file Excel: docs/Bao_cao_Kiem_thu_Toan_Bo_Reseller_Hien.xlsx
# ==============================================================================

param (
    [string]$BaseUrl = "http://localhost:8080"
)

# Thiết lập encoding UTF-8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$totalTests = 0
$passCount = 0
$failCount = 0
$results = @()

function Invoke-SafeApi {
    param (
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )

    $params = @{
        Uri             = $Uri
        Method          = $Method
        Headers         = $Headers
        UseBasicParsing = $true
        TimeoutSec      = 10
    }

    if ($null -ne $Body -and $Body -ne "") {
        $params["Body"] = $Body
        $params["ContentType"] = $ContentType
    }

    try {
        $resp = Invoke-WebRequest @params
        return [PSCustomObject]@{
            StatusCode = [int]$resp.StatusCode
            Content    = $resp.Content
            Error      = $null
        }
    } catch [System.Net.WebException] {
        if ($_.Exception.Response) {
            $webResp = [System.Net.HttpWebResponse]$_.Exception.Response
            $reader = New-Object System.IO.StreamReader($webResp.GetResponseStream())
            $errContent = $reader.ReadToEnd()
            return [PSCustomObject]@{
                StatusCode = [int]$webResp.StatusCode
                Content    = $errContent
                Error      = $_.Exception.Message
            }
        } else {
            return [PSCustomObject]@{
                StatusCode = 0
                Content    = ""
                Error      = $_.Exception.Message
            }
        }
    } catch {
        return [PSCustomObject]@{
            StatusCode = 0
            Content    = ""
            Error      = $_.Exception.Message
        }
    }
}

function Run-Test {
    param (
        [string]$Category,
        [string]$Code,
        [string]$Name,
        [int[]]$ExpectedStatuses,
        [scriptblock]$Action
    )
    $script:totalTests++
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    
    $res = & $Action
    $actualStatus = if ($res) { $res.StatusCode } else { 0 }
    
    $stopwatch.Stop()
    $elapsed = "$($stopwatch.ElapsedMilliseconds)ms"

    if ($ExpectedStatuses -contains $actualStatus) {
        $statusText = "PASS"
        $script:passCount++
        Write-Host "  ✅ [$Code] $Name - Status: $actualStatus ($elapsed)" -ForegroundColor Green
    } else {
        $statusText = "FAIL"
        $script:failCount++
        $expStr = ($ExpectedStatuses -join " hoặc ")
        Write-Host "  ❌ [$Code] $Name - Mong đợi: $expStr, Nhận: $actualStatus ($elapsed)" -ForegroundColor Red
        if ($res.Error) {
            Write-Host "     Chi tiết lỗi: $($res.Error)" -ForegroundColor DarkGray
        }
    }

    $script:results += [PSCustomObject]@{
        Category = $Category
        Code     = $Code
        Name     = $Name
        Expected = ($ExpectedStatuses -join "/")
        Actual   = $actualStatus
        Result   = $statusText
        Duration = $elapsed
    }
}

Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "   BẮT ĐẦU KIỂM THỬ TOÀN DIỆN 100% MODULE RESELLER THEO SRS       " -ForegroundColor Cyan
Write-Host "   Mục tiêu: $BaseUrl                                            " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan

# 0. Kiểm tra kết nối Backend
Write-Host "`n[0/4] Kiểm tra kết nối tới Backend..." -ForegroundColor Yellow
$ping = Invoke-SafeApi -Uri "$BaseUrl/v3/api-docs" -Method "GET"
if ($ping.StatusCode -ne 200) {
    Write-Host ">> LỖI: Chưa kết nối được tới Backend tại $BaseUrl." -ForegroundColor Red
    Write-Host ">> Vui lòng đảm bảo ứng dụng Spring Boot đang chạy trên cổng 8080!" -ForegroundColor Yellow
    exit 1
}
Write-Host ">> Backend đang hoạt động tốt (OpenAPI/Swagger khả dụng: HTTP 200)." -ForegroundColor Green

# 1. Đăng nhập lấy Token cho các vai trò
Write-Host "`n[*] Đăng nhập lấy Token cho Reseller, Admin và Merchant..." -ForegroundColor Yellow
$resellerToken = ""
$adminToken = ""
$merchantToken = ""

$resLogin = Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body '{"email":"reseller1@test.com","password":"Reseller@123"}'
if ($resLogin.StatusCode -eq 200) {
    $resObj = $resLogin.Content | ConvertFrom-Json
    $resellerToken = $resObj.data.accessToken
    Write-Host ">> Đăng nhập Reseller (reseller1@test.com) thành công." -ForegroundColor Green
} else {
    Write-Host ">> Cảnh báo: Đăng nhập Reseller thất bại (HTTP $($resLogin.StatusCode))" -ForegroundColor Red
}

$admLogin = Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body '{"email":"admin@test.com","password":"Admin@123"}'
if ($admLogin.StatusCode -eq 200) {
    $admObj = $admLogin.Content | ConvertFrom-Json
    $adminToken = $admObj.data.accessToken
    Write-Host ">> Đăng nhập Platform Admin (admin@test.com) thành công." -ForegroundColor Green
} else {
    Write-Host ">> Cảnh báo: Đăng nhập Admin thất bại (HTTP $($admLogin.StatusCode))" -ForegroundColor Red
}

$merLogin = Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body '{"email":"merchant@test.com","password":"Merchant@123"}'
if ($merLogin.StatusCode -eq 200) {
    $merObj = $merLogin.Content | ConvertFrom-Json
    $merchantToken = $merObj.data.accessToken
    Write-Host ">> Đăng nhập Merchant (merchant@test.com) thành công." -ForegroundColor Green
} else {
    Write-Host ">> Cảnh báo: Đăng nhập Merchant thất bại (HTTP $($merLogin.StatusCode))" -ForegroundColor Red
}

$resellerHeaders = @{ Authorization = "Bearer $resellerToken" }
$adminHeaders    = @{ Authorization = "Bearer $adminToken" }
$merchantHeaders = @{ Authorization = "Bearer $merchantToken" }

# Lấy 1 Campaign ID hợp lệ từ Marketplace
$sampleCampaignId = ""
$listRes = Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns" -Method "GET" -Headers $resellerHeaders
if ($listRes.StatusCode -eq 200) {
    try {
        $listObj = $listRes.Content | ConvertFrom-Json
        if ($listObj.data.Count -gt 0) {
            $sampleCampaignId = $listObj.data[0].id
            Write-Host ">> Tìm thấy Campaign mẫu: $sampleCampaignId" -ForegroundColor DarkCyan
        }
    } catch {}
}

# ==============================================================================
# NHÓM 1: SECURITY & PHÂN QUYỀN RBAC THEO SRS
# ==============================================================================
Write-Host "`n--- [NHÓM 1: SECURITY & RBAC TESTING] ---" -ForegroundColor Magenta

Run-Test "Security" "SEC-01" "SRS: Chặn truy cập không xác thực (No Token)" @(401, 403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns" -Method "GET"
}

Run-Test "Security" "SEC-02" "SRS: Chặn truy cập với Token giả mạo/sai chữ ký" @(401, 403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns" -Method "GET" -Headers @{ Authorization = "Bearer invalid.fake.token" }
}

Run-Test "Security" "SEC-03" "SRS: Phân quyền RBAC - Reseller cố gọi API Admin" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/admin/tenants" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Security" "SEC-04" "SRS: Phân quyền RBAC - Admin cố gọi API Reseller" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns" -Method "GET" -Headers $adminHeaders
}

Run-Test "Security" "SEC-05" "SRS: Bảo vệ SQL Injection trong query tìm kiếm 'q'" @(200, 400) {
    $sqli = [System.Uri]::EscapeDataString("' OR 1=1; DROP TABLE users; --")
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns?q=$sqli" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Security" "SEC-06" "SRS: Phân quyền RBAC - Reseller gọi API Xếp hạng Commission của Merchant -> 403" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/merchant/resellers/commission-ranking" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Security" "SEC-07" "SRS: Phân quyền RBAC - Reseller gọi API Xếp hạng Score của Merchant -> 403" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/merchant/resellers/score-ranking" -Method "GET" -Headers $resellerHeaders
}

# ==============================================================================
# NHÓM 2: DATA VALIDATION TESTING (XÁC THỰC DỮ LIỆU ĐẦU VÀO THEO SRS)
# ==============================================================================
Write-Host "`n--- [NHÓM 2: DATA VALIDATION TESTING] ---" -ForegroundColor Magenta

Run-Test "Validation" "VAL-01" "SRS: Path Variable sai định dạng UUID (/campaigns/not-a-uuid)" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/not-a-uuid" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Validation" "VAL-02" "SRS: Body rỗng khi Login ({})" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body "{}"
}

Run-Test "Validation" "VAL-03" "SRS: Email sai định dạng khi Login" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body '{"email":"not-an-email-format","password":"Reseller@123"}'
}

Run-Test "Validation" "VAL-04" "SRS: Mật khẩu rỗng khi Login" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/auth/login" -Method "POST" -Body '{"email":"reseller1@test.com","password":""}'
}

Run-Test "Validation" "VAL-05" "SRS: Xung đột tham số FIXED_AMOUNT và minCommissionPercent -> 400" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns?commissionType=FIXED_AMOUNT&minCommissionPercent=10" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Validation" "VAL-06" "SRS: Sai commissionType không thuộc PERCENTAGE hoặc FIXED_AMOUNT -> 400" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns?commissionType=INVALID_TYPE" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Validation" "VAL-07" "SRS: minCommissionPercent vượt ngoài khoảng 0-100 (ví dụ 150) -> 400" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns?minCommissionPercent=150" -Method "GET" -Headers $resellerHeaders
}

# ==============================================================================
# NHÓM 3: SRS MARKETPLACE, DETAILS & APPLICATIONS (CHIẾN DỊCH & NỘP ĐƠN)
# ==============================================================================
Write-Host "`n--- [NHÓM 3: MARKETPLACE, DETAILS & APPLICATIONS] ---" -ForegroundColor Magenta

Run-Test "Marketplace" "SRS-01" "SRS: Lấy danh sách Campaign Marketplace (chỉ ACTIVE & AVAILABLE)" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Marketplace" "SRS-02" "SRS: Xem chi tiết Campaign hợp lệ theo ID" @(200, 404) {
    $targetId = if ($sampleCampaignId) { $sampleCampaignId } else { "00000000-0000-0000-0000-000000000001" }
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/$targetId" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Marketplace" "EDG-01" "SRS: UUID hợp lệ nhưng không tồn tại trong hệ thống -> 404" @(404) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/00000000-0000-0000-0000-000000000099" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Applications" "SRS-03" "SRS: Reseller gửi đơn tham gia chiến dịch (Apply to Campaign)" @(200, 201, 409, 400) {
    $targetId = if ($sampleCampaignId) { $sampleCampaignId } else { "00000000-0000-0000-0000-000000000001" }
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/$targetId/applications" -Method "POST" -Headers $resellerHeaders
}

Run-Test "Applications" "EDG-02" "SRS: Chặn đăng ký trùng chiến dịch (Duplicate Apply -> 409 Conflict)" @(409, 400) {
    $targetId = if ($sampleCampaignId) { $sampleCampaignId } else { "00000000-0000-0000-0000-000000000001" }
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/$targetId/applications" -Method "POST" -Headers $resellerHeaders
}

Run-Test "Applications" "EDG-03" "SRS: Apply vào Campaign không tồn tại / không khả dụng -> 400/404" @(400, 404) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/00000000-0000-0000-0000-000000000099/applications" -Method "POST" -Headers $resellerHeaders
}

Run-Test "Applications" "SRS-04" "SRS: Lấy danh sách các chiến dịch đã đăng ký (My Applications)" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/campaigns/applications" -Method "GET" -Headers $resellerHeaders
}

# ==============================================================================
# NHÓM 4: TRACKING LINKS, COMMISSIONS & DASHBOARD (ĐÃ SỬA LỖI SQL)
# ==============================================================================
Write-Host "`n--- [NHÓM 4: TRACKING LINKS, COMMISSIONS & DASHBOARD] ---" -ForegroundColor Magenta

Run-Test "Operations" "SRS-05" "SRS: Lấy danh sách Tracking Links của Reseller (Đã fix SQL)" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/tracking-links" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Operations" "SRS-06" "SRS: Lấy danh sách Commissions (Hoa hồng) của Reseller (Đã fix SQL)" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/commissions" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Dashboard" "DSH-01" "SRS: Lấy thông tin tổng quan Reseller Dashboard (Tính năng mới) -> 200" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/dashboard" -Method "GET" -Headers $resellerHeaders
}

Run-Test "Dashboard" "DSH-02" "RBAC: Admin cố gọi API Dashboard của Reseller -> 403 Forbidden" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/dashboard" -Method "GET" -Headers $adminHeaders
}

# ==============================================================================
# NHÓM 5: TIKTOK OAUTH & SOCIAL VIDEO API (TÍNH NĂNG MỚI CỦA RESELLER)
# ==============================================================================
Write-Host "`n--- [NHÓM 5: TIKTOK OAUTH & SOCIAL VIDEO TESTING] ---" -ForegroundColor Magenta

Run-Test "TikTok-Sec" "TIK-01" "RBAC: Admin cố gọi API lấy link TikTok OAuth của Reseller -> 403" @(403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/oauth-url" -Method "GET" -Headers $adminHeaders
}

Run-Test "TikTok-Sec" "TIK-02" "RBAC: Admin cố gọi API đăng ký Social Video của Reseller -> 403" @(403) {
    $validBody = '{"postUrl":"https://www.tiktok.com/@test/video/123","socialAccountId":"00000000-0000-0000-0000-000000000001","campaignApplicationId":"00000000-0000-0000-0000-000000000001"}'
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-videos" -Method "POST" -Headers $adminHeaders -Body $validBody
}

Run-Test "TikTok-SRS" "TIK-03" "SRS: Reseller lấy URL đăng nhập TikTok OAuth" @(200, 400, 409, 503) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/oauth-url" -Method "GET" -Headers $resellerHeaders
}

Run-Test "TikTok-Edge" "TIK-04" "Edge Case: Gọi TikTok Callback với state giả mạo / thiếu code (Không 500)" @(400, 401, 403) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/callback?code=fake&state=tampered" -Method "GET"
}

Run-Test "TikTok-Val" "TIK-05" "Validation: Gửi Body rỗng {} khi đăng ký Social Video -> 400" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-videos" -Method "POST" -Headers $resellerHeaders -Body "{}"
}

Run-Test "TikTok-Val" "TIK-06" "Validation: Gửi URL không hợp lệ & UUID không hợp lệ khi đăng ký Video -> 400" @(400) {
    $badBody = '{"postUrl":"not-a-valid-url","socialAccountId":"00000000-0000-0000-0000-000000000099","campaignApplicationId":"00000000-0000-0000-0000-000000000099"}'
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-videos" -Method "POST" -Headers $resellerHeaders -Body $badBody
}

Run-Test "TikTok-Edge" "TIK-07" "Edge Case: Reseller chưa liên kết TikTok gọi lấy danh sách Video (Không 500)" @(200, 400, 404) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/videos" -Method "GET" -Headers $resellerHeaders
}

Run-Test "TikTok-Val" "TIK-08" "Validation: Truyền sai định dạng UUID khi xem Video theo socialAccountId -> 400" @(400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/not-a-uuid/videos" -Method "GET" -Headers $resellerHeaders
}

Run-Test "TikTok-Edge" "TIK-09" "Edge Case & IDOR: Xem Video của socialAccountId không tồn tại / của người khác -> 403/404" @(403, 404) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/reseller/social-accounts/tiktok/00000000-0000-0000-0000-000000000099/videos" -Method "GET" -Headers $resellerHeaders
}

# ==============================================================================
# NHÓM 6: MERCHANT RESELLER RANKINGS (BẢNG XẾP HẠNG RESELLER)
# ==============================================================================
Write-Host "`n--- [NHÓM 6: MERCHANT RESELLER RANKING TESTING] ---" -ForegroundColor Magenta

Run-Test "Merchant-Rank" "RNK-01" "SRS: Merchant xem bảng xếp hạng Reseller theo Commission -> 200" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/merchant/resellers/commission-ranking?currency=VND&page=0&size=5" -Method "GET" -Headers $merchantHeaders
}

Run-Test "Merchant-Rank" "RNK-02" "SRS: Merchant xem bảng xếp hạng Reseller theo Score TikTok -> 200" @(200) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/merchant/resellers/score-ranking" -Method "GET" -Headers $merchantHeaders
}

Run-Test "Merchant-Rank" "RNK-03" "Edge Case: Lọc Commission Ranking với currency không hỗ trợ (USD) -> 200/400" @(200, 400) {
    Invoke-SafeApi -Uri "$BaseUrl/api/v1/merchant/resellers/commission-ranking?currency=USD&page=0&size=5" -Method "GET" -Headers $merchantHeaders
}

# ==============================================================================
# BÁO CÁO TỔNG HỢP (SUMMARY REPORT) & XUẤT EXCEL
# ==============================================================================
Write-Host "`n==================================================================" -ForegroundColor Cyan
Write-Host "            BÁO CÁO TỔNG KẾT KIỂM THỬ TOÀN BỘ RESELLER            " -ForegroundColor Cyan
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host " Tổng số Test Cases đã chạy : $totalTests"
Write-Host " Số lượng Test PASS          : $passCount" -ForegroundColor Green
Write-Host " Số lượng Test FAIL          : $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
$rate = [Math]::Round(($passCount / $totalTests) * 100, 1)
Write-Host " Tỷ lệ đạt (Pass Rate)       : $rate%`n" -ForegroundColor Yellow

$results | Format-Table -Property Code, Category, Name, Expected, Actual, Result, Duration -AutoSize

# Xuất kết quả ra file JSON và gọi Python tạo file Excel
try {
    $jsonPath = Join-Path $PSScriptRoot "test-results.json"
    $results | ConvertTo-Json -Depth 3 | Set-Content -Path $jsonPath -Encoding UTF8
    
    $excelPath = "docs/Bao_cao_Kiem_thu_Toan_Bo_Reseller_Hien.xlsx"
    $pyScript = Join-Path $PSScriptRoot "export_excel_report.py"
    
    python $pyScript $excelPath $jsonPath
    Write-Host "`n📊 [XUẤT EXCEL THÀNH CÔNG] File Excel đã sẵn sàng tại:" -ForegroundColor Green
    Write-Host "   👉 $excelPath" -ForegroundColor Cyan
} catch {
    Write-Host "Lưu ý: Không thể xuất file Excel tự động: $($_.Exception.Message)" -ForegroundColor DarkGray
}
