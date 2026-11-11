package com.ahmad.smartschedulebuilder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A course (e.g. CSCI 410 - Mobile Apps) with its available sections. */
public class Course {

    private final String code;    // e.g. "CSCI 410"
    private final String title;   // e.g. "Mobile Apps"
    private final List<Section> sections = new ArrayList<>();

    public Course(String code, String title) {
        this.code = code;
        this.title = title;
    }

    /** Creates a new section for this course and returns it. */
    public Section addSection(String sectionName) {
        Section section = new Section(code, sectionName);
        sections.add(section);
        return section;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }

    @Override
    public String toString() {
        return code + " - " + title + " (" + sections.size() + " sections)";
    }
}