package com.ahmad.smartschedulebuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.ahmad.smartschedulebuilder.logic.ClashExplainer;
import com.ahmad.smartschedulebuilder.logic.ScheduleGenerator;
import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for the scheduling logic (model + logic packages).
 * They run on the computer, without the emulator:
 * right-click this file in Android Studio -> Run 'ScheduleLogicTest'.
 */
public class ScheduleLogicTest {

    private static final int MON = 0, TUE = 1, WED = 2, THU = 3;

    /** Short helper: slot(MON, 10, 0, 11, 15) = Monday 10:00-11:15 */
    private static TimeSlot slot(int day, int startH, int startM, int endH, int endM) {
        return new TimeSlot(day, TimeSlot.toMinutes(startH, startM), TimeSlot.toMinutes(endH, endM));
    }

    /**
     * The example used in the presentation:
     * CSCI 410 - A: Mon/Wed 08:00-09:15, B: Tue/Thu 11:00-12:15
     * MATH 210 - A: Mon/Wed 09:30-10:45, B: Tue/Thu 08:00-09:15
     * No pair clashes, so there are 2 x 2 = 4 schedules.
     */
    private static List<Course> sampleCourses() {
        Course csci = new Course("CSCI 410", "Mobile Apps");
        Section csciA = csci.addSection("A");
        csciA.addSlot(slot(MON, 8, 0, 9, 15));
        csciA.addSlot(slot(WED, 8, 0, 9, 15));
        Section csciB = csci.addSection("B");
        csciB.addSlot(slot(TUE, 11, 0, 12, 15));
        csciB.addSlot(slot(THU, 11, 0, 12, 15));

        Course math = new Course("MATH 210", "Calculus II");
        Section mathA = math.addSection("A");
        mathA.addSlot(slot(MON, 9, 30, 10, 45));
        mathA.addSlot(slot(WED, 9, 30, 10, 45));
        Section mathB = math.addSection("B");
        mathB.addSlot(slot(TUE, 8, 0, 9, 15));
        mathB.addSlot(slot(THU, 8, 0, 9, 15));

        return Arrays.asList(csci, math);
    }

    // ---------------- TimeSlot ----------------

    @Test
    public void toMinutes_convertsClockTime() {
        assertEquals(630, TimeSlot.toMinutes(10, 30));
        assertEquals(0, TimeSlot.toMinutes(0, 0));
    }

    @Test
    public void overlap_sameDaySharedTime_isTrue() {
        assertTrue(slot(MON, 10, 0, 11, 15).overlapsWith(slot(MON, 11, 0, 12, 0)));
    }

    @Test
    public void overlap_backToBack_isFalse() {
        // 11:15 end and 11:15 start do not clash
        assertFalse(slot(MON, 10, 0, 11, 15).overlapsWith(slot(MON, 11, 15, 12, 30)));
    }

    @Test
    public void overlap_differentDays_isFalse() {
        assertFalse(slot(MON, 10, 0, 11, 15).overlapsWith(slot(TUE, 10, 0, 11, 15)));
    }

