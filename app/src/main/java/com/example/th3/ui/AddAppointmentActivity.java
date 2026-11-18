package com.example.th3.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.th3.R;
import com.example.th3.database.DatabaseHelper;
import com.example.th3.model.Appointment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

/**
 * Màn hình thêm lịch hẹn mới – Phase 5.
 *
 * <p>Luồng:
 * <ol>
 *   <li>Người dùng điền tên, địa điểm, chọn ngày/giờ, bật/tắt nhắc nhở.</li>
 *   <li>Nhấn "Lưu lịch hẹn" → validate → ghi vào SQLite →
 *       Toast thành công → {@link #finish()} quay về
 *       {@link MainActivity} (onResume sẽ tự tải lại dữ liệu).</li>
 *   <li>Nếu validate sai hoặc ghi DB thất bại, giữ nguyên dữ liệu
 *       và hiển thị Snackbar thông báo lỗi.</li>
 * </ol>
 *
 * <p><b>Validation:</b>
 * <ul>
 *   <li>Tên lịch hẹn không được trống.</li>
 *   <li>Địa điểm không được trống.</li>
 *   <li>Ngày giờ được chọn phải <em>lớn hơn</em> thời điểm hiện tại
 *       (kiểm tra cả ngày lẫn giờ theo phút).</li>
 * </ul>
 *
 * <p><b>Định dạng lưu DB:</b> date = "yyyy-MM-dd", time = "HH:mm".
 */
public class AddAppointmentActivity extends AppCompatActivity {

    // ----------------------------------------------------------------
    // Views
    // ----------------------------------------------------------------
    private TextInputLayout   tilTitle;
    private TextInputLayout   tilLocation;
    private TextInputEditText etTitle;
    private TextInputEditText etLocation;
    private DatePicker        datePicker;
    private TimePicker        timePicker;
    private ImageView         ivReminderIcon;
    private SwitchMaterial    switchReminder;

