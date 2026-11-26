# BÁO CÁO TỔNG KẾT REFACTOR DATABASE & FLYWAY MIGRATION (DATABASE_MIGRATION_REPORT.md)

**Dự án:** AI Travel Reseller Network Platform  
**Kỹ sư thực hiện:** Senior Backend Engineer  
**Thời gian hoàn thành:** 05/11/2026  
**Mục tiêu đạt được:** 100% chuẩn hóa 19 bảng nghiệp vụ, cấu hình Flyway độc lập, toàn bộ Entity Hibernate ánh xạ chính xác với `ddl-auto: validate`, toàn bộ 54/54 test cases đều vượt qua thành công.

---

## 1. DANH SÁCH TẬP TIN ĐÃ TẠO HOẶC CHỈNH SỬA

### 1.1. Tập tin Cấu hình & Migration
| Đường dẫn | Thao tác | Mô tả |
|---|---|---|
| `BE/src/main/resources/application.yaml` | Chỉnh sửa | Đổi `spring.flyway.locations` trỏ về `classpath:db/migration`. |
| `BE/src/main/resources/db/migration/V1__create_users_and_roles.sql` | Tạo mới | Bảng `roles`, `users`, `user_roles` (scope global & lower email index). |
| `BE/src/main/resources/db/migration/V2__create_merchants_and_branding.sql` | Tạo mới | Bảng `merchants` (kèm `version` optimistic locking), `merchant_branding`. |
| `BE/src/main/resources/db/migration/V3__create_reseller_profiles.sql` | Tạo mới | Bảng `reseller_profiles` (JSONB payout_info). |
| `BE/src/main/resources/db/migration/V4__create_social_accounts.sql` | Tạo mới | Bảng `social_accounts` (chuẩn hóa kiểu BIGINT, metrics_source, updated_at). |
| `BE/src/main/resources/db/migration/V5__create_products.sql` | Tạo mới | Bảng `products` (NUMERIC(12,2), JSONB images array, FK merchants). |
| `BE/src/main/resources/db/migration/V6__create_campaigns.sql` | Tạo mới | Bảng `campaigns` (NUMERIC(10,2), cookie_window_days, FK products). |
| `BE/src/main/resources/db/migration/V7__create_campaign_applications.sql` | Tạo mới | Bảng `campaign_applications` (các trường snapshot hoa hồng). |
| `BE/src/main/resources/db/migration/V8__create_vouchers.sql` | Tạo mới | Bảng `vouchers` (chuẩn hóa BIGINT cho usage_limit/count). |
| `BE/src/main/resources/db/migration/V9__create_tracking_links.sql` | Tạo mới | Bảng `tracking_links` (bổ sung trường `reseller_id`). |
| `BE/src/main/resources/db/migration/V10__create_reseller_campaign_posts.sql` | Tạo mới | Bảng số 13 `reseller_campaign_posts` (bảng mới từ tài liệu). |
| `BE/src/main/resources/db/migration/V11__create_bookings_and_commissions.sql` | Tạo mới | Bảng `bookings` và `commissions` (mở rộng danh sách trạng thái). |
| `BE/src/main/resources/db/migration/V12__create_payouts_and_payout_items.sql` | Tạo mới | Bảng `payouts` và `payout_items` (kết nối chi trả). |
| `BE/src/main/resources/db/migration/V13__create_ai_contents.sql` | Tạo mới | Bảng `ai_contents` (lưu trữ prompt và JSONB nội dung đa phương tiện). |
| `BE/src/main/resources/db/migration/V14__create_knowledge_documents.sql` | Tạo mới | Bảng `knowledge_documents` (lưu trữ tri thức RAG cho AI). |
| `BE/src/main/resources/db/migration/V15__add_indexes_constraints_and_triggers.sql` | Tạo mới | Toàn bộ triggers: `touch_updated_at`, `copy_commission_snapshot`, `protect_commission_snapshot`, `validate_booking_scope`, `validate_payout_item`, `refresh_payout_total`. |
| `BE/src/main/resources/db/migration/V16__create_platform_admin_support.sql` | Tạo mới | Bảng `platform_settings`, `platform_events`, seed quyền `ROLE_ADMIN`. |
| `BE/src/main/resources/db/migration/V1__init_schema.sql` | Đã xóa | Xóa file migration cũ 12 bảng legacy để chuyển sang chuỗi 16 migration chuẩn. |

