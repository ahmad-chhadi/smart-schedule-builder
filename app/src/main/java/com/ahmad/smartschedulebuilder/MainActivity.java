package com.ahmad.smartschedulebuilder;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ---------- App data (kept in memory only, no database) ----------
    private final List<Course> courses = new ArrayList<>();

    // ---------- Pages ----------
    private View pageCourses;
    private View pagePrefs;
    private View pageSchedules;

    // ---------- Courses page views ----------
    private EditText inputCourseCode;
    private EditText inputCourseTitle;
    private EditText inputSectionName;
    private Spinner spinnerCourse;
    private CheckBox[] dayBoxes;          // index 0 = Mon ... 5 = Sat
    private Button btnStartTime;
    private Button btnEndTime;
    private LinearLayout courseListContainer;
    private TextView textEmptyCourses;

    // Times chosen in the time pickers (-1 = not chosen yet)
    private int startMinute = -1;
    private int endMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupTabs();
        setupCoursesPage();
        refreshCourseList();
    }

    // =====================================================================
    // Tabs
    // =====================================================================

    private void setupTabs() {
        pageCourses = findViewById(R.id.pageCourses);
        pagePrefs = findViewById(R.id.pagePrefs);
        pageSchedules = findViewById(R.id.pageSchedules);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPage(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /** Shows the page at the given position (0, 1 or 2) and hides the others. */
    private void showPage(int position) {
        pageCourses.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        pagePrefs.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        pageSchedules.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }

    // =====================================================================
    // Courses page
    // =====================================================================

    private void setupCoursesPage() {
        inputCourseCode = findViewById(R.id.inputCourseCode);
        inputCourseTitle = findViewById(R.id.inputCourseTitle);
        inputSectionName = findViewById(R.id.inputSectionName);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        courseListContainer = findViewById(R.id.courseListContainer);
        textEmptyCourses = findViewById(R.id.textEmptyCourses);

        dayBoxes = new CheckBox[]{
                findViewById(R.id.checkMon),
                findViewById(R.id.checkTue),
                findViewById(R.id.checkWed),
                findViewById(R.id.checkThu),
                findViewById(R.id.checkFri),
                findViewById(R.id.checkSat)
        };

        findViewById(R.id.btnAddCourse).setOnClickListener(v -> addCourse());
        btnStartTime.setOnClickListener(v -> pickTime(true));
        btnEndTime.setOnClickListener(v -> pickTime(false));
        findViewById(R.id.btnAddSection).setOnClickListener(v -> addSection());
    }

    private void addCourse() {
        String code = inputCourseCode.getText().toString().trim().toUpperCase(Locale.US);
        String title = inputCourseTitle.getText().toString().trim();

        if (code.isEmpty()) {
            toast("Please enter a course code");
            return;
        }
        if (findCourse(code) != null) {
            toast(code + " already exists");
            return;
        }

        courses.add(new Course(code, title.isEmpty() ? "-" : title));
        inputCourseCode.setText("");
        inputCourseTitle.setText("");
        refreshCourseList();
        toast("Added " + code);
    }

    /** Returns the course with this code, or null if it does not exist. */
    private Course findCourse(String code) {
        for (Course c : courses) {
            if (c.getCode().equalsIgnoreCase(code)) {
                return c;
            }
        }
        return null;
    }

    /** Opens a clock dialog. isStart = true for the start time, false for the end time. */
    private void pickTime(boolean isStart) {
        int current = isStart ? startMinute : endMinute;
        int hour = current >= 0 ? current / 60 : 8;
        int minute = current >= 0 ? current % 60 : 0;

        new TimePickerDialog(this, (view, h, m) -> {
            String label = String.format(Locale.US, "%02d:%02d", h, m);
            if (isStart) {
                startMinute = TimeSlot.toMinutes(h, m);
                btnStartTime.setText("Start " + label);
            } else {
                endMinute = TimeSlot.toMinutes(h, m);
                btnEndTime.setText("End " + label);
            }
        }, hour, minute, true).show();
    }

    private void addSection() {
        // 1) Validate the input
        if (courses.isEmpty()) {
            toast("Add a course first");
            return;
        }
        String name = inputSectionName.getText().toString().trim().toUpperCase(Locale.US);
        if (name.isEmpty()) {
            toast("Please enter a section name");
            return;
        }
        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < dayBoxes.length; i++) {
            if (dayBoxes[i].isChecked()) {
                days.add(i);
            }
        }
        if (days.isEmpty()) {
            toast("Choose at least one day");
            return;
        }
        if (startMinute < 0 || endMinute < 0) {
            toast("Choose the start and end time");
            return;
        }
        if (endMinute <= startMinute) {
            toast("End time must be after start time");
            return;
        }

        // 2) Find the section, or create it if it is new.
        //    Adding the same section name again adds more times to it
        //    (useful when a section meets at different hours on different days).
        Course course = courses.get(spinnerCourse.getSelectedItemPosition());
        Section section = null;
        for (Section s : course.getSections()) {
            if (s.getName().equals(name)) {
                section = s;
            }
        }
        if (section == null) {
            section = course.addSection(name);
        }

        // 3) Add one time slot for each selected day
        for (int day : days) {
            section.addSlot(new TimeSlot(day, startMinute, endMinute));
        }

        // 4) Reset the form and refresh the list
        inputSectionName.setText("");
        for (CheckBox box : dayBoxes) {
            box.setChecked(false);
        }
        refreshCourseList();
        toast("Added section " + name + " to " + course.getCode());
    }

    /** Rebuilds the list of courses on screen and the course dropdown. */
    private void refreshCourseList() {
        courseListContainer.removeAllViews();
        textEmptyCourses.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);

        List<String> codes = new ArrayList<>();
        for (Course course : courses) {
            codes.add(course.getCode());

            StringBuilder text = new StringBuilder();
            text.append(course.getCode()).append(" - ").append(course.getTitle());
            if (course.getSections().isEmpty()) {
                text.append("\n    (no sections yet)");
            }
            for (Section s : course.getSections()) {
                text.append("\n    Section ").append(s.getName()).append(": ").append(s.getSlots());
            }

            TextView row = new TextView(this);
            row.setText(text);
            row.setTextSize(16);
            row.setPadding(0, 16, 0, 16);
            row.setOnLongClickListener(v -> {
                courses.remove(course);
                refreshCourseList();
                toast("Removed " + course.getCode());
                return true;
            });
            courseListContainer.addView(row);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, codes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(adapter);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}