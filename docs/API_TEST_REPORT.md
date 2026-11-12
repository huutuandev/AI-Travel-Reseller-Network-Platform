# 📋 BÁO CÁO KẾT QUẢ KIỂM THỬ API HỆ THỐNG (API TEST REPORT)

**Dự án:** AI Travel Reseller Network Platform  
**Vị trí thực hiện:** QA API Test Engineer  
**Môi trường thử nghiệm:** Docker Local Environment (`travel_v2` schema)  
**Thời gian thực thi:** 10/11/2026 20:41:56 (GMT+7)  
**Công cụ kiểm thử:** Postman Collection v2.1 & Newman CLI Engine v6.2.3  
**Base URL:** `http://localhost:8080`

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ (EXECUTIVE SUMMARY)

| Chỉ số | Mục tiêu kỳ vọng | Kết quả thực tế | Đánh giá |
|--------|:----------------:|:---------------:|:--------:|
| **Tổng số HTTP Requests** | 57 | **57** | ✅ 100% |
| **Tổng số Assertions (Kiểm tra)** | 151 | **151** | ✅ 100% |
| **Passed Assertions** | > 95% | **151 (100.0%)** | 🟢 Xuất sắc |
| **Failed Assertions** | 0 | **0 (0.0%)** | 🟢 Không có lỗi |
| **Thời gian chạy toàn bộ collection** | < 30s | **6.2s** | 🟢 Cực nhanh |
| **Thời gian phản hồi TB (Avg Latency)**| < 300ms | **23ms** | 🟢 Vượt chuẩn |
| **Độ trễ thấp nhất (Min Latency)** | - | **4ms** | 🟢 |
| **Độ trễ cao nhất (Max Latency)** | < 1000ms | **147ms** (Login API) | 🟢 Đạt chuẩn |

---

## 2. KẾT QUẢ CHI TIẾT THEO TỪNG NHÓM CHỨC NĂNG

### 2.0 Health Check & OpenAPI Documentation (2/2 Passed)
- **GET** `/swagger-ui/index.html` → `200 OK` (Latency: 38ms) - Giao diện Swagger tải đầy đủ HTML.
- **GET** `/v3/api-docs` → `200 OK` (Latency: 11ms) - Đặc tả OpenAPI 3.1 JSON chuẩn xác.

---

### 2.1 Authentication & Quản lý phiên (10/10 Requests - 100% Passed)
Kiểm thử toàn diện các luồng nghiệp vụ đăng nhập, cấp mới token, và thu hồi phiên làm việc.

| Test ID | Method | Endpoint | Dữ liệu kiểm thử | Kết quả | Latency | Ghi chú |
|:-------:|:------:|----------|------------------|:-------:|:-------:|---------|
| **1.1** | POST | `/api/v1/auth/login` | Admin credentials hợp lệ | **200 OK** | 147ms | Cấp Bearer JWT & Refresh token thành công |
| **1.2** | POST | `/api/v1/auth/login` | Sai mật khẩu (`WrongPassword`) | **401 Unauthorized** | 107ms | Bị từ chối xác thực |
| **1.3** | POST | `/api/v1/auth/login` | Sai format email (`not-an-email`) | **400 Bad Request** | 22ms | Validation bắt lỗi chính xác |
| **1.4** | POST | `/api/v1/auth/login` | Body rỗng `{}` | **400 Bad Request** | 15ms | Validation bắt lỗi trường bắt buộc |
| **1.5** | POST | `/api/v1/auth/login` | Email không tồn tại trong DB | **400 Bad Request** | 35ms | Xử lý an toàn không rò rỉ dữ liệu |
| **1.6** | POST | `/api/v1/auth/refresh` | Refresh token hợp lệ | **200 OK** | 25ms | Xoay vòng (rotate) refresh token thành công |
| **1.7** | POST | `/api/v1/auth/refresh` | Refresh token giả mạo | **400 Bad Request** | 13ms | Từ chối token không tồn tại trong Redis |
| **1.8** | POST | `/api/v1/auth/refresh` | Body rỗng `{}` | **400 Bad Request** | 12ms | Bắt lỗi thiếu token |
| **1.9** | POST | `/api/v1/auth/logout` | Token hợp lệ | **200 OK** | 19ms | Thu hồi refresh token khỏi Redis |
| **1.10**| POST | `/api/v1/auth/login` | Đăng nhập lại sau Logout | **200 OK** | 96ms | Cấp phiên mới hoạt động bình thường |

