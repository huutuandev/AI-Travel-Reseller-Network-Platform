# BÁO CÁO PHÂN TÍCH DATABASE: HIỆN TRẠNG VÀ THIẾT KẾ MỤC TIÊU (PHASE 1)

**Dự án:** AI Travel Reseller Network Platform  
**Phiên bản phân tích:** 1.0  
**Tài liệu đối chiếu:** `TaiLieuCSDL.docx` (19 bảng, 4 nhóm nghiệp vụ)  
**Ngày lập:** 05/11/2026  
**Tác giả:** Senior Backend Engineer (Java Spring Boot, PostgreSQL, JPA/Hibernate, Flyway)

---

## 1. TỔNG QUAN HỆ THỐNG VÀ CÔNG NGHỆ HIỆN TẠI

### 1.1. Stack công nghệ
- **Java Platform:** Java 17 (OpenJDK / Oracle JDK 17.0.12, target release 17).
- **Framework:** Spring Boot 4.1.1 (`spring-boot-starter-parent` 4.1.1).
- **ORM / Persistence:** Hibernate ORM Core 7.4.5.Final, Spring Data JPA, `NamedParameterJdbcTemplate`.
- **Database Engine:** PostgreSQL 15.18 (chạy trên container Docker `postgres:15-alpine` qua Docker Compose).
- **Migration Tool:** Flyway (`flyway-core`, `flyway-database-postgresql`).
- **Cache & Message Broker:** Redis 7-alpine, RabbitMQ 3-management-alpine.
- **Security:** Spring Security, JJWT 0.12.5 (Access Token + Redis Refresh Token).

### 1.2. Cấu hình môi trường & Profile
- **Tập tin cấu hình chính:** `BE/src/main/resources/application.yaml`, import `.env` từ thư mục gốc dự án.
- **Datasource URL:** `jdbc:postgresql://localhost:5432/mydatabase` (local dev/test) hoặc `jdbc:postgresql://postgres_db:5432/mydatabase` (Docker container).
- **Hibernate DDL Auto:** `validate` (nguyên tắc: Hibernate không tự ý thay đổi schema; schema do Flyway kiểm soát hoàn toàn).
- **Schema hiện thời:** `travel_v2` (được cấu hình qua `spring.flyway.default-schema: travel_v2` và `spring.jpa.properties.hibernate.default_schema: travel_v2`).
- **Vị trí migration trước đó:** `classpath:db/redesign` (gồm 2 migration: `V1__approved_18_table_schema.sql` và `V2__platform_admin.sql`).
- **Lịch sử migration cũ:** `classpath:db/migration` trước đây chứa `V1__init_schema.sql` áp dụng vào schema `public` (12 bảng legacy).

### 1.3. Khảo sát dữ liệu thực tế trong Database
- Đã kết nối trực tiếp PostgreSQL 15 qua container `postgres_db`:
  - Schema `public`: Chứa 12 bảng legacy (`campaigns`, `commissions`, `contents`, `conversations`, `merchants`, `orders`, `products`, `resellers`, `roles`, `tracking_links`, `user_roles`, `users`). Toàn bộ 12 bảng đều có 0 bản ghi live data.
  - Schema `travel_v2`: Chứa 18 bảng nghiệp vụ + 2 bảng platform admin (`platform_settings`, `platform_events`). Hầu hết các bảng có 0 bản ghi, ngoại trừ:
    - `roles`: 1 bản ghi hạt giống (`ROLE_ADMIN`, scope: `PLATFORM`).
    - `platform_settings`: 1 bản ghi cấu hình mặc định (id = 1).
  - Tồn tại tập tin sao lưu an toàn `mydatabase_before_reset.dump` (PostgreSQL custom dump) lưu trữ trạng thái trước đây.

---

## 2. ĐỐI CHIẾU DATABASE HIỆN TẠI VỚI 19 BẢNG MỤC TIÊU (`TaiLieuCSDL.docx`)

Tài liệu thiết kế mục tiêu quy định chuẩn hóa 19 bảng chia theo 4 nhóm nghiệp vụ:

