package com.example.th3.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.th3.R;
import com.example.th3.database.DatabaseHelper;
import com.example.th3.model.Appointment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình chính – Dashboard danh sách lịch hẹn.
 *
 * <p>Chức năng Phase 4:
 * <ul>
 *   <li>Hiển thị toolbar và tiêu đề "Lịch hẹn của tôi".</li>
 *   <li>Thống kê tổng số lịch hẹn ({@link DatabaseHelper#countAll()})
 *       và số đang bật nhắc ({@link DatabaseHelper#countReminders()}).</li>
 *   <li>Danh sách lịch hẹn từ SQLite, sắp xếp date ASC, time ASC.</li>
 *   <li>Empty-state khi không có dữ liệu.</li>
 *   <li>FAB mở {@link AddAppointmentActivity}.</li>
 *   <li>Click item mở {@link AppointmentDetailActivity} kèm appointment ID.</li>
 *   <li>Dữ liệu được tải lại trong {@link #onResume()} để phản ánh thay đổi
 *       sau khi quay từ màn hình khác.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    // ----------------------------------------------------------------
    // Views
    // ----------------------------------------------------------------
    private TextView         tvStatTotal;
    private TextView         tvStatReminders;
    private MaterialCardView cardStatTotal;
    private MaterialCardView cardStatReminders;
    private RecyclerView     recyclerAppointments;
    private View             emptyState;

    // ----------------------------------------------------------------
    // Data
    // ----------------------------------------------------------------
    private DatabaseHelper     db;
    private AppointmentAdapter adapter;
    private boolean            isFilterReminderOnly = false;

    // ================================================================
    // Lifecycle
    // ================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Xử lý System Window Insets để tránh thanh Status Bar và Camera Punch Hole che Toolbar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        db = new DatabaseHelper(this);

        bindViews();
        setupStatCardFilter();
        setupRecyclerView();
        setupFab();
        // Dữ liệu được tải lần đầu trong onResume()
    }

    /**
     * Tải lại dữ liệu mỗi khi MainActivity trở thành foreground.
     * Điều này đảm bảo danh sách và thống kê luôn đồng bộ sau khi
     * người dùng quay lại từ AddAppointmentActivity hoặc
     * AppointmentDetailActivity.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadData();
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
        tvStatTotal          = findViewById(R.id.tv_stat_total);
        tvStatReminders      = findViewById(R.id.tv_stat_reminders);
        cardStatTotal        = findViewById(R.id.card_stat_total);
        cardStatReminders    = findViewById(R.id.card_stat_reminders);
        recyclerAppointments = findViewById(R.id.recycler_appointments);
        emptyState           = findViewById(R.id.empty_state);
    }

    /** Thiết lập sự kiện click lọc cho hai card thống kê. */
    private void setupStatCardFilter() {
        if (cardStatTotal != null) {
            cardStatTotal.setOnClickListener(v -> {
                if (isFilterReminderOnly) {
                    isFilterReminderOnly = false;
                    loadData();
                }
            });
        }
        if (cardStatReminders != null) {
            cardStatReminders.setOnClickListener(v -> {
                isFilterReminderOnly = !isFilterReminderOnly;
                loadData();
            });
        }
    }

    /** Khởi tạo RecyclerView và Adapter. */
    private void setupRecyclerView() {
        adapter = new AppointmentAdapter(this::onAppointmentClick);
        recyclerAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerAppointments.setAdapter(adapter);
        // Tối ưu: kích thước RecyclerView không đổi theo nội dung
        recyclerAppointments.setHasFixedSize(false);
    }

    /** FAB mở AddAppointmentActivity. */
    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddAppointmentActivity.class);
            startActivity(intent);
        });
    }

    // ================================================================
    // Data loading
    // ================================================================

    /**
     * Truy vấn SQLite, cập nhật danh sách và thống kê.
     * Gọi trong {@link #onResume()} để luôn hiển thị dữ liệu mới nhất.
     */
    private void loadData() {
        List<Appointment> allAppointments = db.getAllAppointments();
        long total     = db.countAll();
        long reminders = db.countReminders();

        // Cập nhật thống kê
        tvStatTotal.setText(String.valueOf(total));
        tvStatReminders.setText(String.valueOf(reminders));

        // Lọc danh sách nếu chế độ lọc đang bật
        List<Appointment> displayList;
        if (isFilterReminderOnly) {
            displayList = new ArrayList<>();
            for (Appointment appt : allAppointments) {
                if (appt.isReminder()) {
                    displayList.add(appt);
                }
            }
        } else {
            displayList = allAppointments;
        }

        updateCardHighlight();

        // Cập nhật danh sách
        adapter.submitList(displayList);

        // Hiển thị empty-state nếu không có lịch hẹn
        if (displayList.isEmpty()) {
            recyclerAppointments.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerAppointments.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    /** Highlight card đang được chọn làm bộ lọc. */
    private void updateCardHighlight() {
        int activeStrokeWidth = (int) (2.5f * getResources().getDisplayMetrics().density);
        int normalStrokeWidth = (int) (1.5f * getResources().getDisplayMetrics().density);

        if (cardStatReminders != null) {
            if (isFilterReminderOnly) {
                cardStatReminders.setStrokeWidth(activeStrokeWidth);
                cardStatReminders.setStrokeColor(ContextCompat.getColor(this, R.color.primary));
                cardStatReminders.setCardElevation((int) (4f * getResources().getDisplayMetrics().density));
            } else {
                cardStatReminders.setStrokeWidth(normalStrokeWidth);
                cardStatReminders.setStrokeColor(ContextCompat.getColor(this, R.color.stat_reminder_stroke));
                cardStatReminders.setCardElevation((int) (1f * getResources().getDisplayMetrics().density));
            }
        }

        if (cardStatTotal != null) {
            cardStatTotal.setStrokeWidth(normalStrokeWidth);
            cardStatTotal.setStrokeColor(ContextCompat.getColor(this, R.color.stat_total_stroke));
            cardStatTotal.setCardElevation((int) (1f * getResources().getDisplayMetrics().density));
        }
    }

    // ================================================================
    // Navigation
    // ================================================================

    /**
     * Xử lý click item danh sách: mở AppointmentDetailActivity và
     * truyền appointment ID qua Intent extra.
     *
     * @param appointment lịch hẹn được nhấn (không null).
     */
    private void onAppointmentClick(@NonNull Appointment appointment) {
        Intent intent = new Intent(this, AppointmentDetailActivity.class);
        intent.putExtra(AppointmentDetailActivity.EXTRA_APPOINTMENT_ID,
                appointment.getId());
        startActivity(intent);
    }
}