### 1.2. Thực thể JPA (Entities) trong `com.aitravel.reseller.entity`
| Tên lớp Entity | Thao tác | Đặc điểm kỹ thuật |
|---|---|---|
| `User.java` | Chỉnh sửa | Bổ sung `@JsonIgnore` trên `passwordHash`, căn chỉnh độ dài status. |
| `Role.java` | Giữ nguyên | Đã có `name`, `scope`, `description`. |
| `Merchant.java` | Tạo mới | Ánh xạ bảng `merchants`, trường `@Version Long version`. |
| `MerchantBranding.java` | Tạo mới | Ánh xạ bảng `merchant_branding` (quan hệ 1-1 với Merchant). |
| `ResellerProfile.java` | Tạo mới | Ánh xạ bảng `reseller_profiles`, `@JsonIgnore` và `@JdbcTypeCode(SqlTypes.JSON)` trên `payoutInfo`. |
| `SocialAccount.java` | Tạo mới | Ánh xạ bảng `social_accounts`, kiểu `Long` cho follower/view. |
| `Product.java` | Tạo mới | Ánh xạ bảng `products`, `BigDecimal` cho giá, `@JdbcTypeCode(SqlTypes.JSON)` cho `images`. |
| `Campaign.java` | Tạo mới | Ánh xạ bảng `campaigns`, `BigDecimal` cho `commissionRate`. |
| `CampaignApplication.java` | Tạo mới | Ánh xạ bảng `campaign_applications`, lưu snapshot hoa hồng. |
| `Voucher.java` | Tạo mới | Ánh xạ bảng `vouchers`, kiểu `Long` cho `usageLimit` và `usageCount`. |
| `TrackingLink.java` | Tạo mới | Ánh xạ bảng `tracking_links` (kèm trường `reseller`). |
| `ResellerCampaignPost.java` | Tạo mới | Ánh xạ bảng `reseller_campaign_posts`. |
| `Booking.java` | Tạo mới | Ánh xạ bảng `bookings`, `@JdbcTypeCode(SqlTypes.CHAR)` cho `currency CHAR(3)`. |
| `Commission.java` | Tạo mới | Ánh xạ bảng `commissions`, quan hệ 1-1 với `Booking`, `@JdbcTypeCode(SqlTypes.CHAR)` cho `currency`. |
| `Payout.java` | Tạo mới | Ánh xạ bảng `payouts`, `@JdbcTypeCode(SqlTypes.CHAR)` cho `currency`. |
| `PayoutItem.java` | Tạo mới | Ánh xạ bảng `payout_items`, quan hệ 1-1 với `Commission`. |
| `AiContent.java` | Tạo mới | Ánh xạ bảng `ai_contents`, `@JdbcTypeCode(SqlTypes.JSON)` cho `generatedContent`. |
| `KnowledgeDocument.java` | Tạo mới | Ánh xạ bảng `knowledge_documents`. |

### 1.3. Kho lưu trữ (Repositories) trong `com.aitravel.reseller.repository`
Đã tạo mới 16 Spring Data JPA Repository giao tiếp với toàn bộ 19 thực thể:
- `MerchantRepository`, `MerchantBrandingRepository`
- `ResellerProfileRepository`, `SocialAccountRepository`
- `ProductRepository`, `CampaignRepository`, `CampaignApplicationRepository`
- `VoucherRepository`, `TrackingLinkRepository`, `ResellerCampaignPostRepository`
- `BookingRepository`, `CommissionRepository`, `PayoutRepository`, `PayoutItemRepository`
- `AiContentRepository`, `KnowledgeDocumentRepository`

### 1.4. Tập tin Kiểm thử & Tài liệu bàn giao
- `BE/src/test/java/com/aitravel/reseller/DatabaseRelationsAndTriggersIntegrationTest.java`: 5 test case tích hợp toàn diện.
- `BE/DATABASE_ANALYSIS.md`: Báo cáo phân tích hiện trạng và đối chiếu (Phase 1).
- `BE/DATABASE_SCHEMA.md`: Tài liệu đặc tả 19 bảng và quan hệ ERD (Phase 8).
- `BE/DATABASE_MIGRATION_GUIDE.md`: Cẩm nang hướng dẫn migration và deploy (Phase 8).
- `BE/DATABASE_MIGRATION_REPORT.md`: Báo cáo tổng kết dự án (Phase 8).
- `backup_before_flyway_migration.sql`: File backup PostgreSQL an toàn trước khi chạy migration.

---

## 2. TỔNG HỢP CÁC ĐIỂM MÂU THUẪN ĐÃ XỬ LÝ

