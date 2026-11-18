package com.example.th3.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.th3.R;
import com.example.th3.model.Appointment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter cho danh sách lịch hẹn trên dashboard.
 * <p>
 * Mỗi item hiển thị: tên lịch hẹn, địa điểm, ngày giờ
 * (định dạng dd/MM/yyyy · HH:mm) và trạng thái nhắc hẹn
 * (Có nhắc / Không nhắc).
 */
public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    /** Định dạng ngày/giờ đang lưu trong database. */
    private static final String DB_DATE_PATTERN = "yyyy-MM-dd";
    /** Định dạng ngày hiển thị theo yêu cầu. */
    private static final String DISPLAY_DATE_PATTERN = "dd/MM/yyyy";
    /** Dấu phân cách giữa ngày và giờ. */
    private static final String DATE_TIME_SEPARATOR = " · ";

    /** Callback khi người dùng nhấn vào một item lịch hẹn. */
    public interface OnAppointmentClickListener {
        void onAppointmentClick(@NonNull Appointment appointment);
    }

    private final List<Appointment> appointments = new ArrayList<>();
    private final OnAppointmentClickListener listener;

    public AppointmentAdapter(@NonNull OnAppointmentClickListener listener) {
        this.listener = listener;
    }

    /**
     * Thay toàn bộ dữ liệu danh sách.
     * Dữ liệu lấy trực tiếp từ SQLite theo ORDER BY date ASC, time ASC.
     */
    public void submitList(@NonNull List<Appointment> newList) {
        appointments.clear();
        appointments.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.bind(appointments.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvLocation;
        private final TextView tvDatetime;
        private final TextView tvStatus;

        // SimpleDateFormat chạy trên main thread nên có thể dùng lại an toàn
        private final SimpleDateFormat dbDateFormat =
                new SimpleDateFormat(DB_DATE_PATTERN, Locale.US);
        private final SimpleDateFormat displayDateFormat =
                new SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US);

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvLocation = itemView.findViewById(R.id.tv_item_location);
            tvDatetime = itemView.findViewById(R.id.tv_item_datetime);
            tvStatus = itemView.findViewById(R.id.tv_item_status);
        }

        void bind(@NonNull final Appointment appointment,
                  @NonNull final OnAppointmentClickListener listener) {
            tvTitle.setText(appointment.getTitle());
            tvLocation.setText(appointment.getLocation());
            tvDatetime.setText(formatDateTime(appointment.getDate(), appointment.getTime()));

            if (appointment.isReminder()) {
                tvStatus.setText(R.string.status_reminder_on);
                tvStatus.setBackgroundResource(R.drawable.bg_chip_reminder_on);
                tvStatus.setTextColor(ContextCompat.getColor(
                        tvStatus.getContext(), R.color.primary_dark));
            } else {
                tvStatus.setText(R.string.status_reminder_off);
                tvStatus.setBackgroundResource(R.drawable.bg_chip_reminder_off);
                tvStatus.setTextColor(ContextCompat.getColor(
                        tvStatus.getContext(), R.color.text_secondary));
            }

            itemView.setOnClickListener(v -> listener.onAppointmentClick(appointment));
        }

        /**
         * Ghép ngày (yyyy-MM-dd -> dd/MM/yyyy) và giờ (HH:mm)
         * thành chuỗi "dd/MM/yyyy · HH:mm".
         * Nếu ngày sai định dạng, hiển thị nguyên chuỗi gốc.
         */
        private String formatDateTime(String date, String time) {
            String displayDate;
            try {
                displayDate = displayDateFormat.format(
                        dbDateFormat.parse(date));
            } catch (ParseException | NullPointerException e) {
                displayDate = date;
            }
            return displayDate + DATE_TIME_SEPARATOR + time;
        }
    }
}
