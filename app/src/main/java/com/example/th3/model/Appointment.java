package com.example.th3.model;

import androidx.annotation.NonNull;

/**
 * Model của bảng {@code appointments}.
 * <p>
 * Quy ước dữ liệu:
 * <ul>
 *     <li>{@code id}: ID tự tăng của lịch hẹn trong SQLite (int)</li>
 *     <li>{@code title}: tiêu đề lịch hẹn (String)</li>
 *     <li>{@code location}: địa điểm (String)</li>
 *     <li>{@code date}: định dạng yyyy-MM-dd (String)</li>
 *     <li>{@code time}: định dạng HH:mm (String)</li>
 *     <li>{@code reminder}: true = bật nhắc hẹn, false = tắt (boolean)
 *         (lưu trong DB là INTEGER: 1/0)</li>
 * </ul>
 */
public class Appointment {

    private int id;
    private String title;
    private String location;
    private String date;
    private String time;
    private boolean reminder;

    /**
     * Constructor mặc định không tham số.
     */
    public Appointment() {
        this(-1, "", "", "", "", false);
    }

    /**
     * Dùng khi chưa có id (row chưa được INSERT vào database).
     */
    public Appointment(String title, String location, String date, String time, boolean reminder) {
        this(-1, title, location, date, time, reminder);
    }

    /**
     * Constructor đầy đủ các thuộc tính.
     */
    public Appointment(int id, String title, String location, String date, String time, boolean reminder) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.date = date;
        this.time = time;
        this.reminder = reminder;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    /** Trạng thái nhắc hẹn: true = bật, false = tắt. */
    public boolean isReminder() {
        return reminder;
    }

    public boolean getReminder() {
        return reminder;
    }

    public void setReminder(boolean reminder) {
        this.reminder = reminder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        if (id != that.id) return false;
        if (reminder != that.reminder) return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        return time != null ? time.equals(that.time) : that.time == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + (reminder ? 1 : 0);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "Appointment{id=" + id
                + ", title='" + title + '\''
                + ", location='" + location + '\''
                + ", date='" + date + '\''
                + ", time='" + time + '\''
                + ", reminder=" + reminder + '}';
    }
}
