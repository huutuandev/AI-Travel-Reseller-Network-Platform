# Reseller API — Phase 1

Hoàn thành ngày 05/11/2026 (Asia/Bangkok). Phạm vi: ba API đọc Marketplace; chưa triển khai Phase 2.

## Kết quả

| Endpoint | HTTP Method | Trạng thái | Test |
|---|---|---|---|
| `/api/v1/reseller/campaign-filters` | GET | Hoàn thành | PASS: distinct, dữ liệu hợp lệ, loại campaign/product/merchant không khả dụng, authorization |
| `/api/v1/reseller/campaigns` | GET | Hoàn thành | PASS: search, category, destination, commission, participation, pagination, sorting, ownership, validation |
| `/api/v1/reseller/campaigns/{campaignId}` | GET | Hoàn thành | PASS: DTO đầy đủ, UUID, 404, quyền xem lịch sử, terms, ảnh, commission, actions |

- `mvnw verify`: **BUILD SUCCESS**, đóng gói Spring Boot JAR thành công.
- Toàn bộ backend: **126 PASS / 0 FAIL / 0 ERROR / 0 SKIP**.
- Test mới: **72 PASS**, gồm 66 trường hợp integration và 6 trường hợp service.
- Test cũ: **54 PASS**; không sửa các test này.
- Integration dùng PostgreSQL 15 trong container test riêng, cổng `127.0.0.1:55432`, database `reseller_phase1_test`.
- Integration đi qua MockMvc, Spring Security, controller, service, JPA profile repository và JDBC campaign repository đến PostgreSQL thật. Có test Bearer token ký bằng `JwtUtil` hiện có và test tài liệu OpenAPI.
- Dữ liệu fixture được rollback sau mỗi test. Container test được dừng và tự xóa sau khi kiểm thử.
- Log lần verify thành công: `BE/target/phase1-verification.log`; báo cáo từng lớp: `BE/target/surefire-reports/`.
- Artifact: `BE/target/reseller-platform-0.0.1-SNAPSHOT.jar`.

## Xác nhận schema và phạm vi thay đổi

Đã đọc V5, V6, V7, V17, các Entity liên quan và truy vấn SELECT metadata của PostgreSQL đang chạy. Có đủ:

| Bảng | Cột đã xác nhận | Entity hiện có |
|---|---|---|
| `products` | `category_code`, `destination_code`, `currency` | `Product.java` đã đồng bộ |
| `campaigns` | `commission_currency`, `terms_text`, `terms_version` | `Campaign.java` đã đồng bộ |

Không sửa Entity, migration, schema/database chính, Docker Compose, Frontend, Authentication, Account, Tenant hoặc Platform Admin. Không tạo migration mới. Các migration hiện có chỉ được áp dụng tự động vào database test riêng khi dựng môi trường kiểm thử.

**File có sẵn được chỉnh sửa trong công việc này: không có.** Các thay đổi có sẵn của người dùng trong working tree được giữ nguyên; chúng không thuộc phần triển khai này.

## File mới

Đường dẫn Java bên dưới tính từ `BE/src/main/java/com/aitravel/reseller/`:

| File | Vai trò |
|---|---|
| `controller/reseller/ResellerCampaignController.java` | Ba endpoint, role RESELLER, UUID canonical, OpenAPI |
| `service/reseller/ResellerCampaignService.java` | Xác định reseller hiện tại, DTO, ảnh và quyền hành động |
| `repository/reseller/ResellerCampaignRepository.java` | SQL parameterized, điều kiện Marketplace chung, pagination và ownership |
| `dto/request/reseller/CampaignQuery.java` | Chuẩn hóa và validate query; allowlist sort |
| `dto/respone/reseller/CampaignFilterResponse.java` | Filter DTO |
| `dto/respone/reseller/CampaignCardResponse.java` | Card DTO |
| `dto/respone/reseller/CampaignDetailResponse.java` | Detail DTO |
| `dto/respone/reseller/CampaignPageResponse.java` | Pagination envelope |
| `dto/respone/reseller/CampaignFields.java` | Các DTO dùng chung: product, merchant, commission, terms, participation, actions |

Test mới:

- `BE/src/test/java/com/aitravel/reseller/reseller/ResellerCampaignIntegrationTest.java`
- `BE/src/test/java/com/aitravel/reseller/reseller/ResellerCampaignServiceTest.java`

Tài liệu mới:

- `docs/RESELLER_PHASE1_IMPLEMENTATION.md`
- `docs/reseller-phase1.postman_collection.json`
- `docs/examples/reseller-phase1/filters.json`
- `docs/examples/reseller-phase1/list.json`
- `docs/examples/reseller-phase1/detail.json`

