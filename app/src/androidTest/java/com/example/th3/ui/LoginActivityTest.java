package com.example.th3.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.th3.R;
import com.example.th3.database.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Kiểm thử màn hình đăng nhập trên emulator:
 * đúng, sai username, sai password, bỏ trống cả hai trường,
 * bỏ trống từng trường, và điều hướng sau đăng nhập thành công.
 * <p>
 * Tương tác view được thực hiện trực tiếp trên UI thread
 * (setText/performClick) thay vì Espresso, vì system image API 37.2
 * preview của emulator không dispatch window focus ổn định vào app
 * (mà Espresso cần khi thực hiện action).
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    private Instrumentation instrumentation;
    private Instrumentation.ActivityMonitor mainMonitor;
    private Activity loginActivity;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DB_NAME);
        mainMonitor = new Instrumentation.ActivityMonitor(MainActivity.class.getName(), null, false);
        instrumentation.addMonitor(mainMonitor);
    }

    @After
    public void tearDown() {
        // Đóng activity còn sót lại để không đè lên activity của test kế tiếp
        if (loginActivity != null && !loginActivity.isDestroyed()) {
            instrumentation.runOnMainSync(loginActivity::finish);
            instrumentation.waitForIdleSync();
        }
        loginActivity = null;
        instrumentation.removeMonitor(mainMonitor);
    }

    // ------------------------------------------------------------------
    // 1. Đăng nhập đúng → mở MainActivity, LoginActivity finish()
    // ------------------------------------------------------------------

    @Test
    public void login_thanhCong_moMainActivity_vaCloseLogin() {
        launchLogin();
        setText(R.id.et_username, "admin");
        setText(R.id.et_password, "123456");
        clickLogin();

        Activity main = mainMonitor.waitForActivityWithTimeout(5000);
        assertNotNull("MainActivity phải được mở sau khi đăng nhập đúng", main);

        assertTrue("LoginActivity phải finish() sau khi đăng nhập thành công",
                waitForLoginDestroyed(5000));

        // Đóng MainActivity để không đè lên LoginActivity của test kế tiếp
        instrumentation.runOnMainSync(main::finish);
        instrumentation.waitForIdleSync();
    }

    // ------------------------------------------------------------------
    // 2. Sai username
    // ------------------------------------------------------------------

    @Test
    public void login_saiUsername_hienThongBaoLoi() {
        assertLoginFailed("userx", "123456");
    }

    // ------------------------------------------------------------------
    // 3. Sai password
    // ------------------------------------------------------------------

    @Test
    public void login_saiPassword_hienThongBaoLoi() {
        assertLoginFailed("admin", "sai-pass");
    }

    // ------------------------------------------------------------------
    // 4. Bỏ trống cả hai trường
    // ------------------------------------------------------------------

    @Test
    public void login_boTrongCaHai_hienBaoLoiUsername() {
        launchLogin();
        clickLogin();
        checkErrorText(R.string.error_empty_username);
        assertEquals("Không được điều hướng khi input trống", 0, mainMonitor.getHits());
    }

    // ------------------------------------------------------------------
    // 5. Bỏ trống từng trường
    // ------------------------------------------------------------------

    @Test
    public void login_boTrongUsername_hienBaoLoiUsername() {
        launchLogin();
        setText(R.id.et_password, "123456");
        clickLogin();
        checkErrorText(R.string.error_empty_username);
        assertEquals(0, mainMonitor.getHits());
    }

    @Test
    public void login_boTrongPassword_hienBaoLoiPassword() {
        launchLogin();
        setText(R.id.et_username, "admin");
        clickLogin();
        checkErrorText(R.string.error_empty_password);
        assertEquals(0, mainMonitor.getHits());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void assertLoginFailed(String username, String password) {
        launchLogin();
        setText(R.id.et_username, username);
        setText(R.id.et_password, password);
        clickLogin();
        checkErrorText(R.string.error_login_failed);
        assertEquals("Đăng nhập sai không được điều hướng", 0, mainMonitor.getHits());
    }

    /** Mở LoginActivity (đồng bộ, đợi tới khi activity resume xong). */
    private void launchLogin() {
        Intent intent = new Intent(instrumentation.getTargetContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        loginActivity = instrumentation.startActivitySync(intent);
    }

    private void setText(int editTextId, String value) {
        instrumentation.runOnMainSync(() -> {
            TextInputEditText editText = loginActivity.findViewById(editTextId);
            editText.setText(value);
        });
        instrumentation.waitForIdleSync();
    }

    private void clickLogin() {
        instrumentation.runOnMainSync(() -> {
            Button btnLogin = loginActivity.findViewById(R.id.btn_login);
            btnLogin.performClick();
        });
        instrumentation.waitForIdleSync();
    }

    /** Đọc text + visibility của tv_error trên UI thread. */
    private String readErrorText() {
        final AtomicReference<String> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            TextView error = loginActivity.findViewById(R.id.tv_error);
            if (error.getVisibility() == android.view.View.VISIBLE
                    || error.getVisibility() == android.view.View.INVISIBLE) {
                result.set(error.getText().toString());
            }
        });
        return result.get();
    }

    private void checkErrorText(int expectedResId) {
        String expected = instrumentation.getTargetContext().getString(expectedResId);
        assertEquals("Thông báo lỗi hiển thị không đúng", expected, readErrorText());
    }

    /** Chờ LoginActivity bị hủy (finish() đã gọi) trong thời hạn cho trước. */
    private boolean waitForLoginDestroyed(long timeoutMs) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (SystemClock.elapsedRealtime() < deadline) {
            if (loginActivity == null
                    || loginActivity.isDestroyed()
                    || loginActivity.isFinishing()) {
                return true;
            }
            SystemClock.sleep(100);
        }
        return false;
    }
}
