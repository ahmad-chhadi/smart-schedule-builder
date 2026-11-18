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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;
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
        findViewById(R.id.btnExample).setOnClickListener(v -> loadExampleData());
        findViewById(R.id.btnLanguage).setOnClickListener(v -> switchLanguage());
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
            UiUtils.toast(this, getString(R.string.msg_enter_course_code));
            return;
        }
        if (AppData.findCourse(code) != null) {
            UiUtils.toast(this, getString(R.string.msg_course_exists, code));
            return;
        }

        Course course = new Course(code, title.isEmpty() ? "-" : title);
        AppData.courses.add(course);
        AppData.clearResults();
        inputCourseCode.getText().clear();
        inputCourseTitle.getText().clear();

        // Go straight to adding sections for the new course
        openAddSection(course);
    }

    /** Switches the app between English and Arabic. The pages are recreated; AppData stays in memory. */
    private void switchLanguage() {
        String current = getResources().getConfiguration().getLocales().get(0).getLanguage();
        String target = current.equals("ar") ? "en" : "ar";
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(target));
    }

    /** Fills in two sample courses (useful for a quick demo). Replaces the current courses. */
    private void loadExampleData() {
        AppData.courses.clear();

        Course csci = new Course("CSCI 410", "Mobile Apps");
        Section csciA = csci.addSection("A");
        csciA.addSlot(new TimeSlot(0, TimeSlot.toMinutes(8, 0), TimeSlot.toMinutes(9, 15)));    // Mon
        csciA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(8, 0), TimeSlot.toMinutes(9, 15)));    // Wed
        Section csciB = csci.addSection("B");
        csciB.addSlot(new TimeSlot(1, TimeSlot.toMinutes(11, 0), TimeSlot.toMinutes(12, 15)));  // Tue
        csciB.addSlot(new TimeSlot(3, TimeSlot.toMinutes(11, 0), TimeSlot.toMinutes(12, 15)));  // Thu

        Course math = new Course("MATH 210", "Calculus II");
        Section mathA = math.addSection("A");
        mathA.addSlot(new TimeSlot(0, TimeSlot.toMinutes(9, 30), TimeSlot.toMinutes(10, 45)));  // Mon
        mathA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(9, 30), TimeSlot.toMinutes(10, 45)));  // Wed
        Section mathB = math.addSection("B");
        mathB.addSlot(new TimeSlot(1, TimeSlot.toMinutes(8, 0), TimeSlot.toMinutes(9, 15)));    // Tue
        mathB.addSlot(new TimeSlot(3, TimeSlot.toMinutes(8, 0), TimeSlot.toMinutes(9, 15)));    // Thu

        AppData.courses.add(csci);
        AppData.courses.add(math);
        AppData.clearResults();
        refreshCourseList();
        UiUtils.toast(this, getString(R.string.msg_example_loaded));
    }

    /** Opens the Add Sections page for one course. */
    private void openAddSection(Course course) {
        Intent intent = new Intent(this, AddSectionActivity.class);
        intent.putExtra(AddSectionActivity.EXTRA_COURSE_CODE, course.getCode());
        startActivity(intent);
    }

    private void goToPrefs() {
        if (AppData.courses.isEmpty()) {
            UiUtils.toast(this, getString(R.string.msg_add_course_first));
            return;
        }
        for (Course c : AppData.courses) {
            if (c.getSections().isEmpty()) {
                UiUtils.toast(this, getString(R.string.msg_course_no_sections, c.getCode()));
                return;
            }
        }
        startActivity(new Intent(this, PrefsActivity.class));
    }

    /** Asks for confirmation, then removes every course. */
    private void confirmClearAll() {
        if (AppData.courses.isEmpty()) {
            UiUtils.toast(this, getString(R.string.msg_nothing_to_clear));
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_dialog_title)
                .setMessage(R.string.clear_dialog_message)
                .setPositiveButton(R.string.clear_dialog_confirm, (dialog, which) -> {
                    AppData.courses.clear();
                    AppData.clearResults();
                    refreshCourseList();
                    UiUtils.toast(this, getString(R.string.msg_all_removed));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Rebuilds the list of courses on screen: one card per course. */
    private void refreshCourseList() {
        courseListContainer.removeAllViews();
        textEmptyCourses.setVisibility(AppData.courses.isEmpty() ? View.VISIBLE : View.GONE);

        for (Course course : AppData.courses) {
            // One line per section, e.g. "Section A: Mon 08:00–09:15, Wed 08:00–09:15"
            List<String> lines = new ArrayList<>();
            for (Section s : course.getSections()) {
                lines.add(getString(R.string.course_row_section, s.getName(),
                        UiUtils.formatSlots(this, s.getSlots())));
            }
            if (lines.isEmpty()) {
                lines.add(getString(R.string.course_row_no_sections));
            }

            View card = getLayoutInflater().inflate(R.layout.item_card, courseListContainer, false);
            TextView title = card.findViewById(R.id.textCardTitle);
            TextView details = card.findViewById(R.id.textCardDetails);
            title.setText(getString(R.string.course_row_title, course.getCode(), course.getTitle()));
            details.setText(String.join("\n", lines));

            card.setOnClickListener(v -> openAddSection(course));
            card.setOnLongClickListener(v -> {
                AppData.courses.remove(course);
                AppData.clearResults();
                refreshCourseList();
                UiUtils.toast(this, getString(R.string.msg_course_removed, course.getCode()));
                return true;
            });
            courseListContainer.addView(card);
        }
    }
}
