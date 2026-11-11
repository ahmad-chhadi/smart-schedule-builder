package com.ahmad.smartschedulebuilder.logic;

import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds every combination of sections (one per course) with no time clashes,
 * using backtracking.
 */
public class ScheduleGenerator {

    // Safety limit so the app never freezes with too many combinations.
    private static final int MAX_RESULTS = 500;

    /** Entry point: returns all valid schedules. Each schedule = one section per course. */
    public static List<List<Section>> generate(List<Course> courses) {
        List<List<Section>> results = new ArrayList<>();
        backtrack(courses, 0, new ArrayList<>(), results);
        return results;
    }

    /**
     * index   = which course we are choosing a section for now
     * chosen  = sections picked so far (one for each earlier course)
     * results = where complete schedules are saved
     */
    private static void backtrack(List<Course> courses, int index,
                                  List<Section> chosen, List<List<Section>> results) {

        // Stop early if we already have enough results.
        if (results.size() >= MAX_RESULTS) {
            return;
        }

        // 1) BASE CASE: every course already has a section -> save the schedule.
        if (index == courses.size()) {
            results.add(new ArrayList<>(chosen)); // save a COPY
            return;
        }

        // 2) RECURSIVE CASE: try each section of the current course.
        for (Section section : courses.get(index).getSections()) {
            if (fitsWith(section, chosen)) {
                chosen.add(section);                          // choose
                backtrack(courses, index + 1, chosen, results); // go to next course
                chosen.remove(chosen.size() - 1);             // un-choose (backtrack)
            }
        }
    }

    /** Returns true if the candidate section clashes with NONE of the chosen sections. */
    private static boolean fitsWith(Section candidate, List<Section> chosen) {
        for (Section s : chosen) {
            if (candidate.conflictsWith(s)) {
                return false;
            }
        }
        return true;
    }
}