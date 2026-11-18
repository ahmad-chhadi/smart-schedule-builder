package com.ahmad.smartschedulebuilder.logic;

import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Filters schedules by the user's preferences and sorts them best-first. */
public class ScheduleRanker {

    private static final int DAYS_IN_WEEK = 6;   // Mon ... Sat
    private static final int WORK_DAYS = 5;      // Mon ... Fri (used for "free day")

    // Ranking options (same order as the dropdown on the Prefs page)
    public static final int RANK_FEWEST_GAPS = 0;
    public static final int RANK_FEWEST_DAYS = 1;
    public static final int RANK_LATEST_START = 2;

    /** The user's choices on the Prefs page. Defaults = no filter. */
    public static class Preferences {
        public int earliestStart = 0;          // no class may start before this (minutes)
        public int latestEnd = 24 * 60;        // no class may end after this (minutes)
        public boolean wantFreeDay = false;    // at least one weekday (Mon-Fri) without classes
        public int maxHoursPerDay = 24;        // max class hours in one day
        public int rankBy = RANK_FEWEST_GAPS;
    }

    /** Keeps the schedules that pass every filter, sorted best-first. */
    public static List<List<Section>> filterAndSort(List<List<Section>> schedules, Preferences prefs) {
        List<List<Section>> kept = new ArrayList<>();
        for (List<Section> schedule : schedules) {
            if (passesFilters(schedule, prefs)) {
                kept.add(schedule);
            }
        }
        // Lower score = better schedule
        kept.sort(Comparator.comparingInt(s -> score(s, prefs.rankBy)));
        return kept;
    }

    private static boolean passesFilters(List<Section> schedule, Preferences prefs) {
        int[] minutesPerDay = new int[DAYS_IN_WEEK];

        for (TimeSlot slot : allSlots(schedule)) {
            if (slot.getStartMinute() < prefs.earliestStart) return false;
            if (slot.getEndMinute() > prefs.latestEnd) return false;
            minutesPerDay[slot.getDay()] += slot.getEndMinute() - slot.getStartMinute();
        }

        for (int day = 0; day < DAYS_IN_WEEK; day++) {
            if (minutesPerDay[day] > prefs.maxHoursPerDay * 60) return false;
        }

        if (prefs.wantFreeDay) {
            boolean hasFreeDay = false;
            for (int day = 0; day < WORK_DAYS; day++) {
                if (minutesPerDay[day] == 0) {
                    hasFreeDay = true;
                }
            }
            if (!hasFreeDay) return false;
        }
        return true;
    }

    private static int score(List<Section> schedule, int rankBy) {
        switch (rankBy) {
            case RANK_FEWEST_DAYS:
                // Fewer days first; if equal, fewer gaps first
                return countDays(schedule) * 10000 + countGapMinutes(schedule);
            case RANK_LATEST_START:
                // Later first class = better, so use a negative number
                return -earliestStart(schedule);
            case RANK_FEWEST_GAPS:
            default:
                return countGapMinutes(schedule);
        }
    }

    /** Total free minutes between classes on the same day. */
    public static int countGapMinutes(List<Section> schedule) {
        int total = 0;
        List<TimeSlot> slots = allSlots(schedule);
        for (int day = 0; day < DAYS_IN_WEEK; day++) {
            List<TimeSlot> daySlots = new ArrayList<>();
            for (TimeSlot slot : slots) {
                if (slot.getDay() == day) {
                    daySlots.add(slot);
                }
            }
            daySlots.sort(Comparator.comparingInt(TimeSlot::getStartMinute));
            for (int i = 1; i < daySlots.size(); i++) {
                int gap = daySlots.get(i).getStartMinute() - daySlots.get(i - 1).getEndMinute();
                if (gap > 0) {
                    total += gap;
                }
            }
        }
        return total;
    }

    /** Number of days with at least one class. */
    public static int countDays(List<Section> schedule) {
        boolean[] used = new boolean[DAYS_IN_WEEK];
        for (TimeSlot slot : allSlots(schedule)) {
            used[slot.getDay()] = true;
        }
        int count = 0;
        for (boolean u : used) {
            if (u) count++;
        }
        return count;
    }

    /** Start time of the earliest class in the week. */
    public static int earliestStart(List<Section> schedule) {
        int earliest = 24 * 60;
        for (TimeSlot slot : allSlots(schedule)) {
            earliest = Math.min(earliest, slot.getStartMinute());
        }
        return earliest;
    }

    private static List<TimeSlot> allSlots(List<Section> schedule) {
        List<TimeSlot> all = new ArrayList<>();
        for (Section section : schedule) {
            all.addAll(section.getSlots());
        }
        return all;
    }
}