### Nhóm 1: Tenant, Merchant, Tài khoản và Phân quyền (7 bảng)
1. `merchants`
2. `merchant_branding`
3. `roles`
4. `users`
5. `user_roles`
6. `social_accounts`
7. `reseller_profiles`

### Nhóm 2: Sản phẩm, Voucher và Chiến dịch (4 bảng)
8. `products`
9. `campaigns`
10. `campaign_applications`
11. `vouchers`

### Nhóm 3: Tracking, Booking, Conversion và Thanh toán (6 bảng)
12. `tracking_links`
13. `reseller_campaign_posts` *(Bảng mới hoàn toàn, chưa có trong 18 bảng trước)*
14. `bookings`
15. `commissions`
16. `payouts`
17. `payout_items`

### Nhóm 4: Dữ liệu Tri thức và Nội dung AI (2 bảng)
18. `ai_contents`
19. `knowledge_documents`

---

## 3. PHÂN TÍCH CHI TIẾT TỪNG BẢNG: TỒN TẠI, THIẾU/THỪA CỘT VÀ QUAN HỆ FK

| STT | Tên bảng mục tiêu | Trạng thái hiện tại | Phân tích chi tiết cột & ràng buộc | Đánh giá & Hành động cần thiết |
|---|---|---|---|---|
| 1 | `roles` | Đã có trong `travel_v2` | Đủ `id`, `name`, `scope`, `description`, `created_at`. Có `UNIQUE(name)` và `UNIQUE(id, scope)`. | Tương thích tốt. Cần giữ vai trò `scope` cho phân quyền multi-tenant. |
| 2 | `users` | Đã có trong `travel_v2` | Đủ `id`, `email`, `password_hash`, `full_name`, `phone`, `status`, `email_verified_at`, `created_at`, `updated_at`. Index `uq_users_email_lower` trên `LOWER(email)`. | Tương thích tốt. Trạng thái hỗ trợ `ACTIVE`, `LOCKED`, `PENDING_VERIFICATION`, `DISABLED`. |
| 3 | `user_roles` | Đã có trong `travel_v2` | Đang có `user_id`, `role_id`, `created_at`, PK `(user_id, role_id)`. | Tương thích. Bổ sung `role_scope DEFAULT 'GLOBAL'` và FK tổng hợp `(role_id, role_scope) REFERENCES roles(id, scope)` theo đúng tài liệu. |
| 4 | `merchants` | Đã có trong `travel_v2` | Đủ `id`, `user_id`, `code`, `name`, `description`, `contact_email`, `phone`, `address`, `subdomain`, `custom_domain`, `status`, `created_at`, `updated_at`, `version`. | Tương thích tốt. Giữ `version` để phục vụ optimistic locking trong Platform Admin. |
| 5 | `merchant_branding` | Đã có trong `travel_v2` | Đủ `id`, `merchant_id`, `logo_url`, `favicon_url`, `primary_color`, `secondary_color`, `custom_css`, `footer_text`, `updated_at`. `UNIQUE(merchant_id)`. | Tương thích 100%. |
| 6 | `reseller_profiles` | Đã có trong `travel_v2` | Đang có `id`, `user_id`, `payout_info` (JSONB object), `status`, `created_at`, `updated_at`. | Tương thích 100%. |
| 7 | `social_accounts` | Đã có trong `travel_v2` nhưng **lệch kiểu dữ liệu & thiếu cột** | Hiện tại: `followers_count` và `average_views` là `INTEGER`. **Thiếu:** `metrics_source` (VARCHAR(20)), `metrics_updated_at` (TIMESTAMPTZ), `updated_at` (TIMESTAMPTZ). | **Cần chỉnh sửa:** Đổi `followers_count`, `average_views` sang `BIGINT`; thêm `metrics_source`, `metrics_updated_at`, `updated_at`. |
| 8 | `products` | Đã có trong `travel_v2` | Đủ `id`, `merchant_id`, `title`, `slug`, `description`, `images` (JSONB array), `price`, `sale_price`, `availability_status`, `official_checkout_url`, `created_at`, `updated_at`. | Tương thích tốt. FK trỏ chính xác đến `merchants(id)`. |
| 9 | `campaigns` | Đã có trong `travel_v2` | Đủ `id`, `product_id`, `title`, `description`, `commission_type`, `commission_rate`, `cookie_window_days`, `start_date`, `end_date`, `status`, `created_at`, `updated_at`. | Tương thích tốt. FK trỏ đến `products(id)`. |
| 10 | `campaign_applications` | Đã có trong `travel_v2` | Đủ `id`, `campaign_id`, `reseller_id`, `status`, `commission_type_snapshot`, `commission_rate_snapshot`, `applied_at`, `reviewed_at`. `UNIQUE(campaign_id, reseller_id)`. | Tương thích. Cần bổ sung trigger tự chụp snapshot hoa hồng và chống sửa đổi trái phép. |
| 11 | `vouchers` | Đã có trong `travel_v2` nhưng **lệch kiểu dữ liệu** | Hiện tại: `usage_limit` và `usage_count` là `INTEGER`. | **Cần chỉnh sửa:** Đổi `usage_limit` và `usage_count` sang `BIGINT` theo tài liệu. Giữ `discount_type` hỗ trợ mở rộng. |
| 12 | `tracking_links` | Đã có trong `travel_v2` nhưng **thiếu cột** | Hiện tại: Có `id`, `campaign_application_id`, `tracking_code`, `target_url`, `status`, `click_count`, `created_at`. **Thiếu:** `reseller_id` UUID FK -> `reseller_profiles(id)`. | **Cần chỉnh sửa:** Bổ sung cột `reseller_id UUID NOT NULL REFERENCES reseller_profiles(id)`. |
| 13 | `reseller_campaign_posts` | **CHƯA TỒN TẠI** (Bảng số 13) | Bảng mới hoàn toàn trong tài liệu, chưa từng có trong schema. Lưu bài đăng video mạng xã hội liên kết campaign application. | **Cần tạo mới:** Tạo bảng với đủ 13 trường theo tài liệu thiết kế. |
| 14 | `bookings` | Đã có trong `travel_v2` | Đủ `id`, `merchant_id`, `product_id`, `tracking_link_id`, `customer_id`, `total_amount`, `currency`, `booking_status`, `created_at`, `updated_at`. | Tương thích tốt. Trigger kiểm tra scope sản phẩm và merchant. |
| 15 | `commissions` | Đã có trong `travel_v2` | Đủ `id`, `booking_id`, `reseller_id`, `amount`, `currency`, `status`, `created_at`, `updated_at`. `UNIQUE(booking_id)`. | Tương thích. Mở rộng CHECK status để bao hàm cả `APPROVED`, `PARTIALLY_PAID`, `CANCELLED`, `REVERSED`. |
| 16 | `payouts` | Đã có trong `travel_v2` | Đủ `id`, `merchant_id`, `reseller_id`, `total_amount`, `currency`, `payout_status`, `payment_reference`, `processed_at`, `created_at`. | Tương thích tốt. Cần trigger tự động tính tổng tiền từ `payout_items`. |
| 17 | `payout_items` | Đã có trong `travel_v2` | Đủ `id`, `payout_id`, `commission_id`, `amount`, `created_at`. `UNIQUE(commission_id)`. | Tương thích. Cần trigger tự sao chép amount từ `commissions` và kiểm tra điều kiện PAYABLE/PROCESSING. |
| 18 | `ai_contents` | Đã có trong `travel_v2` | Đủ `id`, `campaign_id`, `title`, `content_type`, `prompt_used`, `generated_content` (JSONB), `media_url`, `thumbnail_url`, `duration_seconds`, `generation_status`, `published_at`, `created_at`, `updated_at`. | Tương thích tốt. |
| 19 | `knowledge_documents` | Đã có trong `travel_v2` | Đủ `id`, `merchant_id`, `product_id`, `title`, `content`, `status`, `created_at`, `updated_at`. | Tương thích tốt. |