    @Test
    public void timeSlot_rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class, () -> slot(MON, 11, 0, 10, 0));
    }

    @Test
    public void timeSlot_toString_isReadable() {
        assertEquals("Mon 10:00-11:15", slot(MON, 10, 0, 11, 15).toString());
    }

    // ---------------- Section / Course ----------------

    @Test
    public void section_conflictsIfAnySlotOverlaps() {
        Section a = new Section("X", "A");
        a.addSlot(slot(MON, 10, 0, 11, 0));
        a.addSlot(slot(WED, 10, 0, 11, 0));
        Section b = new Section("Y", "A");
        b.addSlot(slot(WED, 10, 30, 11, 30));   // clashes on Wednesday only
        assertTrue(a.conflictsWith(b));
    }

    @Test
    public void course_addAndRemoveSection() {
        Course c = new Course("CSCI 410", "Mobile Apps");
        Section a = c.addSection("A");
        assertEquals("CSCI 410", a.getCourseCode());
        assertEquals(1, c.getSections().size());
        c.removeSection(a);
        assertEquals(0, c.getSections().size());
    }

    // ---------------- ScheduleGenerator ----------------

    @Test
    public void generator_findsEveryCombinationWhenNothingClashes() {
        assertEquals(4, ScheduleGenerator.generate(sampleCourses()).size());
    }

    @Test
    public void generator_eachScheduleHasOneSectionPerCourse() {
        for (List<Section> schedule : ScheduleGenerator.generate(sampleCourses())) {
            assertEquals(2, schedule.size());
            assertEquals("CSCI 410", schedule.get(0).getCourseCode());
            assertEquals("MATH 210", schedule.get(1).getCourseCode());
        }
    }

    @Test
    public void generator_skipsClashingCombinations() {
        Course csci = new Course("CSCI 410", "");
        csci.addSection("A").addSlot(slot(MON, 10, 0, 11, 15));
        csci.addSection("B").addSlot(slot(TUE, 10, 0, 11, 15));
        Course math = new Course("MATH 210", "");
        math.addSection("A").addSlot(slot(MON, 10, 30, 11, 30));  // clashes with CSCI A

        List<List<Section>> schedules = ScheduleGenerator.generate(Arrays.asList(csci, math));
        assertEquals(1, schedules.size());
        assertEquals("B", schedules.get(0).get(0).getName());
    }

    @Test
    public void generator_noCoursesGivesOneEmptySchedule() {
        List<List<Section>> schedules = ScheduleGenerator.generate(new ArrayList<>());
        assertEquals(1, schedules.size());
        assertTrue(schedules.get(0).isEmpty());
    }

    // ---------------- ScheduleRanker ----------------

    @Test
    public void ranker_noFiltersKeepsEverything() {
        List<List<Section>> all = ScheduleGenerator.generate(sampleCourses());
        assertEquals(4, ScheduleRanker.filterAndSort(all, new ScheduleRanker.Preferences()).size());
    }

    @Test
    public void ranker_earliestStartFilter() {
        // "Nothing before 09:00" keeps only CSCI B (11:00) + MATH A (09:30)
        List<List<Section>> all = ScheduleGenerator.generate(sampleCourses());
        ScheduleRanker.Preferences prefs = new ScheduleRanker.Preferences();
        prefs.earliestStart = TimeSlot.toMinutes(9, 0);
        List<List<Section>> kept = ScheduleRanker.filterAndSort(all, prefs);
        assertEquals(1, kept.size());
        assertEquals("B", kept.get(0).get(0).getName());
        assertEquals("A", kept.get(0).get(1).getName());
    }

    @Test
    public void ranker_fewestDaysPutsTwoDaySchedulesFirst() {
        List<List<Section>> all = ScheduleGenerator.generate(sampleCourses());
        ScheduleRanker.Preferences prefs = new ScheduleRanker.Preferences();
        prefs.rankBy = ScheduleRanker.RANK_FEWEST_DAYS;
        List<List<Section>> sorted = ScheduleRanker.filterAndSort(all, prefs);
        assertEquals(2, ScheduleRanker.countDays(sorted.get(0)));
        assertEquals(2, ScheduleRanker.countDays(sorted.get(1)));
        assertEquals(4, ScheduleRanker.countDays(sorted.get(3)));
    }

    @Test
    public void ranker_countsGapsBetweenClassesOnTheSameDay() {
        // CSCI A (08:00-09:15) + MATH A (09:30-10:45) on Mon and Wed -> 15 min gap x 2 days
        List<Course> courses = sampleCourses();
        List<Section> schedule = Arrays.asList(
                courses.get(0).getSections().get(0),
                courses.get(1).getSections().get(0));
        assertEquals(30, ScheduleRanker.countGapMinutes(schedule));
    }

    @Test
    public void ranker_freeDayFilter() {
        // Friday is always free in the sample, so every schedule passes
        List<List<Section>> all = ScheduleGenerator.generate(sampleCourses());
        ScheduleRanker.Preferences prefs = new ScheduleRanker.Preferences();
        prefs.wantFreeDay = true;
        assertEquals(4, ScheduleRanker.filterAndSort(all, prefs).size());
    }

    // ---------------- ClashExplainer ----------------

    @Test
    public void explainer_namesThePairThatAlwaysClashes() {
        Course csci = new Course("CSCI 410", "");
        csci.addSection("A").addSlot(slot(MON, 10, 0, 11, 15));
        Course math = new Course("MATH 210", "");
        math.addSection("A").addSlot(slot(MON, 10, 30, 11, 30));

        List<Course[]> pairs = ClashExplainer.alwaysClashingPairs(Arrays.asList(csci, math));
        assertEquals(1, pairs.size());
        assertEquals("CSCI 410", pairs.get(0)[0].getCode());
        assertEquals("MATH 210", pairs.get(0)[1].getCode());
    }

    @Test
    public void explainer_findsNoPairWhenNoSinglePairIsToBlame() {
        // Each pair can fit, but all three together cannot (each pair shares one free slot)
        Course a = new Course("A", "");
        a.addSection("1").addSlot(slot(MON, 8, 0, 9, 0));
        a.addSection("2").addSlot(slot(TUE, 8, 0, 9, 0));
        Course b = new Course("B", "");
        b.addSection("1").addSlot(slot(MON, 8, 0, 9, 0));
        b.addSection("2").addSlot(slot(TUE, 8, 0, 9, 0));
        Course c = new Course("C", "");
        c.addSection("1").addSlot(slot(MON, 8, 0, 9, 0));
        c.addSection("2").addSlot(slot(TUE, 8, 0, 9, 0));

        List<Course> courses = Arrays.asList(a, b, c);
        assertEquals(0, ScheduleGenerator.generate(courses).size());
        assertTrue(ClashExplainer.alwaysClashingPairs(courses).isEmpty());
    }
}
