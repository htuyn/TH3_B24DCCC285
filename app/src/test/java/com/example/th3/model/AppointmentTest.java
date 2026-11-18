package com.example.th3.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test cho {@link Appointment} model.
 * Chạy trên local JVM mà không cần Android device/emulator.
 */
public class AppointmentTest {

    @Test
    public void testDefaultConstructor() {
        Appointment appointment = new Appointment();
        assertEquals(-1, appointment.getId());
        assertFalse(appointment.isReminder());
        assertFalse(appointment.getReminder());
    }

    @Test
    public void testConstructorWithoutId() {
        Appointment appointment = new Appointment("Họp nhóm", "Phòng 101", "2026-11-20", "10:00", true);
        assertEquals(-1, appointment.getId());
        assertEquals("Họp nhóm", appointment.getTitle());
        assertEquals("Phòng 101", appointment.getLocation());
        assertEquals("2026-11-20", appointment.getDate());
        assertEquals("10:00", appointment.getTime());
        assertTrue(appointment.isReminder());
        assertTrue(appointment.getReminder());
    }

    @Test
    public void testFullConstructor() {
        Appointment appointment = new Appointment(10, "Khám bệnh", "Bệnh viện", "2026-11-21", "14:00", false);
        assertEquals(10, appointment.getId());
        assertEquals("Khám bệnh", appointment.getTitle());
        assertEquals("Bệnh viện", appointment.getLocation());
        assertEquals("2026-11-21", appointment.getDate());
        assertEquals("14:00", appointment.getTime());
        assertFalse(appointment.isReminder());
        assertFalse(appointment.getReminder());
    }

    @Test
    public void testGettersAndSetters() {
        Appointment appointment = new Appointment();
        appointment.setId(99);
        appointment.setTitle("Phỏng vấn");
        appointment.setLocation("Tầng 5");
        appointment.setDate("2026-11-22");
        appointment.setTime("08:30");
        appointment.setReminder(true);

        assertEquals(99, appointment.getId());
        assertEquals("Phỏng vấn", appointment.getTitle());
        assertEquals("Tầng 5", appointment.getLocation());
        assertEquals("2026-11-22", appointment.getDate());
        assertEquals("08:30", appointment.getTime());
        assertTrue(appointment.isReminder());
        assertTrue(appointment.getReminder());

        appointment.setReminder(false);
        assertFalse(appointment.isReminder());
        assertFalse(appointment.getReminder());
    }

    @Test
    public void testEqualsAndHashCode() {
        Appointment a1 = new Appointment(1, "A", "B", "2026-11-20", "09:00", true);
        Appointment a2 = new Appointment(1, "A", "B", "2026-11-20", "09:00", true);
        Appointment a3 = new Appointment(2, "A", "B", "2026-11-20", "09:00", true);
        Appointment a4 = new Appointment(1, "Khac", "B", "2026-11-20", "09:00", true);

        // Reflexive
        assertEquals(a1, a1);
        // Symmetric
        assertEquals(a1, a2);
        assertEquals(a2, a1);
        assertEquals(a1.hashCode(), a2.hashCode());

        // Different ID
        assertNotEquals(a1, a3);
        // Different Title
        assertNotEquals(a1, a4);
        // Null & different type
        assertNotEquals(null, a1);
        assertNotEquals("string", a1);
    }

    @Test
    public void testToString() {
        Appointment appointment = new Appointment(5, "Sinh nhật", "Nhà hàng", "2026-12-01", "18:00", true);
        String str = appointment.toString();
        assertNotNull(str);
        assertTrue(str.contains("5"));
        assertTrue(str.contains("Sinh nhật"));
        assertTrue(str.contains("Nhà hàng"));
        assertTrue(str.contains("2026-12-01"));
        assertTrue(str.contains("18:00"));
        assertTrue(str.contains("true"));
    }
}
