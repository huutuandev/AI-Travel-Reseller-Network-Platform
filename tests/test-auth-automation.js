const { execSync } = require('child_process');

const BASE_URL = 'http://localhost:8080/api/v1/auth';

function getRedisValue(key) {
    try {
        const output = execSync(`docker exec redis_cache redis-cli GET "${key}"`, { encoding: 'utf-8' });
        const trimmed = output.trim();
        return trimmed === '(nil)' ? null : trimmed;
    } catch (e) {
        return null;
    }
}

function delRedisKey(key) {
    try {
        execSync(`docker exec redis_cache redis-cli DEL "${key}"`);
    } catch (e) {}
}

async function requestApi(endpoint, method = 'POST', body = null, token = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${BASE_URL}${endpoint}`, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined
    });

    const data = await res.json().catch(() => ({}));
    return { status: res.status, ok: res.ok, data };
}

async function runEnterpriseTestSuite() {
    console.log('================================================================================');
    console.log('🚀 SUITE KIỂM THỬ ĐẦY ĐỦ: POSITIVE + NEGATIVE + BOUNDARY + VALIDATION');
    console.log('================================================================================\n');

    const timestamp = Date.now();
    const testEmail = `tester_${timestamp}@example.com`;
    const tempInactiveEmail = `temp_${timestamp}@example.com`;
    const defaultPassword = 'Password@123';
    const newPassword = 'NewPassword@456';
    
    let accessToken = '';
    let refreshToken = '';
    let resetToken = '';

    let passed = 0;
    let failed = 0;

    async function executeTest(testId, type, name, testFn) {
        const typeBadge = `[${type}]`.padEnd(12, ' ');
        process.stdout.write(`👉 ${testId.padEnd(12, ' ')} ${typeBadge} ${name.padEnd(52, '.')} `);
        try {
            await testFn();
            console.log('✅ PASSED');
            passed++;
        } catch (err) {
            console.log('❌ FAILED');
            console.log(`   └─ Chi tiết: ${err.message}`);
            failed++;
        }
    }

    // =========================================================================
    // PHẦN 1: ĐĂNG KÝ & XÁC THỰC EMAIL (REGISTRATION & VERIFICATION)
    // =========================================================================
    console.log('📦 --- PHẦN 1: ĐĂNG KÝ VÀ XÁC THỰC EMAIL ---');

    await executeTest('TC_REG_01', 'Validation', 'Bỏ trống toàn bộ các trường bắt buộc (400)', async () => {
        const res = await requestApi('/register', 'POST', { email: '', password: '', fullName: '' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_02', 'Validation', 'Email sai định dạng (thiếu @/domain) (400)', async () => {
        const res = await requestApi('/register', 'POST', { email: 'invalid_email_format', password: defaultPassword, fullName: 'Tester' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_03', 'Validation', 'Tên đầy đủ chỉ chứa khoảng trắng (400)', async () => {
        const res = await requestApi('/register', 'POST', { email: `space_${timestamp}@example.com`, password: defaultPassword, fullName: '      ' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_04', 'Boundary', 'Mật khẩu dưới biên chuẩn: 5 ký tự (Min - 1) (400)', async () => {
        const res = await requestApi('/register', 'POST', { email: `bnd5_${timestamp}@example.com`, password: '12345', fullName: 'Tester' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_05', 'Boundary', 'Mật khẩu đúng biên chuẩn: đúng 6 ký tự (Min) (200)', async () => {
        const res = await requestApi('/register', 'POST', { email: `bnd6_${timestamp}@example.com`, password: '123456', fullName: 'Tester' });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_06', 'Boundary', 'Mật khẩu đúng biên trên tối đa: 72 ký tự (Max) (200)', async () => {
        const maxPass = 'Aa1@' + 'x'.repeat(68); // Chuẩn 72 ký tự
        const res = await requestApi('/register', 'POST', { email: `bnd72_${timestamp}@example.com`, password: maxPass, fullName: 'Tester' });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_07', 'Boundary', 'Mật khẩu vượt biên trên tối đa: 73 ký tự (Max + 1) (400)', async () => {
        const overPass = 'Aa1@' + 'x'.repeat(69); // 73 ký tự (> 72 bytes)
        const res = await requestApi('/register', 'POST', { email: `bnd73_${timestamp}@example.com`, password: overPass, fullName: 'Tester' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_08', 'Positive', 'Đăng ký tài khoản chính chuẩn hợp lệ (200)', async () => {
        const res = await requestApi('/register', 'POST', { email: testEmail, password: defaultPassword, fullName: 'Nguyen Van Automation' });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_VER_01', 'Boundary', 'OTP dưới biên chuẩn: 5 chữ số (400)', async () => {
        const res = await requestApi('/verify-email', 'POST', { email: testEmail, otp: '12345' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_VER_02', 'Boundary', 'OTP vượt biên chuẩn: 7 chữ số (400)', async () => {
        const res = await requestApi('/verify-email', 'POST', { email: testEmail, otp: '1234567' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_VER_03', 'Validation', 'OTP chứa ký tự chữ cái không hợp lệ (400)', async () => {
        const res = await requestApi('/verify-email', 'POST', { email: testEmail, otp: '12AB56' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_VER_04', 'Positive', 'Xác thực Email bằng OTP 6 số chính xác từ Redis (200)', async () => {
        const otp = getRedisValue(`auth:email:verify:${testEmail}`);
        if (!otp) throw new Error('Không tìm thấy OTP trong Redis!');
        const res = await requestApi('/verify-email', 'POST', { email: testEmail, otp: otp });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_REG_08', 'Negative', 'Đăng ký lại email đã ACTIVE (409 Conflict)', async () => {
        const res = await requestApi('/register', 'POST', { email: testEmail, password: defaultPassword, fullName: 'Nguyen Van Automation' });
        if (res.status !== 409) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_RES_01', 'Positive', 'Gửi lại OTP kích hoạt cho user INACTIVE (200)', async () => {
        await requestApi('/register', 'POST', { email: tempInactiveEmail, password: defaultPassword, fullName: 'Temp User' });
        delRedisKey(`auth:email:verify:resend:${tempInactiveEmail}`); // Clear cooldown để test positive
        const res = await requestApi('/resend-verification', 'POST', { email: tempInactiveEmail });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_RES_02', 'Boundary', 'Cooldown 60s: Gửi lại OTP liên tiếp tức thì (409)', async () => {
        const res = await requestApi('/resend-verification', 'POST', { email: tempInactiveEmail });
        if (res.status !== 409) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_RES_03', 'Negative', 'Gửi lại OTP cho tài khoản đã ACTIVE (409)', async () => {
        const res = await requestApi('/resend-verification', 'POST', { email: testEmail });
        if (res.status !== 409) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    // =========================================================================
    // PHẦN 2: ĐĂNG NHẬP & QUẢN LÝ TOKEN (LOGIN & TOKEN MANAGEMENT)
    // =========================================================================
    console.log('\n📦 --- PHẦN 2: ĐĂNG NHẬP VÀ QUẢN LÝ TOKEN ---');

    await executeTest('TC_AUTH_01', 'Validation', 'Đăng nhập bỏ trống email hoặc mật khẩu (400)', async () => {
        const res = await requestApi('/login', 'POST', { email: '', password: '' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_AUTH_02', 'Negative', 'Đăng nhập sai mật khẩu (Mong đợi 400/401)', async () => {
        const res = await requestApi('/login', 'POST', { email: testEmail, password: 'WrongPassword@999' });
        if (res.status !== 400 && res.status !== 401) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_AUTH_03', 'Negative', 'Đăng nhập tài khoản chưa verify (INACTIVE) (409)', async () => {
        const res = await requestApi('/login', 'POST', { email: tempInactiveEmail, password: defaultPassword });
        if (res.status !== 409) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_AUTH_04', 'Positive', 'Đăng nhập hợp lệ và lấy Access & Refresh Token (200)', async () => {
        const res = await requestApi('/login', 'POST', { email: testEmail, password: defaultPassword });
        if (res.status !== 200 || !res.data.data?.accessToken) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
        accessToken = res.data.data.accessToken;
        refreshToken = res.data.data.refreshToken;
    });

    await executeTest('TC_TOK_01', 'Validation', 'Làm mới Token với chuỗi refreshToken rỗng (400)', async () => {
        const res = await requestApi('/refresh', 'POST', { refreshToken: '' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_TOK_02', 'Validation', 'Làm mới Token với chuỗi sai định dạng (400)', async () => {
        const res = await requestApi('/refresh', 'POST', { refreshToken: 'invalid_format_without_colon' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_TOK_03', 'Negative', 'Làm mới Token với Token không tồn tại/hết hạn (400)', async () => {
        const res = await requestApi('/refresh', 'POST', { refreshToken: 'fake-user-id:fake-token-id' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_TOK_04', 'Positive', 'Làm mới Token thành công (Token Rotation) (200)', async () => {
        const res = await requestApi('/refresh', 'POST', { refreshToken: refreshToken });
        if (res.status !== 200 || !res.data.data?.accessToken) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
        accessToken = res.data.data.accessToken;
        refreshToken = res.data.data.refreshToken;
    });

    await executeTest('TC_AUTH_05', 'Negative', 'Đăng xuất khi không truyền Bearer Token (401)', async () => {
        const res = await requestApi('/logout', 'POST', { refreshToken: refreshToken });
        if (res.status !== 401) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_AUTH_06', 'Negative', 'Đăng xuất với Token giả / sai chữ ký (401)', async () => {
        const res = await requestApi('/logout', 'POST', { refreshToken: refreshToken }, 'fake.jwt.token.here');
        if (res.status !== 401) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_AUTH_07', 'Positive', 'Đăng xuất hợp lệ với Bearer Token (200)', async () => {
        const res = await requestApi('/logout', 'POST', { refreshToken: refreshToken }, accessToken);
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    // =========================================================================
    // PHẦN 3: QUÊN & ĐỔI MẬT KHẨU (FORGOT & RESET PASSWORD)
    // =========================================================================
    console.log('\n📦 --- PHẦN 3: QUÊN VÀ ĐẶT LẠI MẬT KHẨU ---');

    await executeTest('TC_PWD_01', 'Validation', 'Quên mật khẩu với Email sai định dạng (400)', async () => {
        const res = await requestApi('/forgot-password', 'POST', { email: 'wrong_email_format' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_02', 'Positive', 'Yêu cầu gửi OTP quên mật khẩu (200)', async () => {
        const res = await requestApi('/forgot-password', 'POST', { email: testEmail });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_03', 'Boundary', 'Cooldown 60s: Yêu cầu OTP quên pass liên tiếp (409)', async () => {
        const res = await requestApi('/forgot-password', 'POST', { email: testEmail });
        if (res.status !== 409) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_04', 'Negative', 'Xác thực OTP Reset sai (400)', async () => {
        const res = await requestApi('/verify-reset-otp', 'POST', { email: testEmail, otp: '000000' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_05', 'Positive', 'Xác thực OTP Reset đúng và lấy ResetToken (200)', async () => {
        const resetOtp = getRedisValue(`auth:password-reset:${testEmail}`);
        if (!resetOtp) throw new Error('Không tìm thấy Reset OTP trong Redis!');
        const res = await requestApi('/verify-reset-otp', 'POST', { email: testEmail, otp: resetOtp });
        if (res.status !== 200 || !res.data.data) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
        resetToken = res.data.data;
    });

    await executeTest('TC_PWD_06', 'Boundary', 'Đặt lại mật khẩu mới dưới 6 ký tự (Min - 1) (400)', async () => {
        const res = await requestApi('/reset-password', 'POST', { resetToken: resetToken, newPassword: '123' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_07', 'Negative', 'Đặt lại mật khẩu với ResetToken giả/hết hạn (400)', async () => {
        const res = await requestApi('/reset-password', 'POST', { resetToken: 'fake-uuid-token', newPassword: newPassword });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_08', 'Positive', 'Đặt lại mật khẩu mới thành công (200)', async () => {
        const res = await requestApi('/reset-password', 'POST', { resetToken: resetToken, newPassword: newPassword });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_09', 'Negative', 'Dùng lại ResetToken đã sử dụng trước đó (400)', async () => {
        const res = await requestApi('/reset-password', 'POST', { resetToken: resetToken, newPassword: 'AnotherPassword@999' });
        if (res.status !== 400) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    await executeTest('TC_PWD_10', 'Positive', 'Đăng nhập thành công với mật khẩu mới vừa đổi (200)', async () => {
        const res = await requestApi('/login', 'POST', { email: testEmail, password: newPassword });
        if (res.status !== 200 || !res.data.success) throw new Error(`Status ${res.status}: ${JSON.stringify(res.data)}`);
    });

    console.log('\n================================================================================');
    console.log(`📊 TỔNG KẾT BÁO CÁO KIỂM THỬ ENTERPRISE TEST SUITE:`);
    console.log(`   - Tổng số test cases : ${passed + failed}`);
    console.log(`   - Thành công (Passed) : ${passed}`);
    console.log(`   - Thất bại (Failed)   : ${failed}`);
    console.log(`   - Tỷ lệ đạt (Coverage): ${Math.round((passed / (passed + failed)) * 100)}%`);
    console.log('================================================================================\n');
}

runEnterpriseTestSuite();
