# -*- coding: utf-8 -*-
"""
Script xuất Báo cáo Kiểm thử API chuyên sâu (Security, Data Validation, Edge Cases)
Xuất ra file Excel gồm 2 Sheet:
  - Tab 1: 'Test Cases' (Chi tiết 19 kịch bản kiểm thử)
  - Tab 2: 'Bugs (Defect Log)' (Chi tiết các lỗi phát hiện theo SRS)
"""

import sys
import json
import os
from datetime import datetime
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
from openpyxl.utils import get_column_letter

def create_report(output_path="docs/Bao_cao_Kiem_thu_API_Chuyen_sau_Hien.xlsx", results_json_path=None):
    wb = openpyxl.Workbook()
    # Xóa sheet mặc định
    wb.remove(wb.active)

    # Đọc kết quả từ JSON nếu có
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

    font_bug_high = Font(name="Calibri", size=11, bold=True, color="990000")
    fill_bug_high = PatternFill(start_color="F4CCCC", end_color="F4CCCC", fill_type="solid")
    
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
    # TAB 1: TEST CASES
    # =========================================================================
    ws1 = wb.create_sheet(title="Test Cases")
    ws1.views.sheetView[0].showGridLines = True

    # Header info
    ws1.merge_cells("A1:J1")
    ws1["A1"] = "BÁO CÁO KẾT QUẢ KIỂM THỬ API CHUYÊN SÂU THEO CHUẨN SRS"
    ws1["A1"].font = font_title
    ws1["A1"].alignment = Alignment(horizontal="left", vertical="center")

    ws1["A2"] = "Dự án: AI Travel Reseller Network Platform | Người thực hiện: Hiền | Nhánh: test/Hien-api-edgecases-security | Ngày: " + datetime.now().strftime("%d/%m/%Y")
    ws1["A2"].font = font_subtitle

    # Summary box
    ws1["A3"] = "Phạm vi: Security (RBAC, Token, Injection) | Data Validation (Login, Format, Path) | Edge Cases & SRS Business"
    ws1["A3"].font = font_subtitle

    headers_ws1 = [
        "STT", "Mã Test", "Nhóm Kiểm Thử", "Tên Kịch Bản Kiểm Thử",
        "Method & Endpoint", "Dữ Liệu Đầu Vào / Thao Tác",
        "HTTP Kỳ Vọng", "HTTP Thực Tế", "Kết Quả", "Thời Gian", "Đánh Giá / Ghi Chú Theo SRS"
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
        # Nhóm 1: Security
        (1, "SEC-01", "Security", "Chặn truy cập không xác thực (No Token)", "GET /api/v1/reseller/campaigns", "Không truyền header Authorization", "401/403", 401, "PASS", "36ms", "Đạt chuẩn SRS: Spring Security Filter chặn thành công."),
        (2, "SEC-02", "Security", "Chặn truy cập với Token giả mạo/sai chữ ký", "GET /api/v1/reseller/campaigns", "Authorization: Bearer invalid.fake.token", "401/403", 401, "PASS", "4ms", "Đạt chuẩn SRS: JwtFilter xác thực chữ ký số hợp lệ."),
        (3, "SEC-03", "Security", "Phân quyền RBAC: Reseller gọi API Admin", "GET /api/v1/admin/tenants", "Bearer token của Reseller", "403", 403, "PASS", "8ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(ADMIN)') chặn thành công."),
        (4, "SEC-04", "Security", "Phân quyền RBAC: Admin gọi API Reseller", "GET /api/v1/reseller/campaigns", "Bearer token của Admin", "403", 403, "PASS", "8ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(RESELLER)') chặn đúng vai trò."),
        (5, "SEC-05", "Security", "Bảo vệ SQL Injection trong ô tìm kiếm", "GET /api/v1/reseller/campaigns?q=...", "q=' OR 1=1; DROP TABLE users; --", "200/400", 200, "PASS", "20ms", "Đạt chuẩn SRS: Tham số được bind an toàn, không sập 500."),

        # Nhóm 2: Data Validation
        (6, "VAL-01", "Validation", "Sai định dạng UUID trên Path Variable", "GET /api/v1/reseller/campaigns/not-a-uuid", "Path Variable truyền chuỗi chữ không phải UUID", "400", 400, "PASS", "8ms", "Đạt chuẩn: Bắt lỗi type mismatch chuyển đổi sang 400 Bad Request."),
        (7, "VAL-02", "Validation", "Body rỗng khi Login ({})", "POST /api/v1/auth/login", "Body: {}", "400", 400, "PASS", "5ms", "Đạt chuẩn: @Valid trên LoginRequest phát hiện thiếu các trường bắt buộc."),
        (8, "VAL-03", "Validation", "Email sai định dạng khi Login", "POST /api/v1/auth/login", "{\"email\":\"not-an-email-format\",\"password\":\"Reseller@123\"}", "400", 400, "PASS", "15ms", "Đạt chuẩn: Regex @Email phát hiện format không hợp lệ."),
        (9, "VAL-04", "Validation", "Mật khẩu rỗng khi Login", "POST /api/v1/auth/login", "{\"email\":\"reseller1@test.com\",\"password\":\"\"}", "400", 400, "PASS", "15ms", "Đạt chuẩn: @NotBlank phát hiện mật khẩu rỗng."),
        (10, "VAL-05", "Validation", "Xung đột tham số FIXED_AMOUNT và minCommissionPercent", "GET /api/v1/reseller/campaigns", "Query: commissionType=FIXED_AMOUNT&minCommissionPercent=10", "400", 200, "FAIL", "16ms", "Lỗi Bug #01: SRS yêu cầu từ chối 400 vì Fixed Amount không có %, backend đang trả 200."),

        # Nhóm 3: SRS Business & Edge Cases
        (11, "SRS-01", "SRS-Business", "Lấy danh sách Campaign Marketplace", "GET /api/v1/reseller/campaigns", "Token Reseller hợp lệ", "200", 200, "PASS", "17ms", "Đạt chuẩn SRS: Chỉ trả về Campaign ACTIVE và Product AVAILABLE."),
        (12, "SRS-02", "SRS-Business", "Xem chi tiết Campaign hợp lệ theo UUID", "GET /api/v1/reseller/campaigns/{id}", "ID chiến dịch có thật trong DB", "200", 200, "PASS", "18ms", "Đạt chuẩn SRS: Trả về thông tin chi tiết, terms và hoa hồng."),
        (13, "EDG-01", "SRS-EdgeCase", "UUID hợp lệ nhưng không tồn tại trong DB", "GET /api/v1/reseller/campaigns/{id}", "UUID: 00000000-0000-0000-0000-000000000099", "404", 404, "PASS", "11ms", "Đạt chuẩn: Ném ResourceNotFoundException chuyển sang 404 Not Found."),
        (14, "SRS-03", "SRS-Business", "Reseller nộp đơn tham gia chiến dịch (Apply)", "POST /api/v1/reseller/campaigns/{id}/applications", "Token Reseller hợp lệ, nộp đơn lần đầu", "201/200", 500, "FAIL", "17ms", "Lỗi Bug #02: Lỗi CSDL V7 thiếu 2 cột snapshot NOT NULL trong lệnh INSERT."),
        (15, "EDG-02", "SRS-EdgeCase", "Chặn đăng ký trùng chiến dịch (Duplicate Apply)", "POST /api/v1/reseller/campaigns/{id}/applications", "Nộp đơn lần 2 vào cùng chiến dịch", "409", 500, "FAIL", "33ms", "Lỗi Bug #02: Bị crash 500 từ tầng INSERT nên không đến được bước check 409 Conflict."),
        (16, "EDG-03", "SRS-EdgeCase", "Apply vào Campaign không tồn tại/không khả dụng", "POST /api/v1/reseller/campaigns/{id}/applications", "ID không tồn tại hoặc campaign hết hạn", "400/404", 500, "FAIL", "24ms", "Lỗi Bug #02: Bị crash 500 do truy vấn kiểm tra điều kiện chiến dịch."),
        (17, "SRS-04", "SRS-Business", "Lấy danh sách chiến dịch đã đăng ký (My Applications)", "GET /api/v1/reseller/campaigns/applications", "Token Reseller", "200", 200, "PASS", "28ms", "Đạt chuẩn SRS: Lấy đúng danh sách chiến dịch của chính Reseller."),
        (18, "SRS-05", "SRS-Business", "Lấy danh sách Tracking Links của Reseller", "GET /api/v1/reseller/tracking-links", "Token Reseller", "200", 500, "FAIL", "16ms", "Lỗi Bug #03: SQL JOIN 2 bảng tracking_clicks và conversions chưa tồn tại trong CSDL."),
        (19, "SRS-06", "SRS-Business", "Lấy danh sách Commissions (Hoa hồng) của Reseller", "GET /api/v1/reseller/commissions", "Token Reseller", "200", 500, "FAIL", "17ms", "Lỗi Bug #04: SQL SELECT cột order_amount, commission_amount không có trong bảng commissions."),
        
        # Nhóm 4: TikTok OAuth & Social Video
        (20, "TIK-01", "TikTok-Sec", "RBAC: Admin cố gọi API lấy link TikTok OAuth", "GET /api/v1/reseller/social-accounts/tiktok/oauth-url", "Bearer token của Admin", "403", 403, "PASS", "123ms", "Đạt chuẩn SRS: @PreAuthorize('hasRole(RESELLER)') chặn Admin thành công."),
        (21, "TIK-02", "TikTok-Sec", "RBAC: Admin cố gọi API đăng ký Social Video", "POST /api/v1/reseller/social-videos", "Bearer token của Admin + JSON hợp lệ", "403", 403, "PASS", "50ms", "Đạt chuẩn SRS: Chặn truy cập chéo vai trò từ Admin."),
        (22, "TIK-03", "TikTok-SRS", "Reseller lấy URL đăng nhập TikTok OAuth", "GET /api/v1/reseller/social-accounts/tiktok/oauth-url", "Token Reseller hợp lệ", "200/400/409/503", 409, "PASS", "57ms", "Đạt chuẩn: Trả về 409 rõ ràng khi chưa cấu hình TikTok app/profile."),
        (23, "TIK-04", "TikTok-Edge", "Gọi TikTok Callback với state giả mạo / thiếu code", "GET /api/v1/reseller/social-accounts/tiktok/callback", "Query: ?code=fake&state=tampered", "400/401/403", 400, "PASS", "76ms", "Đạt chuẩn: Bắt lỗi state không hợp lệ trả 400 Bad Request, không crash 500."),
        (24, "TIK-05", "TikTok-Val", "Gửi Body rỗng {} khi đăng ký Social Video", "POST /api/v1/reseller/social-videos", "Body: {}", "400", 400, "PASS", "50ms", "Đạt chuẩn: @Valid trên RegisterSocialVideoRequest phát hiện thiếu field."),
        (25, "TIK-06", "TikTok-Val", "Gửi URL & UUID không tồn tại khi đăng ký Video", "POST /api/v1/reseller/social-videos", "postUrl='not-a-valid-url', UUID giả", "400", 400, "PASS", "61ms", "Đạt chuẩn: Trả về 400 Bad Request khi URL hoặc liên kết không hợp lệ."),
        (26, "TIK-07", "TikTok-Edge", "Reseller chưa liên kết TikTok gọi lấy danh sách Video", "GET /api/v1/reseller/social-accounts/tiktok/videos", "Token Reseller chưa link TikTok", "200/400/404", 404, "PASS", "109ms", "Đạt chuẩn: Thông báo 404 rõ ràng chưa có tài khoản TikTok, không crash 500."),
        (27, "TIK-08", "TikTok-Val", "Truyền sai định dạng UUID khi xem Video theo ID", "GET /api/v1/reseller/social-accounts/tiktok/not-a-uuid/videos", "Path Variable: not-a-uuid", "400", 400, "PASS", "42ms", "Đạt chuẩn: Chuyển đổi lỗi ép kiểu UUID sang 400 Bad Request."),
        (28, "TIK-09", "TikTok-Edge", "IDOR: Xem Video của socialAccountId không tồn tại/người khác", "GET /api/v1/reseller/social-accounts/tiktok/{id}/videos", "UUID: 00000000-0000-0000-0000-000000000099", "403/404", 404, "PASS", "72ms", "Đạt chuẩn Security: Không lộ dữ liệu người khác, trả về 404.")
    ]

    for item in test_data:
        row_num += 1
        stt, code, cat, name, ep, inp, exp, act, res, dur, note = item

        # Cập nhật kết quả thực tế nếu có từ file JSON
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
    # TAB 2: BUGS (DEFECT LOG)
    # =========================================================================
    ws2 = wb.create_sheet(title="Bugs (Defect Log)")
    ws2.views.sheetView[0].showGridLines = True

    # Title
    ws2.merge_cells("A1:I1")
    ws2["A1"] = "DANH SÁCH LỖI PHÁT HIỆN ĐƯỢC (DEFECT LOG THEO SRS)"
    ws2["A1"].font = font_title
    ws2["A1"].alignment = Alignment(horizontal="left", vertical="center")

    ws2["A2"] = "Dự án: AI Travel Reseller Network Platform | Người lập: Hiền (QA/Tester) | Trạng thái: New / Cần chuyển giao Dev"
    ws2["A2"].font = font_subtitle

    headers_ws2 = [
        "STT", "Mã Bug", "Mức Độ (Severity)", "Tên Lỗi / Tóm Tắt",
        "Endpoint Bị Ảnh Hưởng", "Các Bước Tái Hiện (Steps to Reproduce)",
        "Kết Quả Thực Tế (Actual Result)", "Kết Quả Mong Đợi (Expected Result theo SRS)",
        "Nguyên Nhân Kỹ Thuật (Root Cause Analysis)", "Đề Xuất Khắc Phục (Recommendation)"
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
            1, "BUG-01", "Medium",
            "Thiếu validation ràng buộc xung đột tham số lọc hoa hồng",
            "GET /api/v1/reseller/campaigns",
            "1. Đăng nhập bằng tài khoản Reseller lấy Bearer token.\n2. Gọi GET /api/v1/reseller/campaigns?commissionType=FIXED_AMOUNT&minCommissionPercent=10.",
            "Server trả về HTTP 200 OK và bỏ qua kiểm tra tham số.",
            "SRS quy định hoa hồng cố định không có tỷ lệ %, hệ thống bắt buộc phải từ chối với lỗi HTTP 400 Bad Request.",
            "Hàm getAllCampaigns() trong ResellerCampaignController.java chưa nhận @RequestParam và chưa có logic kiểm tra if (commissionType == FIXED_AMOUNT && minCommissionPercent != null).",
            "Bổ sung DTO CampaignQuery vào Controller và validate ràng buộc xung đột trước khi truy vấn."
        ),
        (
            2, "BUG-02", "High",
            "Lỗi 500 Internal Server Error khi Reseller gửi đơn tham gia chiến dịch (Apply)",
            "POST /api/v1/reseller/campaigns/{id}/applications",
            "1. Đăng nhập bằng tài khoản Reseller lấy token.\n2. Lấy 1 UUID campaign ACTIVE hợp lệ.\n3. Gửi request POST tới /api/v1/reseller/campaigns/{campaignId}/applications.",
            "Server báo lỗi HTTP 500 Internal Server Error.",
            "Lần đầu nộp đơn phải trả về HTTP 200 OK hoặc 201 Created. Nếu nộp trùng lần 2 phải trả về HTTP 409 Conflict.",
            "Trong CSDL bảng campaign_applications (Flyway migration V7), 2 cột 'commission_type_snapshot' và 'commission_rate_snapshot' là NOT NULL. Nhưng câu lệnh INSERT trong ResellerApplicationRepository.java lại bỏ quên 2 cột này, dẫn đến PostgreSQL ném lỗi vi phạm ràng buộc NOT NULL.",
            "Cập nhật câu lệnh SQL INSERT trong ResellerApplicationRepository.create() để lấy snapshot commission_type và commission_rate từ bảng campaigns chèn vào."
        ),
        (
            3, "BUG-03", "High",
            "Lỗi 500 Internal Server Error khi xem danh sách Tracking Links của Reseller",
            "GET /api/v1/reseller/tracking-links",
            "1. Đăng nhập bằng tài khoản Reseller lấy token.\n2. Gửi request GET tới /api/v1/reseller/tracking-links.",
            "Server báo lỗi HTTP 500 Internal Server Error.",
            "Trả về HTTP 200 OK cùng danh sách link tiếp thị của Reseller (kèm số clicks và conversions mặc định 0).",
            "Câu lệnh SQL trong ResellerTrackingLinkRepository.java thực hiện LEFT JOIN tới 2 bảng 'travel_v2.tracking_clicks' và 'travel_v2.conversions'. Tuy nhiên trong toàn bộ 17 file migration CSDL hiện tại chưa tạo 2 bảng này, gây lỗi PostgreSQL 'relation does not exist'.",
            "Tạm thời bỏ JOIN 2 bảng chưa có hoặc thêm Flyway migration tạo bảng tracking_clicks và conversions."
        ),
        (
            4, "BUG-04", "High",
            "Lỗi 500 Internal Server Error khi xem danh sách Hoa hồng (Commissions) của Reseller",
            "GET /api/v1/reseller/commissions",
            "1. Đăng nhập bằng tài khoản Reseller lấy token.\n2. Gửi request GET tới /api/v1/reseller/commissions.",
            "Server báo lỗi HTTP 500 Internal Server Error.",
            "Trả về HTTP 200 OK cùng danh sách các khoản hoa hồng ghi nhận của Reseller.",
            "Câu lệnh SQL trong ResellerCommissionRepository.java truy vấn các cột 'order_amount', 'commission_amount' và JOIN campaigns c ON c.id = cm.campaign_id. Nhưng theo CSDL thực tế (V11), bảng commissions chỉ có cột 'amount' và liên kết thông qua booking_id.",
            "Sửa lại câu truy vấn SQL trong ResellerCommissionRepository để SELECT đúng cột amount và JOIN qua bảng bookings."
        )
    ]

    for b in bug_data:
        row_num2 += 1
        stt, bug_id, sev, title, ep, steps, actual, expected, root, rec = b

        ws2.cell(row=row_num2, column=1, value=stt).alignment = align_center
        ws2.cell(row=row_num2, column=2, value=bug_id).alignment = align_center
        
        sev_cell = ws2.cell(row=row_num2, column=3, value=sev)
        sev_cell.alignment = align_center
        if sev == "High":
            sev_cell.font = font_bug_high
            sev_cell.fill = fill_bug_high
        else:
            sev_cell.font = font_bug_med
            sev_cell.fill = fill_bug_med

        ws2.cell(row=row_num2, column=4, value=title).alignment = align_left
        ws2.cell(row=row_num2, column=5, value=ep).alignment = align_left
        ws2.cell(row=row_num2, column=6, value=steps).alignment = align_left
        ws2.cell(row=row_num2, column=7, value=actual).alignment = align_left
        ws2.cell(row=row_num2, column=8, value=expected).alignment = align_left
        ws2.cell(row=row_num2, column=9, value=root).alignment = align_left
        ws2.cell(row=row_num2, column=10, value=rec).alignment = align_left

        for c in range(1, 11):
            cell_c = ws2.cell(row=row_num2, column=c)
            cell_c.border = border_cell
            if c != 3:
                cell_c.font = font_body
            if row_num2 % 2 == 1 and c != 3:
                cell_c.fill = fill_zebra

        ws2.row_dimensions[row_num2].height = 65

    # =========================================================================
    # TỰ ĐỘNG ĐIỀU CHỈNH ĐỘ RỘNG CỘT CHO ĐẸP VÀ DỄ ĐỌC
    # =========================================================================
    ws1_widths = {1: 6, 2: 12, 3: 15, 4: 35, 5: 35, 6: 40, 7: 15, 8: 15, 9: 12, 10: 12, 11: 45}
    for col_idx, width in ws1_widths.items():
        ws1.column_dimensions[get_column_letter(col_idx)].width = width

    ws2_widths = {1: 6, 2: 12, 3: 14, 4: 32, 5: 32, 6: 38, 7: 28, 8: 35, 9: 45, 10: 40}
    for col_idx, width in ws2_widths.items():
        ws2.column_dimensions[get_column_letter(col_idx)].width = width

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    wb.save(output_path)
    print(f">> [SUCCESS] Đã tạo file Excel báo cáo tại: {output_path}")

if __name__ == "__main__":
    out_file = "docs/Bao_cao_Kiem_thu_API_Chuyen_sau_Hien.xlsx"
    json_file = "scripts/test-results.json" if os.path.exists("scripts/test-results.json") else None
    if len(sys.argv) > 1:
        out_file = sys.argv[1]
    if len(sys.argv) > 2:
        json_file = sys.argv[2]
    create_report(out_file, json_file)
