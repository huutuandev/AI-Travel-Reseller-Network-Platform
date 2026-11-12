# Rà soát dự án và hướng thay đổi database

Ngày: 29/10/2026. Phạm vi: mã nguồn, migration, cấu hình chạy, frontend và các test hiện có. Chưa kết nối PostgreSQL/Redis thực tế; các nhận xét database dưới đây dựa trên schema trong repository. Hai file SRS .docx đang bị xóa trong working tree khi bắt đầu; không khôi phục hoặc dùng chúng làm yêu cầu nghiệp vụ hiện hành.

## 1. Kiến trúc và mức độ hoàn thiện

- Backend: Java 17 theo pom, Spring Boot 4.1.1, Spring MVC, JPA, Spring Security, JWT, Flyway, PostgreSQL, Redis, SMTP, OpenAPI.
- Frontend: Next.js 16.3.8 / React 19.2.8. app/page.tsx vẫn là trang mẫu; chưa có màn hình đăng nhập hay màn hình theo actor.
- Docker Compose có frontend, backend, PostgreSQL, Redis và RabbitMQ. Chưa thấy dependency hoặc producer/consumer RabbitMQ trong backend.
- Luồng chạy backend: controller -> service -> repository -> PostgreSQL. Redis lưu OTP, reset token và refresh token; JWT access token được xác minh bằng chữ ký.
- Merchant có onboarding, xem/sửa hồ sơ, xem sản phẩm của mình. Product có tạo, xem, sửa và archive. Các miền campaign, nội dung AI, tracking, hội thoại, đơn hàng, hoa hồng hiện chủ yếu mới có entity/schema.
- Các role xuất hiện trong code: ROLE_RESELLER và ROLE_ADMIN. Merchant được nhận diện chủ yếu qua hồ sơ merchants.user_id, chưa có cơ chế gán ROLE_MERCHANT trong onboarding.

## 2. Phạm vi tạm thời chỉ giữ đăng nhập

Các API đang giữ:

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| POST | /api/v1/auth/login | Email/password -> access token + refresh token |
| POST | /api/v1/auth/refresh | Xoay refresh token, phát access token mới |
| POST | /api/v1/auth/logout | Thu hồi refresh token được gửi lên |

Swagger/OpenAPI vẫn truy cập được. AuthController chỉ chứa ba API trên. AccountController chứa đăng ký, xác thực email, gửi lại OTP và khôi phục mật khẩu, nhưng không được tạo bean khi cờ account-management tắt. MerchantController và ProductController tương tự với cờ actor-functions.

Cấu hình mặc định trong application.yaml:

```yaml
app:
  features:
    actor-functions: false
    account-management: false
```

Mã service, DTO, repository, entity và dữ liệu cũ được giữ để khôi phục sau khi chốt schema. Việc tắt API không loại bỏ các bảng nghiệp vụ khỏi JPA. API nghiệp vụ đã tắt không có handler và không xuất hiện trong OpenAPI; request chưa đăng nhập tới đường dẫn nghiệp vụ vẫn có thể nhận 401 do bộ lọc bảo mật, request đã xác thực nhận 404. Đổi cờ thành true sẽ khôi phục nhóm controller tương ứng, nên cần kiểm tra lại phân quyền trước khi bật.

Đăng nhập dùng tài khoản ACTIVE có sẵn và password_hash BCrypt. Chưa có seed tài khoản trong migration, và đăng ký đang tắt, nên database trống chưa có tài khoản để đăng nhập. Không thêm tài khoản/mật khẩu mặc định khi chưa có yêu cầu cấp tài khoản. Logout hiện chỉ thu hồi refresh token; access token vẫn có hiệu lực đến khi hết hạn.

## 3. Database hiện tại

Schema gồm 12 bảng:

| Nhóm | Bảng | Quan hệ chính |
| --- | --- | --- |
| Danh tính | users, roles, user_roles | User nhiều role qua bảng nối |
| Hồ sơ actor | merchants, resellers | Mỗi hồ sơ liên kết duy nhất một user |
| Kinh doanh | products, campaigns | Product thuộc merchant; campaign trỏ merchant và product |
| Nội dung | contents | Thuộc campaign |
| Attribution | tracking_links, conversations | Link nối reseller/campaign/product; conversation thuộc link |
| Doanh thu | orders, commissions | Order trỏ merchant/link/conversation; commission trỏ order/reseller |

Các vấn đề cụ thể cần xử lý trước khi mở lại nghiệp vụ:

