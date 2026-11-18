package com.example.th3.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.th3.model.Appointment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Kiểm thử DatabaseHelper trên thiết bị thật/emulator:
 * đủ 9 phương thức, 2 bảng, dữ liệu mẫu và tính bền vững dữ liệu
 * sau khi đóng rồi mở lại database.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DatabaseHelper.DB_NAME);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DatabaseHelper.DB_NAME);
    }

    // ------------------------------------------------------------------
    // Schema: 2 bảng + dữ liệu mẫu
    // ------------------------------------------------------------------

    @Test
    public void onCreate_tạo2Bảng_vàSeedDữLiệuMẫu() {
        DatabaseHelper helper = new DatabaseHelper(context);
        helper.getReadableDatabase();

        long adminId = helper.checkLogin(DatabaseHelper.SEED_USERNAME, DatabaseHelper.SEED_PASSWORD);
        assertTrue("Tài khoản admin/123456 phải đăng nhập được", adminId > 0);

        List<Appointment> all = helper.getAllAppointments();
        assertEquals("Phải seed 3 lịch hẹn mẫu", 3, all.size());

        Appointment first = all.get(0);
        assertEquals("Họp nhóm đồ án", first.getTitle());
        assertEquals("Phòng B203", first.getLocation());
        assertEquals("2026-10-25", first.getDate());
        assertEquals("09:00", first.getTime());
        assertTrue(first.isReminder());
        Appointment third = all.get(2);
        assertEquals("Đi cà phê với bạn", third.getTitle());
        assertEquals("Highland Coffee", third.getLocation());
        assertEquals("2026-10-27", third.getDate());
        assertEquals("19:00", third.getTime());
        assertFalse(third.isReminder());

        helper.close();
    }

    // ------------------------------------------------------------------
    // 1. checkLogin
    // ------------------------------------------------------------------

    @Test
    public void checkLogin_đúngMậtKhẩu_trảVềUserId() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertTrue(helper.checkLogin("admin", "123456") > 0);
        helper.close();
    }

    @Test
    public void checkLogin_saiMậtKhẩu_trảVềMinus1() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertEquals(-1L, helper.checkLogin("admin", "sai-mat-khau"));
        helper.close();
    }

    @Test
    public void checkLogin_inputNullHoặcRỗng_trảVềMinus1() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertEquals(-1L, helper.checkLogin(null, "123456"));
        assertEquals(-1L, helper.checkLogin("admin", null));
        assertEquals(-1L, helper.checkLogin("admin", " "));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 2. insertUser
    // ------------------------------------------------------------------

    @Test
    public void insertUser_mới_trảVềRowIdVàĐăngNhậpĐược() {
        DatabaseHelper helper = new DatabaseHelper(context);
        long rowId = helper.insertUser("student01", "abc123");
        assertTrue(rowId > 0);
        assertTrue(helper.checkLogin("student01", "abc123") > 0);
        helper.close();
    }

    @Test
    public void insertUser_trùngUsername_hoặcInputKhôngHợpLệ_trảVềMinus1() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertTrue(helper.insertUser("student02", "abc123") > 0);
        assertEquals("Username UNIQUE phải chặn trùng lặp", -1L, helper.insertUser("student02", "khac"));
        assertEquals(-1L, helper.insertUser(null, "abc123"));
        assertEquals(-1L, helper.insertUser("student03", null));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 3. insertAppointment
    // ------------------------------------------------------------------

    @Test
    public void insertAppointment_hợpLệ_trảVềRowId() {
        DatabaseHelper helper = new DatabaseHelper(context);
        Appointment a = new Appointment("Rửa xe", "Gara Nguyễn",
                "2026-11-20", "17:30", true);
        long rowId = helper.insertAppointment(a);
        assertTrue(rowId > 0);

        Appointment saved = helper.getAppointmentById((int) rowId);
        assertNotNull(saved);
        assertEquals((int) rowId, saved.getId());
        assertEquals("Rửa xe", saved.getTitle());

        helper.close();
    }

    @Test
    public void insertAppointment_saiĐịnhDạngDateTime_trảVềMinus1() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertEquals(-1L, helper.insertAppointment(
                new Appointment("X", "Y", "25/10/2026", "09:00", true)));
        assertEquals(-1L, helper.insertAppointment(
                new Appointment("X", "Y", "2026-10-25", "9h", true)));
        assertEquals(-1L, helper.insertAppointment(
                new Appointment(null, "Y", "2026-10-25", "09:00", true)));
        assertEquals(-1L, helper.insertAppointment(null));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 4. getAllAppointments (đã phủ trong seed test ở trên)
    // ------------------------------------------------------------------

    @Test
    public void getAllAppointments_sắpXếpDateAscTimeAsc() {
        DatabaseHelper helper = new DatabaseHelper(context);
        // Thêm lịch cào ngẫu: cùng ngày nhưng giờ sớm hơn, và ngày muộn hơn
        helper.insertAppointment(new Appointment("Sớm trong ngày", "A",
                "2026-10-25", "07:00", false));
        helper.insertAppointment(new Appointment("Muộn hơn", "B",
                "2026-11-01", "08:00", false));

        List<Appointment> all = helper.getAllAppointments();
        assertEquals(5, all.size());
        assertEquals("2026-10-25", all.get(0).getDate());
        assertEquals("07:00", all.get(0).getTime());
        assertEquals("09:00", all.get(1).getTime());
        assertEquals("2026-10-27", all.get(3).getDate());
        assertEquals("2026-11-01", all.get(4).getDate());
        helper.close();
    }

    // ------------------------------------------------------------------
    // 5. getAppointmentById
    // ------------------------------------------------------------------

    @Test
    public void getAppointmentById_khôngTồnTại_trảVềNull() {
        DatabaseHelper helper = new DatabaseHelper(context);
        assertNull(helper.getAppointmentById(99999));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 6. updateReminder
    // ------------------------------------------------------------------

    @Test
    public void updateReminder_bậtTắt_đúngBảnGhi() {
        DatabaseHelper helper = new DatabaseHelper(context);
        // Seed: lịch thứ 3 "Đi cà phê với bạn" có reminder = 0
        List<Appointment> all = helper.getAllAppointments();
        int idTat = all.get(2).getId();

        assertTrue(helper.updateReminder(idTat, true));
        assertTrue(helper.getAppointmentById(idTat).isReminder());

        assertTrue(helper.updateReminder(idTat, false));
        assertFalse(helper.getAppointmentById(idTat).isReminder());

        assertFalse("id không tồn tại phải trả về false", helper.updateReminder(99999, true));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 7. deleteAppointment
    // ------------------------------------------------------------------

    @Test
    public void deleteAppointment_xóaĐúngBảnGhi() {
        DatabaseHelper helper = new DatabaseHelper(context);
        List<Appointment> all = helper.getAllAppointments();
        int id = all.get(0).getId();

        assertTrue(helper.deleteAppointment(id));
        assertNull(helper.getAppointmentById(id));
        assertEquals(2, helper.getAllAppointments().size());

        assertFalse("Xóa lại id đã xóa phải trả về false", helper.deleteAppointment(id));
        helper.close();
    }

    // ------------------------------------------------------------------
    // 8. countAll & 9. countReminders
    // ------------------------------------------------------------------

    @Test
    public void countAll_countReminders_đếmĐúng() {
        DatabaseHelper helper = new DatabaseHelper(context);
        // Seed: 3 lịch, 2 lịch bật reminder
        assertEquals(3L, helper.countAll());
        assertEquals(2L, helper.countReminders());

        List<Appointment> all = helper.getAllAppointments();
        int idBat = all.get(0).getId();
        helper.updateReminder(idBat, false);
        assertEquals(1L, helper.countReminders());

        helper.insertAppointment(new Appointment("Mới", "Z",
                "2026-12-01", "10:00", true));
        assertEquals(4L, helper.countAll());
        assertEquals(2L, helper.countReminders());

        helper.deleteAppointment(idBat);
        assertEquals(3L, helper.countAll());
        helper.close();
    }

    // ------------------------------------------------------------------
    // Bền vững dữ liệu: đóng database rồi mở lại (mô phỏng
    // "đóng và mở lại ứng dụng") — dữ liệu phải còn nguyên.
    // ------------------------------------------------------------------

    @Test
    public void dữLiệuTồnTạiSauKhiĐóngVàMởLạiDatabase() {
        DatabaseHelper helper = new DatabaseHelper(context);
        List<Appointment> before = helper.getAllAppointments();
        long idSua = helper.insertAppointment(new Appointment("Buổi hẹn bền vững", "Nơi X",
                "2026-12-15", "08:15", true));
        assertTrue(idSua > 0);
        helper.updateReminder(before.get(1).getId(), false);
        helper.close(); // đóng hoàn toàn

        // "Mở lại ứng dụng": tạo helper mới trên cùng file db
        DatabaseHelper reopened = new DatabaseHelper(context);
        List<Appointment> after = reopened.getAllAppointments();
        assertEquals(before.size() + 1, after.size());

        Appointment restored = reopened.getAppointmentById((int) idSua);
        assertNotNull(restored);
        assertEquals("Buổi hẹn bền vững", restored.getTitle());
        assertEquals("Nơi X", restored.getLocation());
        assertEquals("2026-12-15", restored.getDate());
        assertEquals("08:15", restored.getTime());
        assertTrue(restored.isReminder());

        assertFalse(reopened.getAppointmentById(before.get(1).getId()).isReminder());
        assertEquals("2 reminder bật: Họp nhóm đồ án + Buổi hẹn bền vững", 2L, reopened.countReminders());
        reopened.close();
    }
}