### Các bảng bổ trợ không thuộc 19 bảng nhưng bắt buộc phải giữ lại:
- `platform_settings`: Bảng cấu hình hệ thống (preview domain, routing mode, template video/caption), được `AdminService` và `PlatformAdminController` sử dụng.
- `platform_events`: Bảng lưu nhật ký kiểm toán hành động quản trị hệ thống, bắt buộc cho `PlatformAdminControllerTest`.

---

## 4. PHÂN TÍCH VÀ ĐỀ XUẤT GIẢI QUYẾT CÁC ĐIỂM MÂU THUẪN TRONG TÀI LIỆU

### 4.1. Mâu thuẫn giữa `tenant_id` và `merchant_id`
- **Hiện tượng:** Trong lời văn thuyết minh của `TaiLieuCSDL.docx`, tác giả nhiều lần dùng khái niệm "tenant", "tenant_id", "mô hình multi-tenancy", "tenant_branding". Nhưng trong bảng đặc tả cột, bảng tên là `merchants` (khóa chính `id`), và các bảng con đều đặt tên cột là `merchant_id` (`products.merchant_id`, `bookings.merchant_id`, `payouts.merchant_id`, `knowledge_documents.merchant_id`, `merchant_branding.merchant_id`).
- **Phân tích:** Trong kiến trúc hệ thống này, mỗi `merchant` chính là một `tenant` độc lập.
- **Giải pháp đề xuất:** Sử dụng thống nhất tên cột `merchant_id` trỏ về `merchants(id)`. Điều này vừa khớp 100% với định nghĩa cột trong tài liệu, vừa đồng nhất với toàn bộ mã nguồn Java, Repository và các truy vấn hiện hữu.

