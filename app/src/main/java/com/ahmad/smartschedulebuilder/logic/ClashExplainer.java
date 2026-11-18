package com.ahmad.smartschedulebuilder.logic;

import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;

import java.util.ArrayList;
import java.util.List;

/** Explains WHY no schedule could be found. */
public class ClashExplainer {

    /** Returns one message for every pair of courses that can never be taken together. */
    public static List<String> explain(List<Course> courses) {
        List<String> reasons = new ArrayList<>();

        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                Course a = courses.get(i);
                Course b = courses.get(j);
                if (alwaysClash(a, b)) {
                    reasons.add("⚠ " + a.getCode() + " and " + b.getCode() + " always clash: "
                            + "every section of one overlaps every section of the other.");
                }
            }
        }

        if (reasons.isEmpty()) {
            reasons.add("No two courses always clash, but no combination of all your courses "
                    + "fits together. Try adding more sections.");
        }
        return reasons;
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