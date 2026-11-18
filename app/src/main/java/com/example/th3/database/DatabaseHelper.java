package com.example.th3.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.th3.model.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Lớp quản lý database SQLite {@code appointments.db} (version 1).
 * <p>
 * Gồm 2 bảng:
 * <ul>
 *     <li>{@code users}: tài khoản đăng nhập.</li>
 *     <li>{@code appointments}: danh sách lịch hẹn.</li>
 * </ul>
 * Mọi truy vấn đều dùng tham số hóa (selection args / bind args),
 * tuyệt đối không nối chuỗi dữ liệu người dùng vào SQL.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    public static final String DB_NAME = "appointments.db";
    public static final int DB_VERSION = 1;

    // ------------------------------------------------------------------
    // Bảng users
    // ------------------------------------------------------------------
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // ------------------------------------------------------------------
    // Bảng appointments
    // ------------------------------------------------------------------
    public static final String TABLE_APPOINTMENTS = "appointments";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_REMINDER = "reminder";

    // Định dạng dữ liệu chuẩn hóa
    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_PATTERN = Pattern.compile("\\d{2}:\\d{2}");

    // Tài khoản mẫu
    public static final String SEED_USERNAME = "admin";
    public static final String SEED_PASSWORD = "123456";

    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + TABLE_USERS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_USERNAME + " TEXT NOT NULL UNIQUE, "
                    + COLUMN_PASSWORD + " TEXT NOT NULL)";

    private static final String SQL_CREATE_APPOINTMENTS =
            "CREATE TABLE " + TABLE_APPOINTMENTS + " ("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TITLE + " TEXT NOT NULL, "
                    + COLUMN_LOCATION + " TEXT NOT NULL, "
                    + COLUMN_DATE + " TEXT NOT NULL, "
                    + COLUMN_TIME + " TEXT NOT NULL, "
                    + COLUMN_REMINDER + " INTEGER NOT NULL)";

    private static final String[] APPOINTMENT_COLUMNS = {
            COLUMN_ID, COLUMN_TITLE, COLUMN_LOCATION,
            COLUMN_DATE, COLUMN_TIME, COLUMN_REMINDER
    };

    /** Thứ tự sắp xếp chuẩn: ngày tăng dần, rồi giờ tăng dần. */
    private static final String ORDER_BY_DATE_TIME =
            COLUMN_DATE + " ASC, " + COLUMN_TIME + " ASC";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ==================================================================
    // Khởi tạo database
    // ==================================================================

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(SQL_CREATE_USERS);
            db.execSQL(SQL_CREATE_APPOINTMENTS);
            seedData(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1: chưa có kịch bản nâng cấp.
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            onCreate(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Chèn dữ liệu mẫu: tài khoản admin/123456 và 3 lịch hẹn ban đầu.
     */
    private void seedData(SQLiteDatabase db) {
        long userRowId = insertUser(db, SEED_USERNAME, SEED_PASSWORD);
        if (userRowId == -1) {
            Log.e(TAG, "Failed to seed default user account");
        }

        insertAppointment(db, new Appointment("Họp nhóm đồ án", "Phòng B203",
                "2026-10-25", "09:00", true));
        insertAppointment(db, new Appointment("Khám răng", "Nha khoa Minh Tâm",
                "2026-10-26", "14:30", true));
        insertAppointment(db, new Appointment("Đi cà phê với bạn", "Highland Coffee",
                "2026-10-27", "19:00", false));
    }

    // ==================================================================
    // users
    // ==================================================================

    /**
     * Kiểm tra đăng nhập.
     *
     * @return id của user nếu khớp username/password, ngược lại -1.
     */
    public long checkLogin(@Nullable String username, @Nullable String password) {
        if (isEmpty(username) || isEmpty(password)) {
            return -1L;
        }

        Cursor cursor = null;
        try (SQLiteDatabase db = getReadableDatabase()) {
            String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
            cursor = db.query(TABLE_USERS, new String[]{COLUMN_ID}, selection,
                    new String[]{username, password}, null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            }
            return -1L;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Đăng ký user mới.
     *
     * @return row id nếu thành công, -1 nếu thất bại hoặc username đã tồn tại.
     */
    public long insertUser(@Nullable String username, @Nullable String password) {
        if (isEmpty(username) || isEmpty(password)) {
            return -1L;
        }
        SQLiteDatabase db = getWritableDatabase();
        return insertUser(db, username, password);
    }

    private long insertUser(@NonNull SQLiteDatabase db, @Nullable String username, @Nullable String password) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        // insert() tự bắt SQLException (vd. vi phạm UNIQUE) và trả về -1.
        return db.insert(TABLE_USERS, null, values);
    }

    // ==================================================================
    // appointments
    // ==================================================================

    /**
     * Thêm lịch hẹn mới. Input được kiểm tra null/rỗng và đúng định dạng
     * date (yyyy-MM-dd), time (HH:mm) trước khi ghi.
     *
     * @return row id của bản ghi nếu thành công, -1 nếu thất bại.
     */
    public long insertAppointment(@Nullable Appointment appointment) {
        if (!isValidAppointment(appointment)) {
            Log.w(TAG, "insertAppointment: invalid appointment " + appointment);
            return -1L;
        }
        SQLiteDatabase db = getWritableDatabase();
        return insertAppointment(db, appointment);
    }

    private long insertAppointment(@NonNull SQLiteDatabase db, @NonNull Appointment appointment) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, appointment.getTitle());
        values.put(COLUMN_LOCATION, appointment.getLocation());
        values.put(COLUMN_DATE, appointment.getDate());
        values.put(COLUMN_TIME, appointment.getTime());
        // reminder lưu dạng INTEGER: 1 = bật, 0 = tắt
        values.put(COLUMN_REMINDER, appointment.isReminder() ? 1 : 0);
        return db.insert(TABLE_APPOINTMENTS, null, values);
    }

    /**
     * Danh sách toàn bộ lịch hẹn, sắp xếp theo date ASC, time ASC.
     */
    @NonNull
    public List<Appointment> getAllAppointments() {
        List<Appointment> result = new ArrayList<>();
        Cursor cursor = null;
        try (SQLiteDatabase db = getReadableDatabase()) {
            cursor = db.query(TABLE_APPOINTMENTS, APPOINTMENT_COLUMNS,
                    null, null, null, null, ORDER_BY_DATE_TIME);
            while (cursor.moveToNext()) {
                result.add(readAppointment(cursor));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /**
     * Lấy một lịch hẹn theo id.
     *
     * @return {@link Appointment} hoặc null nếu không tìm thấy.
     */
    @Nullable
    public Appointment getAppointmentById(int id) {
        Cursor cursor = null;
        try (SQLiteDatabase db = getReadableDatabase()) {
            cursor = db.query(TABLE_APPOINTMENTS, APPOINTMENT_COLUMNS,
                    COLUMN_ID + " = ?", new String[]{String.valueOf(id)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return readAppointment(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Bật/tắt nhắc hẹn của một lịch hẹn.
     *
     * @return true nếu có bản ghi được cập nhật thành công.
     */
    public boolean updateReminder(int id, boolean on) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER, on ? 1 : 0);

        try (SQLiteDatabase db = getWritableDatabase()) {
            int rows = db.update(TABLE_APPOINTMENTS, values,
                    COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            return rows > 0;
        }
    }

    /**
     * Xóa một lịch hẹn theo id.
     *
     * @return true nếu có bản ghi bị xóa.
     */
    public boolean deleteAppointment(int id) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            int rows = db.delete(TABLE_APPOINTMENTS,
                    COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            return rows > 0;
        }
    }

    /**
     * Tổng số lịch hẹn hiện có.
     */
    public long countAll() {
        return count(null);
    }

    /**
     * Số lịch hẹn đang bật nhắc hẹn (reminder = 1).
     */
    public long countReminders() {
        // reminder = 1 ứng với trạng thái bật nhắc hẹn
        return count(COLUMN_REMINDER + " = 1");
    }

    private long count(@Nullable String selection, String... selectionArgs) {
        Cursor cursor = null;
        try (SQLiteDatabase db = getReadableDatabase()) {
            cursor = db.query(TABLE_APPOINTMENTS,
                    new String[]{"COUNT(" + COLUMN_ID + ") AS count"},
                    selection, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return 0L;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // ==================================================================
    // Helper nội bộ
    // ==================================================================

    /** Đọc bản ghi hiện hành của cursor thành {@link Appointment}, xử lý cột null. */
    private Appointment readAppointment(@NonNull Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        String title = safeString(cursor, COLUMN_TITLE);
        String location = safeString(cursor, COLUMN_LOCATION);
        String date = safeString(cursor, COLUMN_DATE);
        String time = safeString(cursor, COLUMN_TIME);
        boolean reminder = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER)) == 1;
        return new Appointment((int) id, title, location, date, time, reminder);
    }

    private String safeString(@NonNull Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? "" : cursor.getString(index);
    }

    private boolean isValidAppointment(@Nullable Appointment appointment) {
        return appointment != null
                && !isEmpty(appointment.getTitle())
                && !isEmpty(appointment.getLocation())
                && isDate(appointment.getDate())
                && isTime(appointment.getTime());
    }

    private boolean isDate(@Nullable String value) {
        return !isEmpty(value) && DATE_PATTERN.matcher(value).matches();
    }

    private boolean isTime(@Nullable String value) {
        return !isEmpty(value) && TIME_PATTERN.matcher(value).matches();
    }

    private boolean isEmpty(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }
}