### 4.2. `roles.scope` và `user_roles.role_scope`
- **Hiện tượng:** Trong bảng đặc tả `roles`, phần văn bản đề cập ràng buộc `UNIQUE (id, scope)` để bảng gán vai trò có thể dùng khóa ngoại tổng hợp kiểm tra vai trò `GLOBAL` hay `TENANT`. Ở bảng `user_roles`, mô tả ghi rõ: *"Khóa ngoại tổng hợp (role_id, role_scope) tham chiếu (roles.id, roles.scope) đảm bảo vai trò thực sự có phạm vi GLOBAL"*. Tuy nhiên bảng cột của `user_roles` chỉ liệt kê `user_id`, `role_id`, `created_at`.
- **Phân tích:** Nếu `user_roles` chỉ có 2 cột khóa chính, không thể tạo khóa ngoại tổng hợp đến `(roles.id, roles.scope)`. Hơn nữa, JPA mapping thông thường `@ManyToMany` trên `User` sẽ insert `(user_id, role_id)`.
- **Giải pháp đề xuất:**
  - Bổ sung cột `role_scope VARCHAR(50) NOT NULL DEFAULT 'GLOBAL' CHECK (role_scope = 'GLOBAL')` vào `user_roles`.
  - Tạo ràng buộc `CONSTRAINT fk_user_roles_role_scoped FOREIGN KEY (role_id, role_scope) REFERENCES roles(id, scope) ON DELETE RESTRICT`.
  - Với giá trị mặc định `'GLOBAL'`, khi JPA hoặc mã Java chỉ truyền `(user_id, role_id)`, PostgreSQL vẫn tự động áp dụng `'GLOBAL'` và kiểm tra toàn vẹn thành công mà không làm ảnh hưởng đến tầng ứng dụng Java.