## Bảng sử dụng và quy tắc nghiệp vụ

Các bảng đọc: `travel_v2.campaigns`, `products`, `merchants`, `users`, `reseller_profiles`, `campaign_applications`. Luồng JWT hiện có đọc thêm `roles` và `user_roles`. Không có thao tác ghi từ ba endpoint.

- Marketplace: campaign `ACTIVE`, `end_date` null hoặc lớn hơn thời điểm hiện tại; product `AVAILABLE`; merchant và tài khoản chủ merchant `ACTIVE`; commission có currency hợp lệ theo loại.
- Theo `RESELLER_API_DESIGN.md`, campaign `ACTIVE` có ngày bắt đầu tương lai vẫn hiển thị để đăng ký sớm. Chưa cho promotion trước ngày bắt đầu. Thời điểm kết thúc là mốc loại trừ: `end_date == now` đã hết hạn.
- Các bản ghi FIXED_AMOUNT cũ thiếu currency được giữ nguyên trong database nhưng bị loại khỏi Marketplace; không đoán loại tiền.
- Category/destination lấy DISTINCT từ cùng tập dữ liệu Marketplace, bỏ null/blank. Không dùng mock FE.
- `q` tìm chuỗi con, không phân biệt hoa thường, trên title campaign hoặc product; ký tự `%`, `_` được coi là ký tự thường. Mọi giá trị lọc đều được bind parameter.
- `page` bắt đầu từ 0; `size` từ 1 đến 100, mặc định 20. Trang vượt cuối trả `200` với `content: []` và tổng bản ghi đúng.
- Sort cho phép `createdAt`, `title`, `endAt`, mỗi trường nhận `asc` hoặc `desc`; mặc định `createdAt,desc`. Luôn thêm UUID cùng chiều để thứ tự ổn định; ngày null xếp cuối.
- `minCommissionPercent` trong khoảng 0–100, chỉ lọc offer PERCENTAGE; kết hợp FIXED_AMOUNT trả 400. `12.00` nghĩa là 12%, không phải 0.12.
- `commission` là offer hiện tại. `participation.commissionSnapshot` giữ type/rate lúc đăng ký, không bị thay bằng offer mới.
- Principal phải là `CustomUserDetails` hợp lệ, có `ROLE_RESELLER`, tài khoản và profile ACTIVE. Profile được tra qua user ID của principal; client không chọn reseller ID.
- Participation LEFT JOIN theo đúng profile đang gọi. Người khác có application không làm campaign trở thành JOINED đối với người gọi.
- Detail tìm đúng UUID. Campaign không tồn tại hoặc không public và người gọi chưa có application đều trả 404, tránh tiết lộ bản ghi riêng tư.
- Người đã có application được xem lịch sử campaign không còn public; các actions bị khóa. `approvedAt` chỉ có khi application APPROVED.
- `canApply` chỉ true khi campaign public và NOT_JOINED. PENDING/REJECTED/WITHDRAWN/SUSPENDED không được tự apply lại. Các quyền promotion chỉ true khi APPROVED, campaign public và đã bắt đầu.
- List/count, filter options sử dụng transaction read-only REPEATABLE_READ và cùng mốc thời gian để giữ nhất quán trong một request.

## Hạn chế phản ánh đúng schema hiện tại

1. Schema chỉ có category/destination code, chưa có nhãn hoặc country. `label` trả nguyên code, `countryCode: null`; không suy diễn tên/đất nước.
2. `campaign_applications` không có snapshot currency. Với snapshot FIXED_AMOUNT, `commissionSnapshot.currency` trả null để không nhầm với currency của offer hiện tại; `commission.currency` vẫn lấy đúng từ campaign. Lịch sử currency cần được xử lý ở phase/schema được phê duyệt sau.
3. Category/destination/terms nullable được trả đúng dữ liệu hiện có. API không tự backfill dữ liệu.
4. Ảnh hỗ trợ mảng URL cũ và object `{url,alt,caption,sortOrder}`. Phần tử không có URL hợp lệ bị bỏ qua; không sinh ảnh mẫu.
5. Actions biểu diễn điều kiện nghiệp vụ. API apply, tracking, assets và posts chưa nằm trong Phase 1.
6. Chưa đo tải lớn hoặc triển khai/restart backend đang phục vụ database chính. Kiểm thử thực tế trong báo cáo là môi trường test tách biệt.

## Request/response đã chạy thực tế

