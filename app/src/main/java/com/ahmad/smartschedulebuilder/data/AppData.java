package com.ahmad.smartschedulebuilder.data;

import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * Data shared by all pages of the app.
 * It lives in memory (RAM) only while the app is open - it is NOT a database
 * and nothing is saved to the phone or the internet.
 */
public final class AppData {

    public static final List<Course> courses = new ArrayList<>();
    public static final ScheduleRanker.Preferences prefs = new ScheduleRanker.Preferences();

    // Results of the last "Generate"
    public static List<List<Section>> results = new ArrayList<>();
    public static int lastTotalFound = -1;   // -1 = Generate not pressed yet

    private AppData() { }   // no objects of this class

    /** Returns the course with this code, or null if it does not exist. */
    public static Course findCourse(String code) {
        for (Course c : courses) {
            if (c.getCode().equalsIgnoreCase(code)) {
                return c;
            }
        }
        return null;
    }

    /** Old results are no longer valid when the courses change. */
    public static void clearResults() {
        results = new ArrayList<>();
        lastTotalFound = -1;
    }
}