---

### 2.2 Platform Admin - Quản lý Tenant (17/17 Requests - 100% Passed)
Kiểm thử CRUD, phân trang, bộ lọc tìm kiếm, máy trạng thái (State Machine) và khóa lạc quan (Optimistic Locking).

| Test ID | Method | Endpoint & Tham số | Kịch bản kiểm thử | Kết quả | Latency | Đánh giá |
|:-------:|:------:|---------------------|-------------------|:-------:|:-------:|:--------:|
| **2.1** | GET | `/admin/tenants` | Mặc định `page=0, size=10` | **200 OK** | 21ms | Đạt (Lấy được Tenant ID đầu tiên) |
| **2.2** | GET | `/admin/tenants?page=0&size=5` | Phân trang tùy chỉnh | **200 OK** | 20ms | Đạt (`size=5`, mảng `length <= 5`) |
| **2.3** | GET | `/admin/tenants?status=ACTIVE` | Lọc theo trạng thái `ACTIVE` | **200 OK** | 13ms | Đạt (100% bản ghi trả về là ACTIVE) |
| **2.4** | GET | `/admin/tenants?status=PENDING` | Lọc theo trạng thái `PENDING` | **200 OK** | 19ms | Đạt (Danh sách rỗng hợp lệ) |
| **2.5** | GET | `/admin/tenants?search=travel` | Tìm kiếm theo từ khóa `travel` | **200 OK** | 12ms | Đạt (Tìm thấy bản ghi phù hợp) |
| **2.6** | GET | `/admin/tenants?status=INVALID` | Trạng thái không hợp lệ | **400 Bad Request** | 17ms | Đạt (Chặn status sai) |
| **2.7** | GET | `/admin/tenants?size=200` | Vượt giới hạn kích thước (`>100`) | **400 Bad Request** | 17ms | Đạt (Giới hạn phân trang tối đa 100) |
| **2.8** | GET | `/admin/tenants/{id}` | Lấy chi tiết Tenant theo UUID | **200 OK** | 38ms | Đạt (Đầy đủ contact, branding, stats) |
| **2.9** | GET | `/admin/tenants/invalid-uuid` | Sai định dạng UUID | **400 Bad Request** | 8ms | Đạt (Bắt lỗi UUID) |
| **2.10**| GET | `/admin/tenants/{nonExistent}` | UUID không tồn tại | **404 Not Found** | 11ms | Đạt (Không tìm thấy tài nguyên) |
| **2.11**| PATCH | `/admin/tenants/{id}/status` | Chuyển `ACTIVE → SUSPENDED` | **200 OK** | 28ms | Đạt (Cập nhật thành công kèm version) |
| **2.12**| PATCH | `/admin/tenants/{id}/status` | Chuyển `SUSPENDED → ACTIVE` | **200 OK** | 30ms | Đạt (Kích hoạt lại thành công) |
| **2.13**| PATCH | `/admin/tenants/{id}/status` | Đình chỉ nhưng không nhập lý do | **400 Bad Request** | 10ms | Đạt (Bắt buộc reason khi SUSPENDED) |
| **2.14**| PATCH | `/admin/tenants/{id}/status` | Gửi version cũ (`version=99999`) | **409 Conflict** | 12ms | Đạt (Chặn xung đột cập nhật đồng thời) |
| **2.15**| PATCH | `/admin/tenants/{id}/status` | Chuyển sai quy tắc `ACTIVE → ACTIVE` | **409 Conflict** | 15ms | Đạt (Chặn vi phạm State Machine) |
| **2.16**| GET | `/admin/tenants/{id}/logs` | Lấy lịch sử sự kiện của Tenant | **200 OK** | 24ms | Đạt (Ghi nhận đầy đủ audit log) |
| **2.17**| GET | `/admin/tenants/{nonExistent}/logs`| Logs của Tenant không tồn tại | **404 Not Found** | 10ms | Đạt |

---

### 2.3 Platform Admin - Dashboard Thống kê (6/6 Requests - 100% Passed)
Kiểm thử các câu truy vấn tổng hợp và giới hạn biên số ngày phân tích.