Các JSON bên dưới được ghi trực tiếp từ response của integration test dùng Bearer token thật, dữ liệu PostgreSQL test và toàn bộ backend. UUID là ID fixture đã rollback, không dùng lại trên database chính. File không chứa access token.

```http
GET /api/v1/reseller/campaign-filters
Authorization: Bearer <accessToken>
```

Response đầy đủ: [filters.json](examples/reseller-phase1/filters.json).

```http
GET /api/v1/reseller/campaigns?category=TOURS&minCommissionPercent=10
Authorization: Bearer <accessToken>
```

Response đầy đủ: [list.json](examples/reseller-phase1/list.json). Kết quả có `totalElements: 1`, campaign `Alpha escape`, commission `12.00`, participation `APPROVED`, giá sản phẩm bằng VND.

```http
GET /api/v1/reseller/campaigns/e62f5340-0b42-463d-85a6-0c44b5118ba9
Authorization: Bearer <accessToken>
```

Response đầy đủ: [detail.json](examples/reseller-phase1/detail.json), có gallery, terms version 2 và cookie window 30 ngày lấy từ fixture database.

Ví dụ lỗi thực tế được test: UUID không hợp lệ → 400; UUID không tồn tại/không được xem → 404; thiếu token → 401; role ADMIN/MERCHANT hoặc profile không hợp lệ → 403; FIXED_AMOUNT kèm minCommissionPercent → 400.

## Postman / Swagger

1. Import `docs/reseller-phase1.postman_collection.json` vào Postman.
2. Đặt collection variable `baseUrl` theo backend đang chạy, `accessToken` là token của tài khoản reseller ACTIVE đã có. Collection không tạo tài khoản hoặc dữ liệu.
3. Chạy Filters, rồi List. List tự lưu UUID đầu tiên vào `campaignId`; nếu list rỗng, cần một campaign hiện có đủ điều kiện hiển thị.
4. Chạy Detail và hai request negative test. Có thể thêm các query được mô tả ở trên vào List.
5. Đổi sang token reseller khác để kiểm tra participation độc lập; đổi sang token merchant/admin để kiểm tra 403. Bỏ token để kiểm tra 401.
6. Swagger: mở `/swagger-ui/index.html`, chọn **Authorize → BearerAuth**, nhập token và dùng nhóm **Reseller campaigns**. Ba endpoint và scheme này đã được kiểm tra qua `/v3/api-docs`.

## Chạy lại kiểm thử

Cần Docker, JDK 23 và Maven wrapper. Từ thư mục `BE`, PowerShell:

```powershell
docker run --detach --rm --name reseller-phase1-test `
  -e POSTGRES_DB=reseller_phase1_test -e POSTGRES_USER=reseller_test `
  -e POSTGRES_PASSWORD=reseller_test_only `
  -p 127.0.0.1:55432:5432 postgres:15-alpine

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-23'
$env:RESELLER_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/reseller_phase1_test'

# Chỉ 72 test mới; tự cấu hình datasource, mail, JWT test và tắt admin bootstrap.
.\mvnw.cmd '-Dtest=ResellerCampaign*Test' test

# Toàn bộ 126 test: ép các test có sẵn sang cùng database riêng.
$env:DB_URL = $env:RESELLER_TEST_DB_URL + '?currentSchema=travel_v2'
$env:DB_USERNAME = 'reseller_test'
$env:DB_PASSWORD = 'reseller_test_only'
$env:SPRING_DATA_REDIS_PORT = '56379'
$env:MAIL_HOST = 'localhost'
$env:MAIL_PORT = '1025'
$env:MAIL_USERNAME = 'test'
$env:MAIL_PASSWORD = 'test'
$env:JWT_SECRET = 'phase1-test-only-secret-key-at-least-32-bytes-long'
$env:JWT_ACCESS_EXPIRATION = '900000'
$env:JWT_REFRESH_EXPIRATION = '604800000'
$env:ADMIN_BOOTSTRAP_ENABLED = 'false'
.\mvnw.cmd '-Dspring.config.import=' verify

docker stop reseller-phase1-test
```

Điều chỉnh JAVA_HOME theo máy. Chạy bằng shell test riêng để các biến môi trường test không ảnh hưởng lần chạy ứng dụng thông thường. Test integration mới chỉ được bật khi `RESELLER_TEST_DB_URL` khớp URL loopback có tên database `reseller_phase1_test`; thiếu biến này thì test được skip, unit test vẫn chạy.

**Dừng ở Phase 1. Chỉ bắt đầu Phase 2 — Campaign Applications sau khi người dùng xác nhận.**