1. Flyway và Hibernate ddl-auto:update cùng quản lý schema. Cần chuyển sang migration làm nguồn duy nhất, rồi dùng validate sau khi đối chiếu schema thực tế và mapping. Không đổi sang validate ngay khi chưa kiểm tra database đang chạy.
2. Campaign lưu merchant_id và product_id riêng, chưa có ràng buộc bảo đảm product thuộc đúng merchant. Tracking link cũng lưu product_id cạnh campaign_id; order lưu merchant_id cạnh tracking_link_id. Từng FK hợp lệ chưa bảo đảm cả chuỗi nghiệp vụ nhất quán.
3. Commission.reseller_id chưa được ràng buộc phải trùng reseller của tracking link gắn với order. Conversation gắn vào order cũng chưa được bảo đảm thuộc cùng tracking link.
4. Nhiều trường tên, ngày, tỷ lệ hoa hồng, số tiền và status cho phép NULL. CHECK không tự chặn NULL; cần kết hợp NOT NULL theo yêu cầu nghiệp vụ.
5. Một campaign chỉ gắn một product. Cần quyết định có campaign nhiều sản phẩm hay không trước khi tạo bảng nối hoặc giữ cấu trúc hiện hành.
6. Commission chỉ lưu tổng và phân bổ; chưa có lịch sử chuyển trạng thái, payout, đối soát hoặc snapshot đầy đủ của điều khoản tại thời điểm phát sinh đơn.
7. Email UNIQUE hiện phân biệt hoa/thường; lookup và đăng nhập dùng email nguyên bản. Cần chốt chuẩn hóa, rà soát dữ liệu trùng rồi bổ sung unique index phù hợp.
8. idx_tracking_links_code và idx_commissions_order trùng khả năng lập chỉ mục của UNIQUE đã có. Một số FK phục vụ truy vấn lại chưa có index riêng; cần chọn theo truy vấn thực tế.
9. created_at/updated_at không có NOT NULL ở schema; updated_at do Hibernate cập nhật nên thao tác SQL trực tiếp không tự cập nhật.
10. Role được tạo lười trong register, không có seed rõ ràng. Cần seed role có tính lặp lại an toàn; tách cấp tài khoản khỏi hồ sơ kinh doanh.

## 4. Hướng xử lý đề xuất, chưa áp dụng schema mới

Bước đầu cần xác định phạm vi: chỉ auth database để dựng lại nền tảng, hay thiết kế đầy đủ nghiệp vụ tương lai. Không thể suy ra schema nghiệp vụ mới chỉ từ việc schema cũ không còn phù hợp.

Nếu chỉ giữ nền đăng nhập: users + roles + user_roles là lõi PostgreSQL; refresh token hiện tiếp tục ở Redis. Cần tách entity/repository nghiệp vụ ra khỏi persistence đang chạy trước khi dùng schema chỉ có ba bảng. Không chỉ xóa các câu CREATE TABLE trong V1 vì JPA vẫn quản lý entity cũ.

Nếu thiết kế đầy đủ: chốt actor và quyền, loại sản phẩm/biến thể/giá, quan hệ campaign-product, quy tắc attribution, nguồn đơn hàng, hoàn/hủy, công thức và thời điểm chốt hoa hồng, quy trình payout. Sau đó mới thiết kế ERD, constraint, index và migration tương ứng.

Với database đã chạy V1, giữ nguyên migration V1; thay đổi qua V2 trở đi. Trước khi migration cần đối chiếu flyway_schema_history, schema thực tế và dữ liệu vi phạm ràng buộc. Có thể cân nhắc đưa bảng nghiệp vụ cũ sang schema lưu trữ sau khi sao lưu và xác định nhu cầu giữ dữ liệu. Chưa chạy migration, xóa bảng hoặc reset Docker volume trong lần thay đổi này.

## 5. Các điểm kỹ thuật cần xem lại khi mở rộng

- CORS đang bị tắt; frontend và backend khác cổng, nên cần proxy cùng origin hoặc cấu hình CORS khi nối giao diện.
- JWT filter lấy quyền từ token, không đọc lại trạng thái tài khoản; tài khoản vừa bị khóa vẫn có thể dùng access token tới lúc hết hạn.
- Rotate refresh token dùng get rồi delete, chưa nguyên tử; có khả năng hai request đồng thời dùng cùng token.
- AuthenticationException chưa có handler riêng trong GlobalExceptionHandler; lỗi sai mật khẩu có thể rơi vào handler Exception trả 500.
- RedisService.deleteByPattern dùng KEYS; cần cân nhắc SCAN hoặc chỉ mục phiên theo user nếu dữ liệu phiên lớn.
- OTP hiện chưa có bộ đếm số lần thử; cần hoàn thiện nếu mở lại đăng ký hoặc reset password.
- Các test service hiện hữu vẫn giữ để bảo toàn khả năng khôi phục nghiệp vụ. Test LoginOnlyEndpointsTest bổ sung kiểm tra login còn hoạt động và nhóm API bị tắt không gọi service.
## 6. Kết quả kiểm tra

Đã compile backend và chạy 36 test thuộc LoginOnlyEndpointsTest, ObsoleteEndpointsTest, AuthLifecycleTest, AuthServiceRegisterTest, MerchantServiceTest và ProductServiceTest: tất cả thành công. Dùng JDK 21, compiler release 17, Maven 3.9.16 và dependency cache offline. Chưa chạy contextLoads tích hợp với PostgreSQL/Redis/SMTP thực tế. git diff --check thành công.

