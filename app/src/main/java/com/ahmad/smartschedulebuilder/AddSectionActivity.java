package com.ahmad.smartschedulebuilder;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Page 2: add sections (days + times) to ONE course. */
public class AddSectionActivity extends AppCompatActivity {

    /** Key used to send the course code from MainActivity to this page. */
    public static final String EXTRA_COURSE_CODE = "course_code";

    private Course course;

    private EditText inputSectionName;
    private CheckBox[] dayBoxes;          // index 0 = Mon ... 5 = Sat
    private Button btnStartTime;
    private Button btnEndTime;
    private LinearLayout sectionListContainer;
    private TextView textEmptySections;

    private int startMinute = -1;         // -1 = not chosen yet
    private int endMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_section);
        UiUtils.applySystemBarsPadding(findViewById(R.id.main));

        // Which course are we editing? (sent by MainActivity)
        String code = getIntent().getStringExtra(EXTRA_COURSE_CODE);
        course = AppData.findCourse(code);
        if (course == null) {
            finish();   // the course no longer exists, go back
            return;
        }

        TextView textCourseTitle = findViewById(R.id.textCourseTitle);
        textCourseTitle.setText(course.getCode() + " - " + course.getTitle());

        inputSectionName = findViewById(R.id.inputSectionName);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        sectionListContainer = findViewById(R.id.sectionListContainer);
        textEmptySections = findViewById(R.id.textEmptySections);

        dayBoxes = new CheckBox[]{
                findViewById(R.id.checkMon),
                findViewById(R.id.checkTue),
                findViewById(R.id.checkWed),
                findViewById(R.id.checkThu),
                findViewById(R.id.checkFri),
                findViewById(R.id.checkSat)
        };

        btnStartTime.setOnClickListener(v -> pickTime(true));
        btnEndTime.setOnClickListener(v -> pickTime(false));
        findViewById(R.id.btnAddSection).setOnClickListener(v -> addSection());
        findViewById(R.id.btnDone).setOnClickListener(v -> finish());   // back to MainActivity

        refreshSectionList();
    }

    /** Opens a clock dialog. isStart = true for the start time, false for the end time. */
    private void pickTime(boolean isStart) {
        int current = isStart ? startMinute : endMinute;
        int hour = current >= 0 ? current / 60 : 8;
        int minute = current >= 0 ? current % 60 : 0;

        new TimePickerDialog(this, (view, h, m) -> {
            if (isStart) {
                startMinute = TimeSlot.toMinutes(h, m);
                btnStartTime.setText("Start " + UiUtils.formatClock(startMinute));
            } else {
                endMinute = TimeSlot.toMinutes(h, m);
                btnEndTime.setText("End " + UiUtils.formatClock(endMinute));
            }
        }, hour, minute, true).show();
    }

    private void addSection() {
        // 1) Validate the input
        String name = inputSectionName.getText().toString().trim().toUpperCase(Locale.US);
        if (name.isEmpty()) {
            UiUtils.toast(this, "Please enter a section name");
            return;
        }
        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < dayBoxes.length; i++) {
            if (dayBoxes[i].isChecked()) {
                days.add(i);
            }
        }
        if (days.isEmpty()) {
            UiUtils.toast(this, "Choose at least one day");
            return;
        }
        if (startMinute < 0 || endMinute < 0) {
            UiUtils.toast(this, "Choose the start and end time");
            return;
        }
        if (endMinute <= startMinute) {
            UiUtils.toast(this, "End time must be after start time");
            return;
        }

        // 2) Find the section, or create it if it is new
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
        AppData.clearResults();

        // 4) Reset the form and refresh the list
        inputSectionName.setText("");
        for (CheckBox box : dayBoxes) {
            box.setChecked(false);
        }
        refreshSectionList();
        UiUtils.toast(this, "Added section " + name);
    }

    /** Rebuilds the list of sections of this course. */
    private void refreshSectionList() {
        sectionListContainer.removeAllViews();
        textEmptySections.setVisibility(course.getSections().isEmpty() ? View.VISIBLE : View.GONE);

        for (Section s : course.getSections()) {
            TextView row = new TextView(this);
            row.setText("Section " + s.getName() + ": " + s.getSlots());
            row.setTextSize(16);
            row.setPadding(0, 12, 0, 12);
            sectionListContainer.addView(row);
        }
    }
}