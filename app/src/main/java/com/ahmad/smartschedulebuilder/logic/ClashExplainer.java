package com.ahmad.smartschedulebuilder.logic;

import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * Explains WHY no schedule could be found.
 * It only finds the problem; the pages turn it into a message in the phone's language.
 */
public class ClashExplainer {

    /** Returns every pair of courses that can never be taken together. Each pair = {courseA, courseB}. */
    public static List<Course[]> alwaysClashingPairs(List<Course> courses) {
        List<Course[]> pairs = new ArrayList<>();

        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                Course a = courses.get(i);
                Course b = courses.get(j);
                if (alwaysClash(a, b)) {
                    pairs.add(new Course[]{a, b});
                }
            }
        }
        return pairs;
    }

    /** True if EVERY section of a clashes with EVERY section of b. */
    private static boolean alwaysClash(Course a, Course b) {
        for (Section sa : a.getSections()) {
            for (Section sb : b.getSections()) {
                if (!sa.conflictsWith(sb)) {
                    return false;   // found one pair that fits -> not "always"
                }
            }
        }
        return true;
    }
}
