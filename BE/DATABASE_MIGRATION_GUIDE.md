# HƯỚNG DẪN MIGRATION VÀ VẬN HÀNH DATABASE (DATABASE_MIGRATION_GUIDE.md)

**Hệ thống:** AI Travel Reseller Network Platform  
**Công cụ Migration:** Flyway Community Edition  
**PostgreSQL Version:** 15+  
**Schema mục tiêu:** `travel_v2`  

---

## 1. CẤU HÌNH FLYWAY TRONG SPRING BOOT

### 1.1. Khai báo Dependencies (`pom.xml`)
Hệ thống sử dụng các thư viện chính thức tương thích với Spring Boot 4.1.1 và PostgreSQL 15+:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-flyway</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

### 1.2. Cấu hình Datasource & Flyway (`application.yaml`)

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/mydatabase}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: travel_v2

  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: travel_v2
    schemas: travel_v2
    create-schemas: true
```

**Nguyên tắc vận hành:**
1. `flyway.enabled: true`: Flyway luôn chạy trước khi Hibernate khởi động để đảm bảo cấu trúc database luôn đồng bộ với mã nguồn.
2. `jpa.hibernate.ddl-auto: validate`: Hibernate tuyệt đối không được tự động sinh bảng hay thay đổi kiểu dữ liệu (`update`/`create`); Hibernate chỉ làm nhiệm vụ kiểm tra và xác thực tính tương thích của Java Entity.
3. Không hardcode thông tin bảo mật. Mọi thông tin kết nối phải truyền qua biến môi trường (`.env` hoặc runtime environment variables).

---

## 2. QUY TRÌNH THỰC HIỆN SAO LƯU (BACKUP) DATABASE

Trước bất kỳ đợt triển khai migration nào trên môi trường Staging hoặc Production, bắt buộc phải sao lưu dữ liệu toàn diện.

### 2.1. Sao lưu qua Docker Container
```powershell
# Sao lưu ra file SQL thuần
docker exec postgres_db pg_dump -U root -d mydatabase > backup_before_migration_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql

# Sao lưu định dạng nén tối ưu (Custom format - hỗ trợ pg_restore song song)
docker exec postgres_db pg_dump -U root -d mydatabase -Fc -f /var/lib/postgresql/data/backup_full.dump
```

### 2.2. Sao lưu chỉ riêng Schema `travel_v2`
```powershell
docker exec postgres_db pg_dump -U root -d mydatabase -n travel_v2 > backup_travel_v2_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql
```

### 2.3. Khôi phục dữ liệu khi cần Rollback
```powershell
# Khôi phục từ file SQL
docker exec -i postgres_db psql -U root -d mydatabase < backup_travel_v2_YYYYMMDD_HHMMSS.sql

# Khôi phục từ file Custom dump
docker exec postgres_db pg_restore -U root -d mydatabase --clean --if-exists /var/lib/postgresql/data/backup_full.dump
```

---

## 3. CÁCH CHẠY VÀ KIỂM TRA MIGRATION

### 3.1. Chạy Migration tự động qua Ứng dụng
Khi khởi động ứng dụng Spring Boot:
```powershell
.\mvnw.cmd spring-boot:run
```
Flyway sẽ tự động quét thư mục `src/main/resources/db/migration`, so sánh danh sách migration với bảng `travel_v2.flyway_schema_history`, và áp dụng lần lượt các version mới theo thứ tự `V1` -> `V16`.

### 3.2. Kiểm tra trạng thái Migration trực tiếp trong PostgreSQL
Kết nối vào database và kiểm tra bảng lịch sử:
```sql
SELECT installed_rank, version, description, type, script, checksum, execution_time, success
FROM travel_v2.flyway_schema_history
ORDER BY installed_rank;
```

Kết quả hợp lệ:
- Tất cả các dòng đều có `success = true`.
- Phiên bản hiện tại đạt `v16`.
- Không có dòng nào bị lỗi hoặc dừng giữa chừng.

### 3.3. Kiểm tra danh sách bảng và Trigger đã tạo
```sql
-- Kiểm tra đủ 19 bảng mục tiêu + 2 bảng platform
SELECT tablename FROM pg_tables WHERE schemaname = 'travel_v2' ORDER BY tablename;