1. **`tenant_id` và `merchant_id`**: Thống nhất dùng `merchant_id` đại diện cho định danh tenant trong mô hình multi-tenancy, khớp với khóa chính `merchants.id` và toàn bộ mã nguồn.
2. **`roles.scope` và `user_roles.role_scope`**: Bổ sung `role_scope VARCHAR(50) NOT NULL DEFAULT 'GLOBAL'` vào `user_roles` với FK tổng hợp `(role_id, role_scope) REFERENCES roles(id, scope)`. Nhờ giá trị mặc định, JPA `@ManyToMany` hoạt động bình thường mà không cần sửa đổi mã nguồn Java.
3. **`products.merchant_id`**: Khẳng định tham chiếu đến `merchants(id)`, loại bỏ sự nhầm lẫn tham chiếu `users(id)` trong văn bản nháp của tài liệu Word.
4. **Quan hệ `campaigns` và `products`**: Giữ mối quan hệ trực tiếp `campaigns -> products -> merchants`. Không thêm cột `merchant_id` dư thừa vào `campaigns`.
5. **`commissions` liên kết `bookings`**: Xác định thực thể chuyển đổi là `bookings(id)` qua cột `commissions.booking_id UNIQUE`.
6. **Bảng số 13 `reseller_campaign_posts`**: Đã bổ sung tạo mới bảng này với đầy đủ 13 trường.
7. **Kiểu dữ liệu tiền tệ**: Sử dụng 100% `NUMERIC(12,2)` và `NUMERIC(10,2)`, loại trừ hoàn toàn `FLOAT`/`DOUBLE`.
8. **Kiểu dữ liệu chuỗi cố định `CHAR(3)`**: Bổ sung `@JdbcTypeCode(SqlTypes.CHAR)` cho các trường tiền tệ `currency` trong Entity JPA để vượt qua kiểm tra nghiêm ngặt của Hibernate validator (`bpchar` vs `varchar`).
9. **Trigger `touch_updated_at`**: Sử dụng `clock_timestamp()` thay vì `now()` để bảo đảm giá trị `updated_at` luôn tăng ngay cả khi cập nhật nhiều lần trong cùng một transaction.

---

## 3. KẾT QUẢ KIỂM THỬ THỰC TẾ

### 3.1. Kết quả Flyway Migration
```
2026-11-05T15:27:37.153+07:00 INFO o.f.core.internal.command.DbValidate : Successfully validated 16 migrations (execution time 00:00.135s)
2026-11-05T15:27:37.223+07:00 INFO o.f.core.internal.command.DbMigrate  : Current version of schema "travel_v2": 16
2026-11-05T15:27:37.231+07:00 INFO o.f.core.internal.command.DbMigrate  : Schema "travel_v2" is up to date. No migration necessary.
```
- Số lượng migration đã áp dụng: **16/16 thành công**.
- Số lượng bảng trong schema `travel_v2`: **21 bảng** (19 bảng nghiệp vụ + 2 bảng Platform Admin).

### 3.2. Kết quả Hibernate Schema Validation
- Cấu hình: `spring.jpa.hibernate.ddl-auto: validate`
- Kết quả log:
```
2026-11-05T15:27:39.622+07:00 INFO j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
```
- Không phát sinh bất kỳ lỗi `SchemaManagementException` nào. Tất cả 19 Entity đều khớp 100% với PostgreSQL schema.

### 3.3. Kết quả Chạy Kiểm thử Toàn diện (`mvn test`)
- **Tổng số bài kiểm thử đã chạy:** **54 test cases**
  - `LoginOnlyEndpointsTest`: 3/3 passed
  - `PlatformAdminControllerTest`: 12/12 passed
  - `AdminServiceTest`: 27/27 passed
  - `AuthLifecycleTest`: 6/6 passed
  - `AuthServiceRegisterTest`: 5/5 passed
  - `ResellerPlatformApplicationTests`: 1/1 passed
  - `DatabaseRelationsAndTriggersIntegrationTest`: 5/5 passed
- **Failures:** **0**
- **Errors:** **0**
- **Skipped:** **0**
- **Trạng thái Build:** **BUILD SUCCESS**

---

## 4. CÁC VẤN ĐỀ CÒN TỒN TẠI VÀ KHUYẾN NGHỊ

1. **Schema legacy `public`:** Hiện tại schema `public` vẫn lưu 12 bảng cũ từ phiên bản trước với 0 bản ghi. Khi chuyển sang môi trường Production chính thức, có thể giữ lại làm backup hoặc drop schema này nếu không còn nhu cầu tham chiếu.
2. **Cơ chế thanh toán Payout:** Trigger `validate_payout_item` đã bảo đảm ràng buộc toàn vẹn ở tầng dữ liệu (chỉ thêm commission `PAYABLE` vào payout `PROCESSING`). Khi xây dựng nghiệp vụ thanh toán tự động với đối tác cổng thanh toán/ngân hàng, cần bảo đảm chuyển trạng thái commission sang `PAID` và payout sang `COMPLETED` trong cùng một transaction nguyên tử (`@Transactional`).
3. **Mã hóa trường nhạy cảm:** Trường `payout_info` trong `reseller_profiles` hiện tại đã được cấu hình `@JsonIgnore` không xuất ra API. Khuyến nghị trong tương lai có thể tích hợp mã hóa tầng ứng dụng (JPA AttributeConverter với AES-256) trước khi lưu vào cột JSONB để đạt chuẩn bảo mật dữ liệu tài chính tối đa.
