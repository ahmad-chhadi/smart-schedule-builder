package com.ahmad.smartschedulebuilder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.Locale;

/** Page 1: add courses and see the list of courses. */
public class MainActivity extends AppCompatActivity {

    private EditText inputCourseCode;
    private EditText inputCourseTitle;
    private LinearLayout courseListContainer;
    private TextView textEmptyCourses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        UiUtils.applySystemBarsPadding(findViewById(R.id.main));

        inputCourseCode = findViewById(R.id.inputCourseCode);
        inputCourseTitle = findViewById(R.id.inputCourseTitle);
        courseListContainer = findViewById(R.id.courseListContainer);
        textEmptyCourses = findViewById(R.id.textEmptyCourses);

        findViewById(R.id.btnAddCourse).setOnClickListener(v -> addCourse());
        findViewById(R.id.btnClearAll).setOnClickListener(v -> confirmClearAll());
        findViewById(R.id.btnGoPrefs).setOnClickListener(v -> goToPrefs());
    }

    /** Runs every time this page appears again (e.g. after returning from Add Sections). */
    @Override
    protected void onResume() {
        super.onResume();
        refreshCourseList();
    }

    private void addCourse() {
        String code = inputCourseCode.getText().toString().trim().toUpperCase(Locale.US);
        String title = inputCourseTitle.getText().toString().trim();

        if (code.isEmpty()) {
            UiUtils.toast(this, "Please enter a course code");
            return;
        }
        if (AppData.findCourse(code) != null) {
            UiUtils.toast(this, code + " already exists");
            return;
        }

        Course course = new Course(code, title.isEmpty() ? "-" : title);
        AppData.courses.add(course);
        AppData.clearResults();
        inputCourseCode.setText("");
        inputCourseTitle.setText("");

        // Go straight to adding sections for the new course
        openAddSection(course);
    }

    /** Opens the Add Sections page for one course. */
    private void openAddSection(Course course) {
        Intent intent = new Intent(this, AddSectionActivity.class);
        intent.putExtra(AddSectionActivity.EXTRA_COURSE_CODE, course.getCode());
        startActivity(intent);
    }

    private void goToPrefs() {
        if (AppData.courses.isEmpty()) {
            UiUtils.toast(this, "Add at least one course first");
            return;
        }
        for (Course c : AppData.courses) {
            if (c.getSections().isEmpty()) {
                UiUtils.toast(this, c.getCode() + " has no sections yet");
                return;
            }
        }
        startActivity(new Intent(this, PrefsActivity.class));
    }

    /** Asks for confirmation, then removes every course. */
    private void confirmClearAll() {
        if (AppData.courses.isEmpty()) {
            UiUtils.toast(this, "There is nothing to clear");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Clear all courses?")
                .setMessage("This removes every course and section you added.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    AppData.courses.clear();
                    AppData.clearResults();
                    refreshCourseList();
                    UiUtils.toast(this, "All courses removed");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Rebuilds the list of courses on screen. */
    private void refreshCourseList() {
        courseListContainer.removeAllViews();
        textEmptyCourses.setVisibility(AppData.courses.isEmpty() ? View.VISIBLE : View.GONE);

        for (Course course : AppData.courses) {
            StringBuilder text = new StringBuilder();
            text.append(course.getCode()).append(" - ").append(course.getTitle());
            if (course.getSections().isEmpty()) {
                text.append("\n    (no sections yet - tap to add)");
            }
            for (Section s : course.getSections()) {
                text.append("\n    Section ").append(s.getName()).append(": ").append(s.getSlots());
            }

            TextView row = new TextView(this);
            row.setText(text);
            row.setTextSize(16);
            row.setPadding(8, 24, 8, 24);
            row.setBackgroundResource(android.R.drawable.list_selector_background);

            row.setOnClickListener(v -> openAddSection(course));
            row.setOnLongClickListener(v -> {
                AppData.courses.remove(course);
                AppData.clearResults();
                refreshCourseList();
                UiUtils.toast(this, "Removed " + course.getCode());
                return true;
            });
            courseListContainer.addView(row);
        }
    }
}