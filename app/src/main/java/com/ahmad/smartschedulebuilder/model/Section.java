package com.ahmad.smartschedulebuilder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One section of a course (e.g. CSCI 410 - Section A).
 * A section meets one or more times per week.
 */
public class Section {

    private final String courseCode;   // e.g. "CSCI 410"
    private final String name;         // e.g. "A"
    private final List<TimeSlot> slots = new ArrayList<>();

    public Section(String courseCode, String name) {
        this.courseCode = courseCode;
        this.name = name;
    }

    public void addSlot(TimeSlot slot) {
        slots.add(slot);
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getName() {
        return name;
    }

    /** Read-only list, so nobody can change the slots from outside. */
    public List<TimeSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /** True if ANY slot of this section overlaps ANY slot of the other section. */
    public boolean conflictsWith(Section other) {
        for (TimeSlot mine : this.slots) {
            for (TimeSlot theirs : other.slots) {
                if (mine.overlapsWith(theirs)) {
                    return true; // one clash is enough
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return courseCode + " (" + name + ") " + slots;
    }
}