- **3.1 GET** `/admin/dashboard/overview` (mặc định 30 ngày) → `200 OK` (11ms) - Trả về `metrics`, `usage`, múi giờ `Asia/Ho_Chi_Minh`.
- **3.2 GET** `/admin/dashboard/overview?days=7` → `200 OK` (23ms) - Tính toán chuẩn xác cho 7 ngày gần nhất.
- **3.3 GET** `/admin/dashboard/overview?days=0` → `400 Bad Request` (15ms) - Chặn giá trị dưới ngưỡng tối thiểu (`days >= 1`).
- **3.4 GET** `/admin/dashboard/overview?days=367` → `400 Bad Request` (28ms) - Chặn giá trị vượt ngưỡng tối đa (`days <= 366`).
- **3.5 GET** `/admin/dashboard/recent-activities` (limit 10) → `200 OK` (16ms) - Lấy danh sách sự kiện gần nhất có gắn với Tenant.
- **3.6 GET** `/admin/dashboard/recent-activities?limit=5` → `200 OK` (15ms) - Giới hạn số lượng chính xác (`content.length <= 5`).

---

### 2.4 Platform Admin - System Config & Logs (7/7 Requests - 100% Passed)
- **4.1 GET** `/admin/system/config` → `200 OK` (10ms) - Lấy Preview Settings (routeMode, baseDomain, templates).
- **4.2 PUT** `/admin/system/config` → `200 OK` (22ms) - Cập nhật cấu hình thành công, version tự động tăng.
- **4.3 PUT** `/admin/system/config` (version xung đột 99999) → `409 Conflict` (11ms) - Bảo vệ dữ liệu bằng khóa lạc quan.
- **4.4 PUT** `/admin/system/config` (routeMode sai) → `400 Bad Request` (9ms) - Chỉ chấp nhận `SUBDOMAIN_PREVIEW`.
- **4.5 PUT** `/admin/system/config` (baseDomain sai định dạng) → `400 Bad Request` (9ms) - Bắt lỗi regex domain name.
- **5.1 GET** `/admin/system/logs` → `200 OK` (13ms) - Lấy lịch sử thao tác toàn hệ thống.
- **5.2 GET** `/admin/system/logs?page=0&size=5` → `200 OK` (12ms) - Phân trang lịch sử chuẩn xác.

---

### 2.5 Kiểm thử Bảo mật & Phân quyền (Authorization & Security - 7/7 Requests - 100% Passed)
Xác thực cơ chế chặn truy cập trái phép của Spring Security và JWT Authentication Filter.

- **6.1 GET** `/admin/tenants` (Không gửi token) → `401 Unauthorized` (7ms) - Chặn truy cập ẩn danh.
- **6.2 GET** `/admin/tenants` (Gửi token giả mạo `invalid.fake.token`) → `401 Unauthorized` (5ms) - Chặn token sai chữ ký.
- **6.3 GET** `/admin/dashboard/overview` (Không token) → `401 Unauthorized` (5ms).
- **6.4 GET** `/admin/system/config` (Không token) → `401 Unauthorized` (6ms).
- **6.5 PUT** `/admin/system/config` (Không token) → `401 Unauthorized` (6ms).
- **6.6 POST** `/auth/register` (Endpoint đang tắt qua Feature Flag) → `404 Not Found` (6ms) - Không lộ endpoint chưa kích hoạt.
- **6.7 POST** `/auth/forgot-password` (Endpoint đang tắt qua Feature Flag) → `404 Not Found` (6ms) - Xử lý an toàn.

---

## 3. ĐO KIỂM HIỆU NĂNG & THỜI GIAN PHẢN HỒI (LATENCY BENCHMARKS)

Mọi endpoint chính đều được đo kiểm và so sánh với ngưỡng SLA tiêu chuẩn:

