# AI-Travel-Reseller-Network-Platform

## Yêu cầu

- Git
- Docker Desktop

## Chạy dự án

### 1. Clone project

git clone https://github.com/huutuandev/AI-Travel-Reseller-Network-Platform.git

### 2. Vào thư mục project

cd AI-Travel-Reseller-Network-Platform

### 3. Chạy Docker

docker compose up --build

### 4. Mở Frontend

Truy cập:

http://localhost:3000

## Dừng dự án

docker compose down

## Chạy lại

docker compose up


## Phạm vi hiện tại

Backend tạm giữ đăng nhập, refresh token và logout. API nghiệp vụ actor, đăng ký và khôi phục mật khẩu tắt mặc định qua app.features trong BE/src/main/resources/application.yaml.

Phân tích kiến trúc, database và hướng thay đổi: [PROJECT_DATABASE_REVIEW.md](docs/PROJECT_DATABASE_REVIEW.md).


## Database mới (18 bảng)

Backend hiện dùng schema travel_v2 do Flyway tạo khi khởi động; Hibernate chỉ validate. Dữ liệu và tài khoản cũ không tự chuyển sang schema mới. Xem [hướng dẫn database mới](docs/DATABASE_V2.md). Mã nghiệp vụ cũ đã lưu tại archive/legacy-v1, ngoài phần build.


## Platform Admin

Đã bổ sung API Platform Admin tại /api/v1/admin, migration V2 và bootstrap tài khoản Admin tùy chọn. Xem [hợp đồng API và hướng dẫn chạy](docs/PLATFORM_ADMIN_API.md). Phạm vi hiện tại gồm đăng nhập và Platform Admin; nghiệp vụ Tenant/Reseller vẫn tạm ngưng.

