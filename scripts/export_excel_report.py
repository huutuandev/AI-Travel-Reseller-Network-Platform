# -*- coding: utf-8 -*-
"""
Script xuất Báo cáo Kiểm thử API Toàn diện (Security, Data Validation, Business Rules, Edge Cases)
Dự án: AI Travel Reseller Network Platform
Nhánh: test/Hien-reseller
Xuất ra file Excel chuẩn Doanh nghiệp gồm 2 Sheet:
  - Tab 1: 'Test Cases' (Chi tiết 37 kịch bản kiểm thử bao phủ toàn bộ 7+ controllers của Reseller)
  - Tab 2: 'Bugs (Defect Log)' (Ghi chép chi tiết các lỗi phát hiện theo chuẩn SRS, Root Cause & Đề xuất cho Dev)
"""

import sys
import json
import os
from datetime import datetime
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

def create_report(output_path="docs/Bao_cao_Kiem_thu_Toan_Bo_Reseller_Hien.xlsx", results_json_path=None):
    wb = openpyxl.Workbook()
    wb.remove(wb.active) # Xóa sheet mặc định

    # Đọc kết quả thực tế từ file JSON nếu có
    actual_results = {}
    if results_json_path and os.path.exists(results_json_path):
        try:
            with open(results_json_path, "r", encoding="utf-8-sig") as f:
                data = json.load(f)
                for item in data:
                    actual_results[item["Code"]] = item
        except Exception as e:
            print(f"Warning: Could not read JSON: {e}")

    # =========================================================================
    # STYLES CHUẨN DOANH NGHIỆP
    # =========================================================================
    font_title = Font(name="Calibri", size=16, bold=True, color="1F497D")
    font_subtitle = Font(name="Calibri", size=11, italic=True, color="595959")
    font_header = Font(name="Calibri", size=11, bold=True, color="FFFFFF")
    font_body = Font(name="Calibri", size=11)
    font_bold = Font(name="Calibri", size=11, bold=True)
    
    font_pass = Font(name="Calibri", size=11, bold=True, color="274E13")
    fill_pass = PatternFill(start_color="D9EAD3", end_color="D9EAD3", fill_type="solid")
    
    font_fail = Font(name="Calibri", size=11, bold=True, color="783F04")
    fill_fail = PatternFill(start_color="FCE5CD", end_color="FCE5CD", fill_type="solid")

    font_bug_open = Font(name="Calibri", size=11, bold=True, color="990000")
    fill_bug_open = PatternFill(start_color="F4CCCC", end_color="F4CCCC", fill_type="solid")
    
    font_bug_fixed = Font(name="Calibri", size=11, bold=True, color="274E13")
    fill_bug_fixed = PatternFill(start_color="D9EAD3", end_color="D9EAD3", fill_type="solid")

    font_bug_med = Font(name="Calibri", size=11, bold=True, color="7F6000")
    fill_bug_med = PatternFill(start_color="FFF2CC", end_color="FFF2CC", fill_type="solid")

    fill_header_navy = PatternFill(start_color="1F497D", end_color="1F497D", fill_type="solid")
    fill_header_red = PatternFill(start_color="C00000", end_color="C00000", fill_type="solid")
    fill_zebra = PatternFill(start_color="F9FAFB", end_color="F9FAFB", fill_type="solid")

    thin_border_side = Side(style='thin', color="D9D9D9")
    border_cell = Border(left=thin_border_side, right=thin_border_side, top=thin_border_side, bottom=thin_border_side)

    align_center = Alignment(horizontal="center", vertical="center", wrap_text=True)
    align_left = Alignment(horizontal="left", vertical="center", wrap_text=True)
    align_right = Alignment(horizontal="right", vertical="center", wrap_text=True)

    # =========================================================================
    # TAB 1: TEST CASES (TOÀN BỘ 37 KỊCH BẢN KIỂM THỬ)
    # =========================================================================
    ws1 = wb.create_sheet(title="Test Cases")
    ws1.views.sheetView[0].showGridLines = True

    # Header title
    ws1.merge_cells("A1:K1")
    ws1["A1"] = "BÁO CÁO KẾT QUẢ KIỂM THỬ TOÀN BỘ CHỨC NĂNG API RESELLER (SRS AUDIT)"
    ws1["A1"].font = font_title
    ws1["A1"].alignment = Alignment(horizontal="left", vertical="center")

    ws1["A2"] = "Dự án: AI Travel Reseller Network Platform | Người thực hiện: Hiền (QA/Tester) | Nhánh: test/Hien-reseller | Ngày: " + datetime.now().strftime("%d/%m/%Y")
    ws1["A2"].font = font_subtitle

    ws1["A3"] = "Phạm vi: Security/RBAC | Data Validation | Marketplace & Detail | Applications | Tracking & Commissions | Dashboard | TikTok OAuth & Video | Merchant Rankings"
    ws1["A3"].font = font_subtitle

    headers_ws1 = [
        "STT", "Mã Test", "Nhóm Kiểm Thử", "Tên Kịch Bản Kiểm Thử",
        "Method & Endpoint", "Dữ Liệu Đầu Vào / Thao Tác",
        "HTTP Kỳ Vọng", "HTTP Thực Tế", "Kết Quả", "Thời Gian", "Đánh Giá / Ghi Chú Theo Chuẩn SRS"
    ]

    row_num = 5
    for col_idx, h in enumerate(headers_ws1, 1):
        cell = ws1.cell(row=row_num, column=col_idx, value=h)
        cell.font = font_header
        cell.fill = fill_header_navy
        cell.alignment = align_center
        cell.border = border_cell
    ws1.row_dimensions[row_num].height = 28

    test_data = [
        # Nhóm 1: Security & RBAC (7 test cases)
        (1, "SEC-01", "Security", "Chặn truy cập không xác thực (No Token)", "GET /api/v1/reseller/campaigns", "Không truyền header Authorization", "401/403", 401, "PASS", "129ms", "Đạt chuẩn SRS: Spring Security Filter chặn thành công."),
        (2, "SEC-02", "Security", "Chặn truy cập với Token giả mạo / sai chữ ký", "GET /api/v1/reseller/campaigns", "Authorization: Bearer invalid.fake.token", "401/403", 401, "PASS", "36ms", "Đạt chuẩn SRS: JwtFilter xác thực chữ ký số hợp lệ."),
        (3, "SEC-03", "Security", "Phân quyền RBAC: Reseller gọi API Quản trị Admin", "GET /api/v1/admin/tenants", "Bearer token của Reseller", "403", 403, "PASS", "55ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(ADMIN)') chặn thành công."),
        (4, "SEC-04", "Security", "Phân quyền RBAC: Admin gọi API Reseller", "GET /api/v1/reseller/campaigns", "Bearer token của Admin", "403", 403, "PASS", "69ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(RESELLER)') chặn đúng vai trò."),
        (5, "SEC-05", "Security", "Bảo vệ SQL Injection trong tham số tìm kiếm 'q'", "GET /api/v1/reseller/campaigns?q=...", "q=' OR 1=1; DROP TABLE users; --", "200/400", 200, "PASS", "126ms", "Đạt chuẩn SRS: Tham số được bind an toàn (PreparedStatement), không sập 500."),
        (6, "SEC-06", "Security", "Phân quyền RBAC: Reseller gọi API Xếp hạng Commission của Merchant", "GET /api/v1/merchant/resellers/commission-ranking", "Bearer token của Reseller", "403", 403, "PASS", "58ms", "Đạt chuẩn SRS: Chặn truy cập chéo vai trò từ Reseller sang Merchant."),
        (7, "SEC-07", "Security", "Phân quyền RBAC: Reseller gọi API Xếp hạng Score của Merchant", "GET /api/v1/merchant/resellers/score-ranking", "Bearer token của Reseller", "403", 403, "PASS", "58ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(MERCHANT)') chặn Reseller thành công."),

        # Nhóm 2: Data Validation (7 test cases)
        (8, "VAL-01", "Validation", "Sai định dạng UUID trên Path Variable", "GET /api/v1/reseller/campaigns/not-a-uuid", "Path Variable truyền chuỗi chữ không phải UUID", "400", 400, "PASS", "65ms", "Đạt chuẩn: Bắt lỗi type mismatch chuyển đổi sang 400 Bad Request."),
        (9, "VAL-02", "Validation", "Body rỗng khi Login ({})", "POST /api/v1/auth/login", "Body: {}", "400", 400, "PASS", "72ms", "Đạt chuẩn: @Valid trên LoginRequest phát hiện thiếu các trường bắt buộc."),
        (10, "VAL-03", "Validation", "Email sai định dạng khi Login", "POST /api/v1/auth/login", "{\"email\":\"not-an-email-format\",\"password\":\"Reseller@123\"}", "400", 400, "PASS", "62ms", "Đạt chuẩn: Regex @Email phát hiện format không hợp lệ."),
        (11, "VAL-04", "Validation", "Mật khẩu rỗng khi Login", "POST /api/v1/auth/login", "{\"email\":\"reseller1@test.com\",\"password\":\"\"}", "400", 400, "PASS", "43ms", "Đạt chuẩn: @NotBlank phát hiện mật khẩu rỗng."),
        (12, "VAL-05", "Validation", "Xung đột tham số FIXED_AMOUNT và minCommissionPercent", "GET /api/v1/reseller/campaigns", "Query: commissionType=FIXED_AMOUNT&minCommissionPercent=10", "400", 400, "PASS", "88ms", "Đạt chuẩn SRS: Đã được Dev sửa trong commit 468d41c, kiểm tra xung đột và trả 400."),
        (13, "VAL-06", "Validation", "Sai commissionType không thuộc PERCENTAGE hoặc FIXED_AMOUNT", "GET /api/v1/reseller/campaigns", "Query: commissionType=INVALID_TYPE", "400", 400, "PASS", "65ms", "Đạt chuẩn: Ném IllegalArgumentException được GlobalExceptionHandler bắt trả 400."),
        (14, "VAL-07", "Validation", "minCommissionPercent vượt ngoài khoảng 0-100 (ví dụ 150)", "GET /api/v1/reseller/campaigns", "Query: minCommissionPercent=150", "400", 400, "PASS", "68ms", "Đạt chuẩn SRS: Ràng buộc giá trị phần trăm hợp lệ từ chối tham số phi lý."),

        # Nhóm 3: Marketplace & Applications (7 test cases)
        (15, "SRS-01", "Marketplace", "Lấy danh sách Campaign Marketplace (chỉ ACTIVE & AVAILABLE)", "GET /api/v1/reseller/campaigns", "Token Reseller hợp lệ", "200", 200, "PASS", "97ms", "Đạt chuẩn SRS: Trả về danh sách chiến dịch đang hoạt động kèm sản phẩm khả dụng."),
        (16, "SRS-02", "Marketplace", "Xem chi tiết Campaign hợp lệ theo UUID", "GET /api/v1/reseller/campaigns/{id}", "ID chiến dịch có thật trong CSDL", "200", 200, "PASS", "145ms", "Đạt chuẩn SRS: Trả về thông tin chi tiết, điều khoản và tỷ lệ hoa hồng."),
        (17, "EDG-01", "Marketplace", "UUID hợp lệ nhưng không tồn tại trong hệ thống", "GET /api/v1/reseller/campaigns/{id}", "UUID: 00000000-0000-0000-0000-000000000099", "404", 404, "PASS", "152ms", "Đạt chuẩn: ResourceNotFoundException chuyển sang 404 Not Found rõ ràng."),
        (18, "SRS-03", "Applications", "Reseller nộp đơn tham gia chiến dịch (Apply to Campaign)", "POST /api/v1/reseller/campaigns/{id}/applications", "Token Reseller hợp lệ, nộp đơn chiến dịch", "200/201/409/400", 500, "FAIL", "323ms", "Lỗi Bug #02 (OPEN): Thiếu cột commission_type_snapshot NOT NULL trong INSERT."),
        (19, "EDG-02", "Applications", "Chặn đăng ký trùng chiến dịch (Duplicate Apply)", "POST /api/v1/reseller/campaigns/{id}/applications", "Nộp đơn lần 2 vào cùng chiến dịch", "409/400", 500, "FAIL", "99ms", "Lỗi Bug #02 (OPEN): Do lệnh INSERT bị crash 500 nên luồng không đến bước 409 Conflict."),
        (20, "EDG-03", "Applications", "Apply vào Campaign không tồn tại / không khả dụng", "POST /api/v1/reseller/campaigns/{id}/applications", "UUID không tồn tại", "400/404", 400, "PASS", "86ms", "Đạt chuẩn SRS: Kiểm tra chiến dịch không tồn tại/không active và trả 400 Bad Request."),
        (21, "SRS-04", "Applications", "Lấy danh sách chiến dịch đã đăng ký (My Applications)", "GET /api/v1/reseller/campaigns/applications", "Token Reseller", "200", 200, "PASS", "148ms", "Đạt chuẩn SRS: Lấy đúng danh sách chiến dịch của chính Reseller đang đăng nhập."),

        # Nhóm 4: Tracking Links, Commissions & Dashboard (4 test cases)
        (22, "SRS-05", "Operations", "Lấy danh sách Tracking Links của Reseller", "GET /api/v1/reseller/tracking-links", "Token Reseller", "200", 200, "PASS", "130ms", "Đạt chuẩn SRS: Đã fix lỗi SQL (commit 468d41c), lấy đúng link và số lượt clicks."),
        (23, "SRS-06", "Operations", "Lấy danh sách Commissions (Hoa hồng) của Reseller", "GET /api/v1/reseller/commissions", "Token Reseller", "200", 200, "PASS", "129ms", "Đạt chuẩn SRS: Đã fix lỗi SQL (commit 468d41c), SELECT cột amount và JOIN bookings."),
        (24, "DSH-01", "Dashboard", "Lấy thông tin tổng quan Reseller Dashboard (Tính năng mới)", "GET /api/v1/reseller/dashboard", "Token Reseller", "200", 200, "PASS", "245ms", "Đạt chuẩn SRS: Trả về KPI tổng clicks, conversions, pending/confirmed commissions."),
        (25, "DSH-02", "Dashboard", "RBAC: Admin cố gọi API Dashboard của Reseller", "GET /api/v1/reseller/dashboard", "Bearer token của Admin", "403", 403, "PASS", "67ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(RESELLER)') bảo vệ endpoint Dashboard."),

        # Nhóm 5: TikTok OAuth & Social Video (9 test cases)
        (26, "TIK-01", "TikTok-Sec", "RBAC: Admin cố gọi API lấy link TikTok OAuth của Reseller", "GET /api/v1/reseller/social-accounts/tiktok/oauth-url", "Bearer token của Admin", "403", 403, "PASS", "66ms", "Đạt chuẩn SRS: Chặn Admin gọi API tài khoản mạng xã hội của Reseller."),
        (27, "TIK-02", "TikTok-Sec", "RBAC: Admin cố gọi API đăng ký Social Video của Reseller", "POST /api/v1/reseller/social-videos", "Bearer token của Admin", "403", 403, "PASS", "106ms", "Đạt chuẩn SRS: Chặn Admin tạo liên kết video tiếp thị thay Reseller."),
        (28, "TIK-03", "TikTok-SRS", "Reseller lấy URL đăng nhập TikTok OAuth", "GET /api/v1/reseller/social-accounts/tiktok/oauth-url", "Token Reseller hợp lệ", "200/400/409/503", 409, "PASS", "70ms", "Đạt chuẩn: Trả về 409 khi tài khoản chưa cấu hình TikTok profile, không crash 500."),
        (29, "TIK-04", "TikTok-Edge", "Gọi TikTok Callback với state giả mạo / thiếu code", "GET /api/v1/reseller/social-accounts/tiktok/callback", "Query: ?code=fake&state=tampered", "400/401/403", 400, "PASS", "63ms", "Đạt chuẩn: Bắt lỗi state tampered trả 400 Bad Request, bảo vệ CSRF an toàn."),
        (30, "TIK-05", "TikTok-Val", "Gửi Body rỗng {} khi đăng ký Social Video", "POST /api/v1/reseller/social-videos", "Body: {}", "400", 400, "PASS", "109ms", "Đạt chuẩn: @Valid phát hiện thiếu postUrl, campaignApplicationId."),
        (31, "TIK-06", "TikTok-Val", "Gửi URL không hợp lệ & UUID không hợp lệ khi đăng ký Video", "POST /api/v1/reseller/social-videos", "postUrl='not-a-valid-url', UUID giả", "400", 400, "PASS", "68ms", "Đạt chuẩn: Regex kiểm tra URL và bắt lỗi ràng buộc tham chiếu."),
        (32, "TIK-07", "TikTok-Edge", "Reseller chưa liên kết TikTok gọi lấy danh sách Video", "GET /api/v1/reseller/social-accounts/tiktok/videos", "Token Reseller chưa link TikTok", "200/400/404", 404, "PASS", "138ms", "Đạt chuẩn: Trả về 404 thông báo chưa có tài khoản liên kết, không ném 500."),
        (33, "TIK-08", "TikTok-Val", "Truyền sai định dạng UUID khi xem Video theo socialAccountId", "GET /api/v1/reseller/social-accounts/tiktok/not-a-uuid/videos", "Path Variable: not-a-uuid", "400", 400, "PASS", "98ms", "Đạt chuẩn: Bắt lỗi chuyển đổi UUID trả về 400 Bad Request."),
        (34, "TIK-09", "TikTok-Edge", "IDOR: Xem Video của socialAccountId không tồn tại / của người khác", "GET /api/v1/reseller/social-accounts/tiktok/{id}/videos", "UUID: 00000000-0000-0000-0000-000000000099", "403/404", 404, "PASS", "91ms", "Đạt chuẩn Security: Không tiết lộ thông tin người khác, trả về 404 Not Found."),

        # Nhóm 6: Merchant Reseller Rankings (3 test cases)
        (35, "RNK-01", "Merchant-Rank", "Merchant xem bảng xếp hạng Reseller theo Commission", "GET /api/v1/merchant/resellers/commission-ranking", "Token Merchant, params currency=VND, page=0, size=5", "200", 200, "PASS", "154ms", "Đạt chuẩn SRS: Trả về bảng xếp hạng Reseller đóng góp hoa hồng cao nhất."),
        (36, "RNK-02", "Merchant-Rank", "Merchant xem bảng xếp hạng Reseller theo Score TikTok", "GET /api/v1/merchant/resellers/score-ranking", "Token Merchant", "200", 200, "PASS", "165ms", "Đạt chuẩn SRS: Trả về danh sách Reseller có điểm tương tác mạng xã hội cao."),
        (37, "RNK-03", "Merchant-Rank", "Lọc Commission Ranking với currency không hỗ trợ (USD)", "GET /api/v1/merchant/resellers/commission-ranking", "Query: currency=USD&page=0&size=5", "200/400", 200, "PASS", "95ms", "Đạt chuẩn: Xử lý an toàn danh sách trống hoặc trả 200 OK với empty content.")
    ]

    for item in test_data:
        row_num += 1
        stt, code, cat, name, ep, inp, exp, act, res, dur, note = item

        # Cập nhật kết quả thực tế nếu có từ file JSON chạy tự động
        if code in actual_results:
            act = actual_results[code]["Actual"]
            res = actual_results[code]["Result"]
            dur = actual_results[code]["Duration"]

        ws1.cell(row=row_num, column=1, value=stt).alignment = align_center
        ws1.cell(row=row_num, column=2, value=code).alignment = align_center
        ws1.cell(row=row_num, column=3, value=cat).alignment = align_center
        ws1.cell(row=row_num, column=4, value=name).alignment = align_left
        ws1.cell(row=row_num, column=5, value=ep).alignment = align_left
        ws1.cell(row=row_num, column=6, value=inp).alignment = align_left
        ws1.cell(row=row_num, column=7, value=exp).alignment = align_center
        ws1.cell(row=row_num, column=8, value=act).alignment = align_center
        
        res_cell = ws1.cell(row=row_num, column=9, value=res)
        res_cell.alignment = align_center
        if res == "PASS":
            res_cell.font = font_pass
            res_cell.fill = fill_pass
        else:
            res_cell.font = font_fail
            res_cell.fill = fill_fail

        ws1.cell(row=row_num, column=10, value=dur).alignment = align_center
        ws1.cell(row=row_num, column=11, value=note).alignment = align_left

        for c in range(1, 12):
            cell_c = ws1.cell(row=row_num, column=c)
            cell_c.border = border_cell
            if c != 9:
                cell_c.font = font_body
            if row_num % 2 == 1 and c != 9:
                cell_c.fill = fill_zebra

        ws1.row_dimensions[row_num].height = 24

    # Summary row at bottom
    total_cnt = len(test_data)
    pass_cnt = sum(1 for item in test_data if (actual_results[item[1]]["Result"] if item[1] in actual_results else item[8]) == "PASS")
    fail_cnt = total_cnt - pass_cnt
    pass_rate = round((pass_cnt / total_cnt) * 100, 1) if total_cnt else 0
    row_num += 2
    ws1.cell(row=row_num, column=2, value="TỔNG KẾT:").font = font_bold
    ws1.cell(row=row_num, column=4, value=f"Tổng số test: {total_cnt}  |  PASS: {pass_cnt} ({pass_rate}%)  |  FAIL: {fail_cnt} ({round(100-pass_rate,1)}%)").font = font_bold

    # =========================================================================
    # TAB 2: BUGS (DEFECT LOG THEO SRS)
    # =========================================================================
    ws2 = wb.create_sheet(title="Bugs (Defect Log)")
    ws2.views.sheetView[0].showGridLines = True

    # Title
    ws2.merge_cells("A1:K1")
    ws2["A1"] = "DANH SÁCH LỖI VÀ THEO DÕI KHẮC PHỤC (DEFECT LOG THEO CHUẨN SRS)"
    ws2["A1"].font = font_title
    ws2["A1"].alignment = Alignment(horizontal="left", vertical="center")

    ws2["A2"] = "Dự án: AI Travel Reseller Network Platform | Người lập: Hiền (QA/Tester) | Nhánh: test/Hien-reseller | Ngày: " + datetime.now().strftime("%d/%m/%Y")
    ws2["A2"].font = font_subtitle

    headers_ws2 = [
        "STT", "Mã Bug", "Trạng Thái (Status)", "Mức Độ (Severity)", "Tên Lỗi / Tóm Tắt",
        "Endpoint Bị Ảnh Hưởng", "Các Bước Tái Hiện (Steps to Reproduce)",
        "Kết Quả Thực Tế (Actual Result)", "Kết Quả Mong Đợi (Expected Result theo SRS)",
        "Nguyên Nhân Kỹ Thuật (Root Cause Analysis)", "Đề Xuất Khắc Phục Cho Dev (Recommendation)"
    ]

    row_num2 = 4
    for col_idx, h in enumerate(headers_ws2, 1):
        cell = ws2.cell(row=row_num2, column=col_idx, value=h)
        cell.font = font_header
        cell.fill = fill_header_red
        cell.alignment = align_center
        cell.border = border_cell
    ws2.row_dimensions[row_num2].height = 28

    bug_data = [
        (
            1, "BUG-02", "OPEN (Chưa sửa)", "High (Nghiêm trọng)",
            "Lỗi HTTP 500 Internal Server Error khi Reseller nộp đơn tham gia chiến dịch (Apply to Campaign)",
            "POST /api/v1/reseller/campaigns/{id}/applications",
            "1. Đăng nhập bằng tài khoản Reseller ('reseller1@test.com') lấy Bearer Token.\n2. Lấy 1 Campaign ID hợp lệ đang ACTIVE từ CSDL (ví dụ '50000000-0000-0000-0000-000000000001').\n3. Gửi HTTP POST tới /api/v1/reseller/campaigns/{id}/applications kèm Bearer Token.",
            "Server trả về HTTP 500 Internal Server Error (Database constraint violation).",
            "Lần đầu nộp đơn: Phải ghi nhận đơn thành công, trả về HTTP 200 OK hoặc 201 Created. Nếu nộp đơn trùng lần 2: Phải trả về HTTP 409 Conflict ('Reseller already applied').",
            "Trong CSDL bảng 'campaign_applications' (Flyway migration V7), hai cột 'commission_type_snapshot' (VARCHAR) và 'commission_rate_snapshot' (NUMERIC) được khai báo là NOT NULL. Tuy nhiên trong mã nguồn 'ResellerApplicationRepository.create()', câu lệnh SQL INSERT chỉ chèn (id, campaign_id, reseller_id, status, applied_at) mà bỏ quên 2 cột snapshot này. PostgreSQL ném lỗi: null value in column 'commission_type_snapshot' violates not-null constraint.",
            "Bổ sung việc đọc commission_type và commission_rate từ bảng campaigns để điền vào 2 cột commission_type_snapshot và commission_rate_snapshot trong câu lệnh SQL INSERT tại ResellerApplicationRepository.create()."
        ),
        (
            2, "BUG-01", "RESOLVED (Đã sửa)", "Medium (Trung bình)",
            "Thiếu validation kiểm tra xung đột giữa loại hoa hồng cố định (FIXED_AMOUNT) và tỷ lệ phần trăm",
            "GET /api/v1/reseller/campaigns",
            "1. Đăng nhập bằng tài khoản Reseller lấy Bearer Token.\n2. Gửi request GET /api/v1/reseller/campaigns?commissionType=FIXED_AMOUNT&minCommissionPercent=10.",
            "Trước đây Server trả về HTTP 200 OK và bỏ qua kiểm tra tính hợp lý của tham số.",
            "SRS quy định hoa hồng cố định (FIXED_AMOUNT) không tồn tại tỷ lệ %, hệ thống bắt buộc phải từ chối request với mã lỗi HTTP 400 Bad Request.",
            "Trước commit 468d41c, ResellerCampaignController và ResellerCampaignService không có logic kiểm tra ràng buộc chéo giữa 2 tham số query này.",
            "Dev đã sửa thành công trong commit 468d41c: Bổ sung logic if ('FIXED_AMOUNT'.equals(commissionType) && minCommissionPercent != null) throw new IllegalArgumentException(...) trong ResellerCampaignService.java, được GlobalExceptionHandler bắt và trả về HTTP 400 Bad Request."
        ),
        (
            3, "BUG-03", "RESOLVED (Đã sửa)", "High (Nghiêm trọng)",
            "Lỗi HTTP 500 Internal Server Error khi xem danh sách Tracking Links của Reseller do JOIN bảng không tồn tại",
            "GET /api/v1/reseller/tracking-links",
            "1. Đăng nhập bằng tài khoản Reseller lấy Bearer Token.\n2. Gửi request GET tới /api/v1/reseller/tracking-links.",
            "Trước đây Server trả về HTTP 500 Internal Server Error (relation 'travel_v2.tracking_clicks' does not exist).",
            "Trả về HTTP 200 OK cùng danh sách liên kết tiếp thị của Reseller (kèm số lượt click và lượt chuyển đổi đặt tour).",
            "Câu lệnh SQL cũ trong ResellerTrackingLinkRepository.java thực hiện LEFT JOIN tới 2 bảng 'travel_v2.tracking_clicks' và 'travel_v2.conversions'. Tuy nhiên trong toàn bộ schema CSDL hiện tại chưa tạo 2 bảng này.",
            "Dev đã sửa thành công trong commit 468d41c: Loại bỏ JOIN bảng tracking_clicks, lấy trực tiếp số lượt click từ cột 'click_count' có sẵn trong bảng tracking_links và đếm số lượng đặt phòng thành công qua bảng 'bookings'."
        ),
        (
            4, "BUG-04", "RESOLVED (Đã sửa)", "High (Nghiêm trọng)",
            "Lỗi HTTP 500 Internal Server Error khi xem danh sách Hoa hồng (Commissions) do SELECT cột không tồn tại",
            "GET /api/v1/reseller/commissions",
            "1. Đăng nhập bằng tài khoản Reseller lấy Bearer Token.\n2. Gửi request GET tới /api/v1/reseller/commissions.",
            "Trước đây Server trả về HTTP 500 Internal Server Error (column 'order_amount' does not exist).",
            "Trả về HTTP 200 OK cùng danh sách các khoản hoa hồng của Reseller kèm số tiền đơn hàng và trạng thái chi trả.",
            "Câu lệnh SQL cũ trong ResellerCommissionRepository.java thực hiện SELECT các cột 'order_amount', 'commission_amount' trực tiếp từ bảng commissions. Trong khi đó schema thực tế bảng commissions lưu số tiền hoa hồng ở cột 'amount' và liên kết thông qua 'booking_id'.",
            "Dev đã sửa thành công trong commit 468d41c: Chỉnh lại câu SQL: SELECT cm.amount AS commission_amount, b.total_amount AS order_amount kết nối thông qua bảng 'bookings'."
        )
    ]

    for b in bug_data:
        row_num2 += 1
        stt, bug_id, status, sev, title, ep, steps, actual, expected, root, rec = b

        ws2.cell(row=row_num2, column=1, value=stt).alignment = align_center
        ws2.cell(row=row_num2, column=2, value=bug_id).alignment = align_center
        
        status_cell = ws2.cell(row=row_num2, column=3, value=status)
        status_cell.alignment = align_center
        if "OPEN" in status:
            status_cell.font = font_bug_open
            status_cell.fill = fill_bug_open
        else:
            status_cell.font = font_bug_fixed
            status_cell.fill = fill_bug_fixed

        sev_cell = ws2.cell(row=row_num2, column=4, value=sev)
        sev_cell.alignment = align_center
        if "High" in sev:
            sev_cell.font = font_bug_open
            sev_cell.fill = fill_bug_open
        else:
            sev_cell.font = font_bug_med
            sev_cell.fill = fill_bug_med

        ws2.cell(row=row_num2, column=5, value=title).alignment = align_left
        ws2.cell(row=row_num2, column=6, value=ep).alignment = align_left
        ws2.cell(row=row_num2, column=7, value=steps).alignment = align_left
        ws2.cell(row=row_num2, column=8, value=actual).alignment = align_left
        ws2.cell(row=row_num2, column=9, value=expected).alignment = align_left
        ws2.cell(row=row_num2, column=10, value=root).alignment = align_left
        ws2.cell(row=row_num2, column=11, value=rec).alignment = align_left

        for c in range(1, 12):
            cell_c = ws2.cell(row=row_num2, column=c)
            cell_c.border = border_cell
            if c not in (3, 4):
                cell_c.font = font_body
            if row_num2 % 2 == 1 and c not in (3, 4):
                cell_c.fill = fill_zebra

        ws2.row_dimensions[row_num2].height = 70

    # =========================================================================
    # TỰ ĐỘNG ĐIỀU CHỈNH ĐỘ RỘNG CỘT CHO ĐẸP VÀ DỄ ĐỌC
    # =========================================================================
    ws1_widths = {1: 6, 2: 12, 3: 16, 4: 36, 5: 38, 6: 40, 7: 16, 8: 15, 9: 12, 10: 12, 11: 48}
    for col_idx, width in ws1_widths.items():
        ws1.column_dimensions[get_column_letter(col_idx)].width = width

    ws2_widths = {1: 6, 2: 12, 3: 18, 4: 18, 5: 34, 6: 34, 7: 38, 8: 28, 9: 36, 10: 46, 11: 42}
    for col_idx, width in ws2_widths.items():
        ws2.column_dimensions[get_column_letter(col_idx)].width = width

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    wb.save(output_path)
    print(f">> [SUCCESS] Đã tạo file Excel báo cáo tại: {output_path}")

if __name__ == "__main__":
    out_file = "docs/Bao_cao_Kiem_thu_Toan_Bo_Reseller_Hien.xlsx"
    json_file = "scripts/test-results.json" if os.path.exists("scripts/test-results.json") else None
    if len(sys.argv) > 1:
        out_file = sys.argv[1]
    if len(sys.argv) > 2:
        json_file = sys.argv[2]
    create_report(out_file, json_file)