-- Kiểm tra danh sách trigger hoạt động
SELECT trigger_name, event_manipulation, event_object_table, action_statement
FROM information_schema.triggers
WHERE trigger_schema = 'travel_v2'
ORDER BY event_object_table;
```

---

## 4. HƯỚNG DẪN TRIỂN KHAI PRODUCTION (PRODUCTION DEPLOYMENT)

### Bước 1: Kiểm tra tính sẵn sàng (Pre-flight checks)
- Xác nhận container PostgreSQL đã hoàn tất khởi động và vượt qua healthcheck.
- Đảm bảo database user có quyền `CREATE` trên schema `travel_v2` và quyền tạo triggers/functions.
- Kiểm tra dung lượng ổ đĩa khả dụng để chứa file backup.

### Bước 2: Thực hiện Backup Production
- Chạy lệnh `pg_dump` sao lưu dữ liệu hiện có (xem mục 2).

### Bước 3: Triển khai Migration (Blue-Green hoặc Maintenance Window)
- Chạy bản build ứng dụng mới:
```bash
docker compose up -d --build backend
```
- Theo dõi log khởi động của backend để xác nhận:
  - `DbValidate: Successfully validated 16 migrations`
  - `DbMigrate: Current version of schema "travel_v2": 16`
  - `LocalContainerEntityManagerFactoryBean: Initialized JPA EntityManagerFactory`

### Bước 4: Kiểm tra tính toàn vẹn (Post-deployment verification)
- Chạy smoke test các API:
  - `POST /api/v1/auth/login`
  - `GET /api/v1/admin/tenants`
  - `GET /api/v1/admin/overview`
- Kiểm tra metrics và nhật ký ứng dụng không phát sinh ngoại lệ `SchemaManagementException`.

---

## 5. HƯỚNG DẪN XỬ LÝ SỰ CỐ MIGRATION AN TOÀN

### 5.1. Khi gặp lỗi Checksum Mismatch (Sửa nhầm file migration cũ)
- **Triệu chứng:** `Migration checksum mismatch for migration version X`.
- **Nguyên nhân:** File migration đã áp dụng trên DB bị sửa nội dung (dấu cách, comment, code SQL).
- **Quy tắc tuyệt đối:** Migration đã chạy phải xem là bất biến (Immutable).
- **Cách xử lý chuẩn:**
  1. Nếu trên môi trường Staging/Dev: Kiểm tra file `src/main/resources/db/migration/VX...sql` so với commit git trước đó. Nếu sửa nhầm, hãy revert file về nội dung gốc.
  2. Nếu cần sửa logic schema thực sự: **Không sửa file cũ**. Hãy tạo một migration mới `V17__fix_...sql` để thực hiện câu lệnh `ALTER TABLE` hoặc cập nhật hàm.
  3. Chỉ sử dụng lệnh `flyway.repair()` khi có sự chỉ định rõ ràng của Lead Engineer sau khi đã xác nhận schema DB và file SQL hoàn toàn nhất quán.

### 5.2. Khi Migration bị lỗi giữa chừng (Failed Migration)
- **Triệu chứng:** `FlywayException: Migration failed!`.
- **Nguyên nhân:** Lỗi cú pháp SQL, vi phạm ràng buộc dữ liệu cũ hoặc bảng bị khóa do transaction khác.
- **Cách xử lý an toàn:**
  1. Kiểm tra log chi tiết để xác định chính xác câu lệnh bị lỗi:
     ```sql
     SELECT * FROM travel_v2.flyway_schema_history WHERE success = false;
     ```
  2. PostgreSQL hỗ trợ DDL trong transaction: hầu hết các migration DDL bị lỗi sẽ tự động rollback.
  3. Xóa bản ghi migration lỗi khỏi bảng lịch sử:
     ```sql
     DELETE FROM travel_v2.flyway_schema_history WHERE success = false;
     ```
  4. Khắc phục nguyên nhân gốc rễ (sửa file migration hoặc xử lý dữ liệu vi phạm).
  5. Chạy lại ứng dụng để Flyway thực thi lại migration đã được sửa.
