# Schema mới — 29/10/2026

Nguồn: bản SQL đầy đủ 18 bảng người dùng cung cấp. File được giữ nguyên tại BE/src/main/resources/db/redesign/V1__approved_18_table_schema.sql.

Backend dùng PostgreSQL schema travel_v2 trong database cấu hình bởi DB_URL. Flyway tạo schema và chạy migration mới khi backend khởi động. Hibernate dùng cùng schema và ddl-auto: validate. Migration V1 cũ vẫn giữ nguyên để bảo toàn lịch sử, nhưng không nằm trong locations đang chạy.

Chạy bằng docker compose up --build như README. Database role cần quyền CREATE schema. Đây là schema mới rỗng: tài khoản và dữ liệu trong public không tự chuyển sang travel_v2. Cần cấp tài khoản ACTIVE với password_hash BCrypt và full_name trước khi đăng nhập; role cần name và scope. Không có mật khẩu mặc định. Chưa thực thi migration lên PostgreSQL thực tế trong phiên chỉnh sửa này.

Chỉ User và Role được map JPA ở giai đoạn đăng nhập. Các bảng nghiệp vụ khác do Flyway tạo đúng SQL, chưa triển khai lại entity/service. Mã nghiệp vụ cũ và test tương ứng được chuyển tới archive/legacy-v1, nằm ngoài source Maven; không bật lại bằng actor-functions. Cờ account-management vẫn tắt đăng ký/OTP/reset. Login, refresh và logout giữ hoạt động.

Email lookup dùng ignore case tương ứng unique index lower(email). User thêm emailVerifiedAt, cập nhật độ dài fullName/phone/status. Role thêm scope. DDL nghiệp vụ được giữ theo bản duyệt; các trigger kiểm tra scope khi sửa bản ghi con, chưa ngăn mọi thay đổi trên bản ghi cha (ví dụ chuyển product sang merchant khác). Khi triển khai nghiệp vụ cần quy định các quan hệ nguồn bất biến hoặc bổ sung kiểm tra cập nhật cha.

Tài liệu PROJECT_DATABASE_REVIEW.md mô tả trạng thái trước lần thay schema này; tài liệu hiện tại thay thế các hướng dẫn bật lại nghiệp vụ và lựa chọn schema trong báo cáo đó.