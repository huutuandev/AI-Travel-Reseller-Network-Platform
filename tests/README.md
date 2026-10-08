# 🧪 QA & AUTOMATION TEST SUITE - AI TRAVEL PLATFORM

Thư mục này chứa toàn bộ tài nguyên kiểm thử (Automation & Manual/Postman) cho hệ thống **Authentication APIs**.

---

## 📁 Cấu Trúc Thư Mục

```text
tests/
├── test-auth-automation.js              # Script NodeJS kiểm thử tự động 37 Test Cases (Zero-dependency)
├── AI_Travel_Auth_Postman_Collection.json # File Collection chuẩn Postman v2.1
└── README.md                            # Hướng dẫn kiểm thử
```

---

## 🚀 1. Chạy Kiểm Thử Tự Động (Automation Testing)

Script tự động chạy toàn bộ **37 Test Cases** (Positive, Negative, Boundary, Validation), tự sinh email, tự đọc OTP từ Redis, kiểm tra Token rotation, quên mật khẩu và in báo cáo.

### Cách chạy:
Đảm bảo hệ thống Backend và Docker đang chạy (`docker compose up -d`), sau đó mở terminal tại thư mục gốc dự án và gõ:

```bash
node tests/test-auth-automation.js
```

---

## 📮 2. Kiểm Thử Bằng Postman (Postman Collection)

1. Mở **Postman** -> Bấm **`Import`**.
2. Chọn file `tests/AI_Travel_Auth_Postman_Collection.json`.
3. Trong Collection, các biến `baseUrl`, `accessToken`, `refreshToken`, `resetToken` đã được lập trình sẵn để tự động truyền giữa các request.

---

## 📊 3. Tóm Tắt 37 Test Cases

* **Positive Tests (10 TCs):** Đăng ký hợp lệ, xác thực OTP, đăng nhập, refresh token rotation, đổi mật khẩu, đăng xuất.
* **Negative Tests (11 TCs):** Trùng email active, sai OTP, sai mật khẩu, tài khoản inactive, token giả mạo, dùng lại resetToken.
* **Boundary Tests (9 TCs):** Mật khẩu 5 ký tự (Min-1), 6 ký tự (Min), 72 ký tự (Max), 73 ký tự (Max+1); OTP 5 số, 7 số; Cooldown 60s.
* **Validation Tests (7 TCs):** Bỏ trống field, email sai định dạng, họ tên chứa khoảng trắng, OTP chứa chữ cái, refreshToken rỗng/sai format.
