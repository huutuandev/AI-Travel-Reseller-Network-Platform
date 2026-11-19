# BÁO CÁO KIỂM THỬ API CHUYÊN SÂU THEO CHUẨN SRS (HIỀN)

- **Người thực hiện:** Hiền
- **Nhánh thực hiện:** `test/Hien-api-edgecases-security`
- **Dự án:** AI Travel Reseller Network Platform
- **Mục tiêu:** Kiểm thử chất lượng API mở rộng theo tài liệu đặc tả SRS, tập trung đánh giá tính an toàn bảo mật (RBAC, Token, Injection), tính toàn vẹn dữ liệu (Data Validation) và khả năng xử lý các trường hợp biên/ngoại lệ (Edge Cases).

---

## 1. PHẠM VI NGHIỆP VỤ SRS VỀ TÀI KHOẢN & ĐĂNG KÝ
Theo đặc tả nghiệp vụ của dự án B2B Reseller Network:
- Nền tảng **không mở chức năng tự do đăng ký người dùng (Public Self-Registration)**.
- Tài khoản Reseller và Merchant được cấp tập trung qua Quản trị viên Platform Admin hoặc qua hệ thống Seed Data được duyệt trước.
- Vì vậy, nhóm kiểm thử xác thực dữ liệu đầu vào (Data Validation) tập trung vào luồng **Đăng nhập (Login)**, **Truy vấn tham số (Query Parameters)** và **Định dạng khóa định danh (UUID Path Variables)**.

---

## 2. KẾT QUẢ KIỂM THỬ THỰC TẾ (AUTOMATION TEST RESULTS)

- **Tổng số kịch bản:** 19
- **Số lượng PASS:** 13 (68.4%)
- **Số lượng FAIL (Phát hiện Bug):** 6 (31.6%)

| Mã Test | Nhóm | Tên Kịch Bản | Kỳ Vọng | Thực Tế | Kết Quả |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **SEC-01** | Security | Chặn truy cập không có Token | `401/403` | 401 | **PASS** |
| **SEC-02** | Security | Chặn Token giả mạo / sai chữ ký | `401/403` | 401 | **PASS** |
| **SEC-03** | Security | Phân quyền RBAC: Reseller không vào được Admin API | `403` | 403 | **PASS** |
| **SEC-04** | Security | Phân quyền RBAC: Admin không gọi được Reseller API | `403` | 403 | **PASS** |
| **SEC-05** | Security | Chống SQL Injection trong ô tìm kiếm `q` | `200/400` | 200 | **PASS** |
| **VAL-01** | Validation | Sai định dạng UUID trên Path Variable | `400` | 400 | **PASS** |
| **VAL-02** | Validation | Gửi Body rỗng `{}` khi Login | `400` | 400 | **PASS** |
| **VAL-03** | Validation | Email sai quy cách khi Login | `400` | 400 | **PASS** |
| **VAL-04** | Validation | Mật khẩu rỗng khi Login | `400` | 400 | **PASS** |
| **VAL-05** | Validation | Xung đột tham số FIXED_AMOUNT và minCommissionPercent | `400` | 200 | **FAIL (Bug #01)** |
| **SRS-01** | Business | Lấy danh sách Campaign Marketplace (chỉ ACTIVE & AVAILABLE) | `200` | 200 | **PASS** |
| **SRS-02** | Business | Xem chi tiết Campaign hợp lệ theo UUID | `200` | 200 | **PASS** |
| **EDG-01** | Edge Case | UUID đúng format nhưng không tồn tại trong CSDL | `404` | 404 | **PASS** |
| **SRS-03** | Business | Reseller gửi đơn tham gia chiến dịch (Apply) | `201/200` | 500 | **FAIL (Bug #02)** |
| **EDG-02** | Edge Case | Chặn đăng ký trùng chiến dịch (Duplicate Apply -> 409 Conflict) | `409` | 500 | **FAIL (Bug #02)** |
| **EDG-03** | Edge Case | Apply vào Campaign không tồn tại / không đủ điều kiện | `400/404` | 500 | **FAIL (Bug #02)** |
| **SRS-04** | Business | Lấy danh sách chiến dịch đã đăng ký (My Applications) | `200` | 200 | **PASS** |
| **SRS-05** | Business | Lấy danh sách Tracking Links của Reseller | `200` | 500 | **FAIL (Bug #03)** |
| **SRS-06** | Business | Lấy danh sách Commissions (Hoa hồng) của Reseller | `200` | 500 | **FAIL (Bug #04)** |

---

## 3. BẢNG PHÂN TÍCH DEFECT LOG (BÁO CÁO LỖI CHO ĐỘI PHÁT TRIỂN)

### 🐛 Bug #01 (Mức độ: Medium) - Thiếu Validation xung đột tham số lọc hoa hồng
- **Endpoint:** `GET /api/v1/reseller/campaigns?commissionType=FIXED_AMOUNT&minCommissionPercent=10`
- **Mô tả:** SRS quy định hoa hồng cố định không có tỷ lệ %, việc truyền cả 2 tham số phải trả về lỗi `400 Bad Request`. Hiện tại Controller chưa có logic bắt lỗi này nên vẫn trả về `200 OK`.

### 🐛 Bug #02 (Mức độ: High) - Lỗi 500 khi Reseller nộp đơn tham gia chiến dịch (Apply)
- **Endpoint:** `POST /api/v1/reseller/campaigns/{id}/applications`
- **Nguyên nhân kỹ thuật:** CSDL bảng `campaign_applications` (Flyway migration V7) có 2 cột bắt buộc `NOT NULL` là `commission_type_snapshot` và `commission_rate_snapshot`. Tuy nhiên, câu lệnh SQL INSERT trong `ResellerApplicationRepository.java` lại bỏ sót 2 cột này, dẫn đến PostgreSQL quăng lỗi vi phạm rằng buộc NOT NULL và server trả về 500.

### 🐛 Bug #03 (Mức độ: High) - Lỗi 500 khi xem danh sách Tracking Links
- **Endpoint:** `GET /api/v1/reseller/tracking-links`
- **Nguyên nhân kỹ thuật:** Câu lệnh SQL trong `ResellerTrackingLinkRepository.java` thực hiện JOIN tới 2 bảng `travel_v2.tracking_clicks` và `travel_v2.conversions`. Nhưng trong cấu trúc database hiện tại chưa tồn tại 2 bảng này, gây lỗi `relation does not exist`.

### 🐛 Bug #04 (Mức độ: High) - Lỗi 500 khi xem danh sách Hoa hồng (Commissions)
- **Endpoint:** `GET /api/v1/reseller/commissions`
- **Nguyên nhân kỹ thuật:** File `ResellerCommissionRepository.java` truy vấn các cột `order_amount`, `commission_amount`, `campaign_id` trên bảng `travel_v2.commissions`. Trong khi theo schema CSDL (Flyway V11), các cột này không tồn tại trực tiếp trên bảng `commissions` (tên cột là `amount` và liên kết qua `booking_id`).