    // ----------------------------------------------------------------
    // Data
    // ----------------------------------------------------------------
    private DatabaseHelper db;

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_appointment);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        db = new DatabaseHelper(this);

        bindViews();
        setupToolbar();
        setupSaveButton();
        initDateTimeToCurrent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null) {
            db.close();
        }
    }

    // ================================================================
    // Setup helpers
    // ================================================================

    /** Ánh xạ toàn bộ view từ layout. */
    private void bindViews() {
        tilTitle       = findViewById(R.id.til_title);
        tilLocation    = findViewById(R.id.til_location);
        etTitle        = findViewById(R.id.et_title);
        etLocation     = findViewById(R.id.et_location);
        datePicker     = findViewById(R.id.date_picker);
        timePicker     = findViewById(R.id.time_picker);
        ivReminderIcon = findViewById(R.id.iv_add_reminder_icon);
        switchReminder = findViewById(R.id.switch_reminder);

        if (switchReminder != null) {
            switchReminder.setOnCheckedChangeListener((v, isChecked) -> updateReminderIconState(isChecked));
        }
    }

    private void updateReminderIconState(boolean on) {
        if (ivReminderIcon == null) return;
        if (on) {
            ivReminderIcon.setBackgroundResource(R.drawable.bg_chip_reminder_on);
            ImageViewCompat.setImageTintList(ivReminderIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        } else {
            ivReminderIcon.setBackgroundResource(R.drawable.bg_chip_reminder_off);
            ImageViewCompat.setImageTintList(ivReminderIcon,
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_reminder_off_text)));
        }
    }

    /** Gắn MaterialToolbar làm ActionBar, bật nút Back. */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_add);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.add_title));
        }
    }

    /**
     * Khởi tạo DatePicker và TimePicker ở thời điểm hiện tại.
     * TimePicker dùng định dạng 24 giờ theo đúng thiết kế.
     */
    private void initDateTimeToCurrent() {
        Calendar now = Calendar.getInstance();
        datePicker.init(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH),
                null
        );
        timePicker.setIs24HourView(true);
        timePicker.setHour(now.get(Calendar.HOUR_OF_DAY));
        timePicker.setMinute(now.get(Calendar.MINUTE));
    }

    /** Gắn listener cho nút Lưu và bàn phím. */
    private void setupSaveButton() {
        ExtendedFloatingActionButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> attemptSave());

        // Hỗ trợ nhấn phím Done / Enter trên bàn phím ảo để lưu ngay
        etLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                attemptSave();
                return true;
            }
            return false;
        });

        etTitle.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                attemptSave();
                return true;
            }
            return false;
        });
    }

    // ================================================================
    // Menu (nút Back trên Toolbar)
    // ================================================================

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ================================================================
    // Validation & Save
    // ================================================================

    /**
     * Điểm vào khi người dùng nhấn "Lưu lịch hẹn".
     * Validate đầu vào; nếu hợp lệ thì ghi DB và quay về.
     */
    private void attemptSave() {
        // --- Xóa lỗi cũ ---
        tilTitle.setError(null);
        tilLocation.setError(null);

        String title    = getText(etTitle);
        String location = getText(etLocation);

        // 1. Kiểm tra tên
        if (TextUtils.isEmpty(title)) {
            tilTitle.setError(getString(R.string.error_title_empty));
            tilTitle.requestFocus();
            return;
        }

        // 2. Kiểm tra địa điểm
        if (TextUtils.isEmpty(location)) {
            tilLocation.setError(getString(R.string.error_location_empty));
            tilLocation.requestFocus();
            return;
        }

        // 3. Kiểm tra ngày giờ > hiện tại
        if (!isDateTimeFuture()) {
            showError(getString(R.string.error_datetime_past));
            return;
        }

        // --- Tất cả hợp lệ: lưu vào DB ---
        saveAppointment(title, location);
    }

    /**
     * Kiểm tra ngày giờ được chọn có lớn hơn thời điểm hiện tại không.
     * So sánh theo đơn vị phút để tránh race-condition giây.
     *
     * @return true nếu ngày giờ chọn > now (theo phút).
     */
    private boolean isDateTimeFuture() {
        // Thời điểm hiện tại, cắt giây và mili-giây
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        // Thời điểm được chọn
        Calendar selected = Calendar.getInstance();
        selected.set(Calendar.YEAR,         datePicker.getYear());
        selected.set(Calendar.MONTH,        datePicker.getMonth());
        selected.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
        selected.set(Calendar.HOUR_OF_DAY,  timePicker.getHour());
        selected.set(Calendar.MINUTE,       timePicker.getMinute());
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        // Phải NGHIÊM NGẶT lớn hơn (không cho phép bằng)
        return selected.after(now);
    }

    /**
     * Tạo {@link Appointment}, ghi vào SQLite và xử lý kết quả.
     * Nếu thành công: Snackbar xanh → finish().
     * Nếu thất bại: Snackbar đỏ, giữ nguyên form.
     */
    private void saveAppointment(@NonNull String title, @NonNull String location) {
        String dateStr = formatDate(
                datePicker.getYear(),
                datePicker.getMonth() + 1,   // getMonth() trả về 0-based
                datePicker.getDayOfMonth()
        );
        String timeStr = formatTime(
                timePicker.getHour(),
                timePicker.getMinute()
        );
        boolean reminder = switchReminder.isChecked();

        Appointment appointment = new Appointment(title, location, dateStr, timeStr, reminder);
        long rowId = db.insertAppointment(appointment);

        if (rowId != -1) {
            // Thành công
            Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.save_success),
                    Snackbar.LENGTH_SHORT
            ).show();
            // Delay nhỏ để Snackbar hiện trước khi đóng màn hình
            findViewById(android.R.id.content).postDelayed(this::finish, 800);
        } else {
            // Thất bại: giữ dữ liệu, thông báo lỗi
            showError(getString(R.string.save_failed));
        }
    }

    // ================================================================
    // Format helpers
    // ================================================================

    /**
     * Định dạng ngày thành "yyyy-MM-dd" để lưu vào DB.
     *
     * @param year  năm 4 chữ số.
     * @param month tháng 1-based (đã cộng 1 từ DatePicker.getMonth()).
     * @param day   ngày trong tháng.
     */
    @NonNull
    private String formatDate(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    /**
     * Định dạng giờ thành "HH:mm" để lưu vào DB.
     *
     * @param hour   giờ 0-23.
     * @param minute phút 0-59.
     */
    @NonNull
    private String formatTime(int hour, int minute) {
        return String.format(Locale.US, "%02d:%02d", hour, minute);
    }

    // ================================================================
    // UI helpers
    // ================================================================

    /**
     * Lấy text từ EditText, trim khoảng trắng, trả về rỗng nếu null.
     */
    @NonNull
    private String getText(@NonNull TextInputEditText editText) {
        CharSequence text = editText.getText();
        return text == null ? "" : text.toString().trim();
    }

    /**
     * Hiển thị Snackbar lỗi trên root view.
     *
     * @param message nội dung thông báo lỗi.
     */
    private void showError(@NonNull String message) {
        Snackbar.make(
                findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }
}