| Endpoint | Mô tả | Ngưỡng SLA | Thời gian đo được | Đánh giá |
|----------|-------|:----------:|:-----------------:|:--------:|
| `POST /api/v1/auth/login` | Đăng nhập Admin (Xác thực BCrypt + sinh JWT) | < 500ms | **75ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/tenants?size=50` | Lấy danh sách 50 Tenant (SQL Query + Phân trang) | < 500ms | **9ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/tenants/{id}` | Chi tiết Tenant (Multi-table Join + Thống kê) | < 500ms | **11ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/dashboard/overview?days=30` | Thống kê Dashboard 30 ngày | < 500ms | **16ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/dashboard/overview?days=366` | Thống kê Dashboard toàn bộ 366 ngày (Max Range) | < 1000ms | **40ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/system/config` | Lấy cấu hình hệ thống | < 300ms | **10ms** | 🟢 Cực tốt |
| `GET /api/v1/admin/system/logs?size=100` | Lấy 100 bản ghi nhật ký hệ thống | < 500ms | **13ms** | 🟢 Cực tốt |
| `POST /api/v1/auth/refresh` | Làm mới Token (Xác minh Redis + sinh JWT mới) | < 500ms | **13ms** | 🟢 Cực tốt |

> **Nhận xét hiệu năng:**
> - Tất cả các API backend đều có độ trễ cực thấp (< 50ms cho các truy vấn dữ liệu thông thường).
> - API Đăng nhập (`/auth/login`) mất 75ms - 147ms là hoàn toàn hợp lý và an toàn do chi phí tính toán băm mật khẩu của thuật toán BCrypt.

---

## 4. LOG LỖI ĐÃ PHÁT HIỆN & ĐÃ XỬ LÝ (BUG FIX LOG)

Trong quá trình khởi chạy và kiểm thử, đã phát hiện và xử lý thành công 3 lỗi nghiêm trọng:

1. **Lỗi Docker Image Outdated (13 ngày trước):**
   - *Hiện tượng:* Backend container liên tục bị dừng ngay khi khởi động (`exited code 1`).
   - *Nguyên nhân:* Image trên máy người dùng là bản build cũ 13 ngày trước, thiếu cấu hình Platform Admin và schema `travel_v2`.
   - *Khắc phục:* Đã tiến hành build lại toàn bộ image từ mã nguồn mới nhất (`docker compose build backend`).

2. **Lỗi Fallback Database URL (`localhost:5432`):**
   - *Hiện tượng:* Log báo lỗi `Connection to localhost:5432 refused`.
   - *Nguyên nhân:* Spring Boot 4/3 cần biến chuẩn `SPRING_DATASOURCE_URL` để ghi đè thuộc tính datasource trong Docker container.
   - *Khắc phục:* Bổ sung các biến `SPRING_DATASOURCE_*` vào `docker-compose.yml`.

3. **Lỗi Xung đột Khóa ngoại Scope (`fk_user_roles_role_scoped`):**
   - *Hiện tượng:* `AdminBootstrap` crash khi khởi động với lỗi: `Key (role_id, role_scope)=(..., GLOBAL) is not present in table roles`.
   - *Nguyên nhân:* Bảng `roles` lưu `ROLE_ADMIN` có `scope = 'PLATFORM'`, nhưng bảng `user_roles` lại mặc định `role_scope = 'GLOBAL'` và có ràng buộc `CHECK (role_scope = 'GLOBAL')`.
   - *Khắc phục:* Tạo migration `V17__fix_user_roles_scope.sql` và trigger PostgreSQL `trg_user_roles_scope` tự động đồng bộ `role_scope` theo `travel_v2.roles.scope` của role được gán, cho phép gán quyền `ROLE_ADMIN` mượt mà.

---

## 5. KẾT LUẬN & KHUYẾN NGHỊ

### 5.1 Kết luận
- Toàn bộ các API thuộc phạm vi hoạt động hiện tại (**Authentication** và **Platform Admin**) đều hoạt động chính xác 100% theo đúng hợp đồng nghiệp vụ quy định trong [`docs/PLATFORM_ADMIN_API.md`](file:///d:/JOB/INTERN/AI-Travel-Reseller-Network-Platform/docs/PLATFORM_ADMIN_API.md).
- Bộ test collection gồm **57 test cases** đã được chạy kiểm chứng thực tế và vượt qua **151/151 assertions** (100% Pass Rate).
- Tốc độ phản hồi đạt mức xuất sắc (trung bình 23ms).

### 5.2 Khuyến nghị cho các giai đoạn tiếp theo
1. **Mở rộng Actor Tenant / Reseller:** Khi bật lại các tính năng `actor-functions: true`, bổ sung các bộ test tương ứng cho Onboarding Merchant, Quản lý sản phẩm, Chiến dịch và Theo dõi đơn hàng.
2. **Cấu hình CORS:** Trước khi đưa vào sử dụng thực tế giữa Frontend (`localhost:3000`) và Backend (`localhost:8080`), cần thêm cấu hình `CorsConfigurationSource` trong `SecurityConfig.java` để cho phép các HTTP methods từ nguồn Frontend.
3. **Rate Limiting:** Cân nhắc triển khai Bucket4j hoặc Redis Rate Limiter cho endpoint `/api/v1/auth/login` để phòng chống tấn công dò mật khẩu (Brute Force).
