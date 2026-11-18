package com.ahmad.smartschedulebuilder.ui;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmad.smartschedulebuilder.R;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small helpers used by every page. */
public final class UiUtils {

    private UiUtils() { }

    /** Keeps the content away from the status bar, navigation bar and keyboard. */
    public static void applySystemBarsPadding(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    /** 630 -> "10:30" */
    public static String formatClock(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** 75 -> "1h 15m" (in Arabic: "1س 15د") */
    public static String formatDuration(Context context, int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) return context.getString(R.string.duration_minutes, m);
        if (m == 0) return context.getString(R.string.duration_hours, h);
        return context.getString(R.string.duration_hours_minutes, h, m);
    }

    /** One time slot in the phone's language, e.g. "Mon 10:00–11:15" or "الإثنين 10:00–11:15". */
    public static String formatSlot(Context context, TimeSlot slot) {
        String[] dayNames = context.getResources().getStringArray(R.array.day_names);
        return context.getString(R.string.slot_format, dayNames[slot.getDay()],
                formatClock(slot.getStartMinute()), formatClock(slot.getEndMinute()));
    }

    /** Several time slots joined with commas. */
    public static String formatSlots(Context context, List<TimeSlot> slots) {
        List<String> parts = new ArrayList<>();
        for (TimeSlot slot : slots) {
            parts.add(formatSlot(context, slot));
        }
        return String.join(context.getString(R.string.slot_separator), parts);
    }

    public static void toast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
