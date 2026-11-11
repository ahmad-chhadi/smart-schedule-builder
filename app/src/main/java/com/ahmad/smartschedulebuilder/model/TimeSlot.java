package com.ahmad.smartschedulebuilder.model;

import java.util.Locale;


public class TimeSlot {

    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    private final int day;          // 0 = Mon ... 5 = Sat
    private final int startMinute;  // minutes from midnight
    private final int endMinute;    // minutes from midnight

    public TimeSlot(int day, int startMinute, int endMinute) {
        if (day < 0 || day >= DAY_NAMES.length) {
            throw new IllegalArgumentException("Invalid day: " + day);
        }
        if (endMinute <= startMinute) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        this.day = day;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
    }

    /** Converts a clock time to minutes, e.g. toMinutes(10, 30) = 630. */
    public static int toMinutes(int hour, int minute) {
        return hour * 60 + minute;
    }

    public int getDay() {
        return day;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    /** Returns true if this slot and the other slot happen at the same time. */
    public boolean overlapsWith(TimeSlot other) {
        if (this.day != other.day) {
            return false; // different days can never clash
        }
        return this.startMinute < other.endMinute
                && other.startMinute < this.endMinute;
    }

    /** Formats minutes as "HH:MM", e.g. 630 -> "10:30". */
    private static String formatTime(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    @Override
    public String toString() {
        return DAY_NAMES[day] + " " + formatTime(startMinute) + "-" + formatTime(endMinute);
    }
}