### 4.3. `products.merchant_id` tham chiếu `users` hay `merchants`
- **Hiện tượng:** Dòng mô tả trường `products.merchant_id` trong tài liệu ghi nhầm: *"FK tùy chọn đến users.id, xác định Merchant quản lý sản phẩm"*. Nhưng ngay dòng dưới phần giải thích lại viết: *"id định danh sản phẩm, còn tenant_id xác định chủ sở hữu... campaign tham chiếu sản phẩm bằng cặp (tenant_id, product_id)"*. Đồng thời các bảng `bookings`, `knowledge_documents`, `vouchers` đều coi `product` thuộc về `merchants`.
- **Phân tích:** Nếu `products.merchant_id` trỏ tới `users(id)`, chuỗi quan hệ nghiệp vụ đa người thuê (multi-tenant) sẽ bị phá vỡ: một user có thể không phải là merchant, hoặc booking kiểm tra `booking.merchant_id == product.merchant_id` sẽ thất bại vì so sánh ID merchant với ID user.
- **Giải pháp đề xuất:** Khẳng định chuẩn xác: `products.merchant_id` là `UUID NOT NULL REFERENCES merchants(id) ON DELETE RESTRICT`. Ghi chú rõ điểm sai sót do copy-paste trong bản thảo tài liệu Word.

### 4.4. Quan hệ giữa `campaigns` và `products`
- **Hiện tượng:** Văn bản mô tả nhắc đến khóa ngoại tổng hợp `(tenant_id, product_id)` giữa `campaigns` và `products`. Tuy nhiên bảng cột của `campaigns` chỉ có `product_id UUID NOT NULL` mà không có cột `tenant_id` hay `merchant_id`.
- **Phân tích:** Mỗi sản phẩm (`product`) đã thuộc về một `merchant` duy nhất. Nếu thêm `merchant_id` vào `campaigns`, dữ liệu sẽ bị trùng lặp và phát sinh rủi ro bất nhất quán (ví dụ chiến dịch chọn merchant A nhưng sản phẩm lại của merchant B).
- **Giải pháp đề xuất:** Giữ nguyên `campaigns.product_id UUID NOT NULL REFERENCES products(id) ON DELETE RESTRICT`. Mối quan hệ giữa chiến dịch và merchant được xác định tường minh qua sản phẩm (`campaigns -> products -> merchants`). Các trigger bảo đảm an toàn phạm vi khi đặt booking hoặc tạo voucher dựa trên đường dẫn liên kết này.

### 4.5. `commissions` liên kết `bookings` hay `conversions`
- **Hiện tượng:** Lời dẫn nhắc đến *"hoa hồng phát sinh từ conversion"*, nhưng trong schema không hề có bảng `conversions`. Cột thực tế là `booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id)`.
- **Phân tích:** "Conversion" là thuật ngữ tiếp thị chỉ hành vi chuyển đổi đơn hàng thành công. Trong mô hình dữ liệu, đơn hàng du lịch chính là thực thể `bookings`.
- **Giải pháp đề xuất:** Sử dụng đúng thực thể chuẩn hóa `bookings(id)` thông qua cột `commissions.booking_id`. Ràng buộc `UNIQUE(booking_id)` bảo đảm mỗi đơn đặt dịch vụ chỉ ghi nhận tối đa một khoản hoa hồng.

### 4.6. Danh sách trạng thái thực tế của `commissions`
- **Hiện tượng:** Cột mô tả liệt kê `PENDING, PAYABLE, PAID, PARTIALLY_PAID`. Văn bản bên dưới lại mô tả: `PENDING, APPROVED, PAYABLE, PAID, CANCELLED, REVERSED`.
- **Giải pháp đề xuất:** Hợp nhất đầy đủ toàn bộ các trạng thái nghiệp vụ hợp lệ vào CHECK constraint:
  `CHECK (status IN ('PENDING', 'APPROVED', 'PAYABLE', 'PAID', 'PARTIALLY_PAID', 'CANCELLED', 'REVERSED'))`.

---

## 5. KẾ HOẠCH MIGRATION VÀ QUẢN LÝ RỦI RO

### 5.1. Cấu trúc Flyway Migration mục tiêu (Đặt tại `src/main/resources/db/migration/`)
Nhằm bảo đảm tính tường minh, dễ truy vết và chạy lại ổn định trên mọi môi trường từ cơ sở dữ liệu trắng, các migration được chia theo thứ tự phụ thuộc khóa ngoại như sau:

