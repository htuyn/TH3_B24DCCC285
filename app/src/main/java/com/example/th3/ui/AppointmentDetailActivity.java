package com.example.th3.ui;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Màn hình chi tiết lịch hẹn – Phase 6.
 *
 * <p>Chức năng:
 * <ul>
 *   <li>Nhận {@code appointment_id} từ Intent extra, truy vấn SQLite
 *       bằng {@link DatabaseHelper#getAppointmentById(int)}.
 *       Không truyền toàn bộ đối tượng qua Intent.</li>
 *   <li>Hiển thị: tên, địa điểm, ngày giờ, trạng thái nhắc nhở.</li>
 *   <li>Switch nhắc nhở: gọi {@link DatabaseHelper#updateReminder(int, boolean)};
 *       nếu thất bại khôi phục trạng thái cũ.</li>
 *   <li>Nút Xóa: hiển thị AlertDialog xác nhận,
 *       gọi {@link DatabaseHelper#deleteAppointment(int)},
 *       nếu thành công {@link #finish()} → {@code MainActivity.onResume()} tự reload.</li>
 *   <li>Xử lý an toàn khi ID không hợp lệ hoặc không tìm thấy: không crash.</li>
 * </ul>
 */
public class AppointmentDetailActivity extends AppCompatActivity {

    /** Key Intent extra nhận appointment ID từ {@link MainActivity}. */
    public static final String EXTRA_APPOINTMENT_ID = "appointment_id";

    /** Giá trị sentinel khi Intent không chứa ID hợp lệ. */
    private static final int INVALID_ID = -1;

    // ----------------------------------------------------------------
    // Định dạng ngày
    // ----------------------------------------------------------------
    private static final String DB_DATE_PATTERN      = "yyyy-MM-dd";
    private static final String DISPLAY_DATE_PATTERN = "dd/MM/yyyy";
    private static final String DATE_TIME_SEPARATOR  = " · ";

    // ----------------------------------------------------------------
    // Views
    // ----------------------------------------------------------------
    private TextView      tvTitle;
    private TextView      tvLocation;
    private TextView      tvDatetime;
    private TextView      tvReminderStatus;
    private ImageView     ivReminderIcon;
    private SwitchMaterial switchReminder;
    private MaterialButton btnBack;
    private MaterialButton btnDelete;

    // ----------------------------------------------------------------
    // Data
    // ----------------------------------------------------------------
    private DatabaseHelper db;
    private int            appointmentId = INVALID_ID;

    /**
     * Flag ngăn listener của Switch kích hoạt DB update
     * trong lúc đang khởi tạo giao diện từ dữ liệu DB.
     */
    private boolean isInitializingSwitch = false;

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        db = new DatabaseHelper(this);

        bindViews();
        setupToolbar();
        setupButtons();
        loadAndDisplay();
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

    private void bindViews() {
        tvTitle          = findViewById(R.id.tv_detail_title);
        tvLocation       = findViewById(R.id.tv_detail_location);
        tvDatetime       = findViewById(R.id.tv_detail_datetime);
        tvReminderStatus = findViewById(R.id.tv_reminder_status);
        ivReminderIcon   = findViewById(R.id.iv_detail_reminder_icon);
        switchReminder   = findViewById(R.id.switch_detail_reminder);
        btnBack          = findViewById(R.id.btn_back);
        btnDelete        = findViewById(R.id.btn_delete);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    // ================================================================
    // Load & Display
    // ================================================================

    /**
     * Đọc ID từ Intent, truy vấn DB, hiển thị dữ liệu hoặc xử lý lỗi.
     */
    private void loadAndDisplay() {
        // Lấy ID từ Intent
        appointmentId = getIntent().getIntExtra(EXTRA_APPOINTMENT_ID, INVALID_ID);

        if (appointmentId == INVALID_ID) {
            showErrorAndDisableActions(getString(R.string.detail_invalid_id));
            return;
        }

        Appointment appointment = db.getAppointmentById(appointmentId);
        if (appointment == null) {
            showErrorAndDisableActions(getString(R.string.detail_not_found));
            return;
        }

        displayAppointment(appointment);
        setupSwitchListener();
    }

    /**
     * Điền dữ liệu lịch hẹn lên các view.
     */
    private void displayAppointment(@NonNull Appointment appointment) {
        tvTitle.setText(appointment.getTitle());
        tvLocation.setText(appointment.getLocation());
        tvDatetime.setText(formatDateTime(appointment.getDate(), appointment.getTime()));

        // Khởi tạo Switch mà không kích hoạt listener (dùng flag)
        isInitializingSwitch = true;
        switchReminder.setChecked(appointment.isReminder());
        isInitializingSwitch = false;

        updateReminderStatusText(appointment.isReminder());
    }

    /**
     * Cập nhật text trạng thái "Đang bật" / "Đang tắt" và màu sắc/icon.
     */
    private void updateReminderStatusText(boolean on) {
        if (on) {
            tvReminderStatus.setText(getString(R.string.detail_reminder_on));
            tvReminderStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.success));
            if (ivReminderIcon != null) {
                ivReminderIcon.setBackgroundResource(R.drawable.bg_chip_reminder_on);
                ImageViewCompat.setImageTintList(ivReminderIcon,
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            }
        } else {
            tvReminderStatus.setText(getString(R.string.detail_reminder_off));
            tvReminderStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.text_secondary));
            if (ivReminderIcon != null) {
                ivReminderIcon.setBackgroundResource(R.drawable.bg_chip_reminder_off);
                ImageViewCompat.setImageTintList(ivReminderIcon,
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chip_reminder_off_text)));
            }
        }
    }

    /**
     * Hiển thị lỗi lên tiêu đề, vô hiệu hoá Switch và nút Xóa
     * khi không tìm thấy dữ liệu.
     */
    private void showErrorAndDisableActions(@NonNull String message) {
        tvTitle.setText(message);
        tvLocation.setText("");
        tvDatetime.setText("");
        tvReminderStatus.setText("");

        switchReminder.setEnabled(false);
        btnDelete.setEnabled(false);

        Snackbar.make(
                findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // ================================================================
    // Reminder Switch
    // ================================================================

    /**
     * Gắn listener cho Switch sau khi đã khởi tạo xong,
     * tránh gọi DB ngoài ý muốn trong lúc {@code setChecked()}.
     */
    private void setupSwitchListener() {
        switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Bỏ qua sự kiện do khởi tạo
            if (isInitializingSwitch) return;
            handleReminderToggle(isChecked);
        });
    }

    /**
     * Xử lý khi người dùng bật/tắt Switch nhắc nhở.
     *
     * <ol>
     *   <li>Gọi {@link DatabaseHelper#updateReminder(int, boolean)}.</li>
     *   <li>Nếu thành công: cập nhật text trạng thái.</li>
     *   <li>Nếu thất bại: khôi phục trạng thái Switch cũ,
     *       hiện Snackbar thông báo lỗi.</li>
     * </ol>
     */
    private void handleReminderToggle(boolean newState) {
        boolean success = db.updateReminder(appointmentId, newState);

        if (success) {
            updateReminderStatusText(newState);
        } else {
            // Khôi phục trạng thái cũ mà không kích hoạt lại listener
            isInitializingSwitch = true;
            switchReminder.setChecked(!newState);
            isInitializingSwitch = false;

            updateReminderStatusText(!newState);
            showSnackbar(getString(R.string.detail_reminder_update_failed));
        }
    }

    // ================================================================
    // Delete
    // ================================================================

    /**
     * Hiển thị AlertDialog xác nhận trước khi xóa.
     * Nếu người dùng hủy → không làm gì.
     * Nếu xác nhận → gọi {@link #performDelete()}.
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.detail_delete_title))
                .setMessage(getString(R.string.detail_delete_message))
                .setPositiveButton(getString(R.string.detail_delete_confirm),
                        (DialogInterface dialog, int which) -> performDelete())
                .setNegativeButton(getString(R.string.detail_delete_cancel),
                        (DialogInterface dialog, int which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    /**
     * Thực hiện xóa lịch hẹn khỏi DB.
     * Nếu thành công: Snackbar → {@link #finish()} (MainActivity.onResume tự reload).
     * Nếu thất bại: giữ màn hình, hiện Snackbar lỗi.
     */
    private void performDelete() {
        boolean success = db.deleteAppointment(appointmentId);

        if (success) {
            Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.detail_delete_success),
                    Snackbar.LENGTH_SHORT
            ).show();
            // Delay nhỏ để Snackbar hiển thị trước khi đóng màn hình
            findViewById(android.R.id.content).postDelayed(this::finish, 800);
        } else {
            showSnackbar(getString(R.string.detail_delete_failed));
        }
    }

    // ================================================================
    // Format helpers
    // ================================================================

    /**
     * Chuyển đổi date (yyyy-MM-dd) + time (HH:mm) thành
     * "dd/MM/yyyy · HH:mm" để hiển thị.
     * Nếu định dạng lỗi trả về chuỗi gốc.
     */
    @NonNull
    private String formatDateTime(@Nullable String date, @Nullable String time) {
        String displayDate;
        try {
            SimpleDateFormat dbFmt  = new SimpleDateFormat(DB_DATE_PATTERN, Locale.US);
            SimpleDateFormat uiFmt  = new SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US);
            displayDate = uiFmt.format(dbFmt.parse(date != null ? date : ""));
        } catch (ParseException | NullPointerException e) {
            displayDate = date != null ? date : "";
        }
        return displayDate + DATE_TIME_SEPARATOR + (time != null ? time : "");
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
    // UI helpers
    // ================================================================

    private void showSnackbar(@NonNull String message) {
        Snackbar.make(
                findViewById(android.R.id.content),
                message,
                Snackbar.LENGTH_LONG
        ).show();
    }
}
