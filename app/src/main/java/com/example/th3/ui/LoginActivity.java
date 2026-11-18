package com.example.th3.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.th3.R;
import com.example.th3.database.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Màn hình đăng nhập — launcher activity của ứng dụng.
 * <p>
 * Luồng xử lý khi nhấn nút Đăng nhập:
 * <ol>
 *     <li>Kiểm tra dữ liệu trống (bỏ trống cả hai hoặc từng trường).</li>
 *     <li>Gọi {@link DatabaseHelper#checkLogin(String, String)}.</li>
 *     <li>Nếu đúng: Intent sang {@link MainActivity} và gọi {@code finish()}.</li>
 *     <li>Nếu sai: Hiển thị thông báo "Sai tên đăng nhập hoặc mật khẩu".</li>
 * </ol>
 * Tuyệt đối không hardcode tài khoản trong Activity, mọi xác thực qua SQLite Database.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextView tvError;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        tvError = findViewById(R.id.tv_error);
        Button btnLogin = findViewById(R.id.btn_login);

        databaseHelper = new DatabaseHelper(this);

        btnLogin.setOnClickListener(v -> attemptLogin());

        // Hỗ trợ nhấn phím Done trên bàn phím ảo để kích hoạt đăng nhập ngay
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        // Ẩn thông báo lỗi khi người dùng nhập/sửa lại nội dung
        TextWatcher clearErrorWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (tvError != null && tvError.getVisibility() == View.VISIBLE) {
                    tvError.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        etUsername.addTextChangedListener(clearErrorWatcher);
        etPassword.addTextChangedListener(clearErrorWatcher);
    }

    private void attemptLogin() {
        hideKeyboard();

        String username = readText(etUsername);
        String password = readText(etPassword);

        // 1. Kiểm tra dữ liệu trống
        if (username.isEmpty()) {
            showError(getString(R.string.error_empty_username));
            return;
        }
        if (password.isEmpty()) {
            showError(getString(R.string.error_empty_password));
            return;
        }

        // 2. Xác thực qua SQLite Database
        long userId = databaseHelper.checkLogin(username, password);

        // 3. Nếu đúng: Intent sang MainActivity và kết thúc LoginActivity
        if (userId > 0) {
            Intent intent = new Intent(this, MainActivity.class);
            // FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK đảm bảo
            // back stack bị xóa sạch: nhấn Back ở MainActivity sẽ thoát ứng dụng,
            // không quay lại màn hình đăng nhập.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // 4. Nếu sai: Hiển thị thông báo lỗi
        showError(getString(R.string.error_login_failed));
    }

    private String readText(TextInputEditText editText) {
        if (editText == null) {
            return "";
        }
        CharSequence text = editText.getText();
        return text == null ? "" : text.toString().trim();
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (databaseHelper != null) {
            databaseHelper.close();
        }
        super.onDestroy();
    }
}