- `V1__create_users_and_roles.sql`: Tạo bảng `roles`, `users`, `user_roles` cùng các ràng buộc scope và unique index email.
- `V2__create_merchants_and_branding.sql`: Tạo bảng `merchants`, `merchant_branding` và index subdomain/custom_domain.
- `V3__create_reseller_profiles.sql`: Tạo bảng `reseller_profiles` và ràng buộc JSONB object.
- `V4__create_social_accounts.sql`: Tạo bảng `social_accounts` với kiểu `BIGINT` cho `followers_count` và `average_views`.
- `V5__create_products.sql`: Tạo bảng `products` với giá NUMERIC(12,2) và JSONB array cho `images`.
- `V6__create_campaigns.sql`: Tạo bảng `campaigns` với tỷ lệ hoa hồng NUMERIC(10,2) và ràng buộc ngày.
- `V7__create_campaign_applications.sql`: Tạo bảng `campaign_applications` kèm các cột snapshot hoa hồng.
- `V8__create_vouchers.sql`: Tạo bảng `vouchers` với `usage_limit` / `usage_count` kiểu `BIGINT`.
- `V9__create_tracking_links.sql`: Tạo bảng `tracking_links` liên kết với `campaign_applications` và `reseller_profiles`.
- `V10__create_reseller_campaign_posts.sql`: Tạo bảng mới `reseller_campaign_posts` (Bảng số 13).
- `V11__create_bookings_and_commissions.sql`: Tạo bảng `bookings` và `commissions`.
- `V12__create_payouts_and_payout_items.sql`: Tạo bảng `payouts` và `payout_items`.
- `V13__create_ai_contents.sql`: Tạo bảng `ai_contents` hỗ trợ nội dung đa phương tiện và JSONB.
- `V14__create_knowledge_documents.sql`: Tạo bảng `knowledge_documents` lưu trữ tri thức RAG cho AI.
- `V15__add_indexes_constraints_and_triggers.sql`: Tạo toàn bộ triggers (`touch_updated_at`, snapshot hoa hồng, kiểm tra scope cross-table, validate payout items và tự động cộng tổng `payouts.total_amount`).
- `V16__create_platform_admin_support.sql`: Giữ nguyên hai bảng bổ trợ `platform_settings`, `platform_events` và seed quyền `ROLE_ADMIN` để đảm bảo tương thích 100% với `AdminService` và bộ test hiện tại.

### 5.2. Nhận diện rủi ro & Kế hoạch xử lý
1. **Rủi ro mất mát dữ liệu:**
   - *Biện pháp:* Tạo bản sao lưu dump hoàn chỉnh của PostgreSQL bằng `pg_dump` trước khi thực hiện bất kỳ thao tác nào.
2. **Rủi ro xung đột checksum hoặc Flyway schema history cũ:**
   - *Biện pháp:* Chuyển vị trí migration sang `classpath:db/migration`. Thiết lập baseline hoặc xử lý bảng lịch sử migration một cách nhất quán trên schema chỉ định.
3. **Rủi ro lỗi Hibernate Schema Validation khi khởi động (`ddl-auto: validate`):**
   - *Biện pháp:* Toàn bộ các Entity Java Spring Boot phải được đồng bộ chính xác từng tên cột, kiểu dữ liệu (`UUID`, `Instant`, `BigDecimal`, `Long`, `Integer`, `String`), độ dài `length`, `precision`, `scale`, và mapping JSONB bằng `@JdbcTypeCode(SqlTypes.JSON)`.
4. **Rủi ro đứt gãy luồng đăng nhập / admin hiện tại:**
   - *Biện pháp:* Bảo toàn cấu trúc trường `password_hash`, `email_verified_at`, `status` của `User`, bảo toàn bảng `platform_settings` và các trigger toàn vẹn. Chạy kiểm thử tự động toàn diện `mvn clean test` để kiểm chứng.

---
*Kết thúc Báo cáo Phase 1. Tiếp tục triển khai Phase 2 & Phase 3.*
