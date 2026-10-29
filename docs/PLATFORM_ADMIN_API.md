# Platform Admin — hợp đồng backend

Triển khai dựa trên schema travel_v2 gồm 18 bảng, không sử dụng entity nghiệp vụ cũ trong archive. Admin dùng SQL có tham số qua AdminRepository để tổng hợp trên các bảng hiện có; không cần khôi phục toàn bộ entity sản phẩm/chiến dịch. Controller -> AdminService (transaction) -> AdminRepository (NamedParameterJdbcTemplate).

## Khởi động và tài khoản

Flyway tự chạy V1 rồi V2 trong travel_v2 khi backend khởi động. V2 thêm merchants.version, platform_settings, platform_events và seed ROLE_ADMIN (scope PLATFORM). Không có tài khoản/mật khẩu mặc định.

Để tạo tài khoản Admin mới, thêm vào .env dùng bởi backend:

```properties
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_EMAIL=your-admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=<mat-khau-rieng-tu-12-den-72-ky-tu>
```

Khởi động bằng docker compose up --build. Bootstrap chỉ tạo tài khoản mới; không đổi mật khẩu hay nâng quyền user có sẵn. Sau lần tạo thành công, tắt ADMIN_BOOTSTRAP_ENABLED và xóa mật khẩu bootstrap khỏi cấu hình. Đăng nhập POST /api/v1/auth/login với email/password, dùng accessToken làm Bearer trong Swagger /swagger-ui/index.html.

Mọi API /api/v1/admin/** yêu cầu ROLE_ADMIN. Filter JWT đọc lại user và role hiện tại từ DB; tài khoản không ACTIVE hoặc đã bị thu hồi quyền không tiếp tục truy cập bằng token cũ. Việc đình chỉ merchant không khóa user sở hữu merchant.

## API

Response chung: {success, message, data}. UUID dùng cho path id, code dùng hiển thị mã Tenant.

| Method | Path dưới /api/v1/admin | Query / body |
|---|---|---|
| GET | /tenants | page=0,size=10,search=,status= |
| GET | /tenants/{id} | UUID |
| PATCH | /tenants/{id}/status | status,reason,version |
| GET | /tenants/{id}/logs | page=0,size=20 |
| GET | /dashboard/overview | days=30 (1..366) |
| GET | /dashboard/recent-activities | limit=10 (1..100) |
| GET | /system/config | — |
| PUT | /system/config | routeMode,baseDomain,captionTemplate,videoTemplate,version |
| GET | /system/logs | page=0,size=20 |

Phân trang trả content,totalElements,totalPages,number,size. size tối đa 100. Danh sách sắp xếp created_at DESC, id để ổn định. Search không phân biệt hoa/thường, tìm theo name/code/subdomain hoặc UUID chính xác. status lọc ACTIVE/PENDING/SUSPENDED; bỏ trống để lấy tất cả.

Chi tiết Tenant có contact (name,ownerEmail,email,phone,address), branding (logoUrl,primaryColor,secondaryColor; null khi chưa có), stats. Không suy diễn storefrontStatus READY khi chưa có routing thật.

## Quy tắc trạng thái

PENDING -> ACTIVE: phê duyệt. ACTIVE -> SUSPENDED: đình chỉ, bắt buộc reason không trắng, tối đa 1000 ký tự. SUSPENDED -> ACTIVE: kích hoạt lại. Các chuyển đổi khác trả 409. Gửi version lấy từ GET danh sách/chi tiết; version cũ trả 409. UPDATE và audit event trong cùng transaction, khóa dòng FOR UPDATE ngăn quyết định đồng thời ghi đè.

```json
{"status":"SUSPENDED","reason":"Cần hoàn thiện hồ sơ doanh nghiệp","version":0}
```

Tenant API/storefront hiện chưa triển khai lại. Khi mở các chức năng đó, chúng phải kiểm tra merchants.status=ACTIVE; bản Admin hiện cập nhật trạng thái và loại merchant bị đình chỉ khỏi thống kê campaign đang chạy, không tự xóa campaign hay khóa tài khoản người sở hữu.

## Cấu hình preview

routeMode chỉ nhận SUBDOMAIN_PREVIEW; baseDomain dạng tên miền chữ thường, không có protocol/path. Hai template tối đa 10000 ký tự, cho phép chuỗi rỗng. PUT cần version; cập nhật đồng thời trả 409. Lưu cấu hình không tạo DNS/SSL và chưa gọi AI provider.

```json
{"routeMode":"SUBDOMAIN_PREVIEW","baseDomain":"aitravel.example","captionTemplate":"Use verified product details.","videoTemplate":"Create a short travel script.","version":0}
```

## Định nghĩa số liệu

- Total/active/pending/suspended là tổng hiện tại. activePercentage làm tròn một chữ số thập phân, mẫu số 0 trả 0.
- newTenantsThisMonth tính từ đầu tháng theo Asia/Ho_Chi_Minh đến thời điểm truy vấn.
- usage.attributedBookings: booking_status CONFIRMED, có tracking_link_id, created_at thuộc [from,to). days mặc định 30 ngày liên tục. Response trả from,to,timezone.
- runningCampaigns: trạng thái ACTIVE, merchant ACTIVE, start_date <= thời điểm hiện tại hoặc null, end_date > hiện tại hoặc null. Đây là số hiện tại, không phải số tích lũy trong kỳ.
- stats.totalResellers của Tenant: COUNT DISTINCT reseller qua các application APPROVED của các campaign thuộc Tenant.
- stats.attributedBookings của Tenant: CONFIRMED, có tracking link, toàn thời gian; bookingsPeriod=ALL_TIME.
- trackedClicks/clicks30Days trả null và clicksAvailability=NOT_COLLECTED. Schema hiện có tổng click, chưa có lịch sử theo ngày. Frontend cần hiển thị chưa có dữ liệu, không đổi thành 0.
- CONFIRMED và APPROVED là hợp đồng trạng thái cho các module booking/application phát triển sau này; cần dùng nhất quán.

## Nhật ký và giới hạn phạm vi

platform_events chứa tenantId,actorUserId,eventType,area,result,previousStatus,newStatus,reason,createdAt. Module hiện ghi TENANT_STATUS_CHANGED và PREVIEW_SETTINGS_UPDATED thành công. Recent tenant activities chỉ lấy sự kiện có merchant_id. Không tạo lịch sử giả cho đăng ký/campaign/branding cũ; khi triển khai các module đó cần ghi sự kiện tại nguồn. System logs ở phiên bản này là lịch sử thao tác đã lưu, không phải toàn bộ log server/authentication/AI. Không lưu prompt hoặc mật khẩu vào audit.

Lỗi: 400 dữ liệu/UUID sai, 401 chưa xác thực, 403 thiếu quyền, 404 không có Tenant, 409 xung đột version hoặc chuyển trạng thái sai. SQL dùng tham số và tên bảng cố định.

## Kiểm tra

Test MVC kiểm tra anonymous, reseller, admin, UUID sai, thiếu version, tài khoản bị khóa và role bị thu hồi. Test service kiểm tra quy tắc chuyển trạng thái, reason, version conflict, ghi audit, giới hạn phân trang. Các test auth hiện hữu vẫn chạy.

Chưa xác nhận migration và truy vấn bằng PostgreSQL thực tế trong môi trường này; chưa chạy contextLoads phụ thuộc PostgreSQL/Redis/SMTP. Cần chạy trên database phát triển trước khi tích hợp UI. Không dùng kết quả unit test làm bằng chứng SQL/migration đã chạy thành công.