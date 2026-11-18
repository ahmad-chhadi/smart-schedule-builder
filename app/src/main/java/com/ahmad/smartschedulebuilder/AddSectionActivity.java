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
        textCourseTitle.setText(getString(R.string.course_row_title, course.getCode(), course.getTitle()));

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
                btnStartTime.setText(getString(R.string.btn_start_time_value, UiUtils.formatClock(startMinute)));
            } else {
                endMinute = TimeSlot.toMinutes(h, m);
                btnEndTime.setText(getString(R.string.btn_end_time_value, UiUtils.formatClock(endMinute)));
            }
        }, hour, minute, true).show();
    }

    private void addSection() {
        // 1) Validate the input
        String name = inputSectionName.getText().toString().trim().toUpperCase(Locale.US);
        if (name.isEmpty()) {
            UiUtils.toast(this, getString(R.string.msg_enter_section_name));
            return;
        }
        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < dayBoxes.length; i++) {
            if (dayBoxes[i].isChecked()) {
                days.add(i);
            }
        }
        if (days.isEmpty()) {
            UiUtils.toast(this, getString(R.string.msg_choose_day));
            return;
        }
        if (startMinute < 0 || endMinute < 0) {
            UiUtils.toast(this, getString(R.string.msg_choose_times));
            return;
        }
        if (endMinute <= startMinute) {
            UiUtils.toast(this, getString(R.string.msg_end_after_start));
            return;
        }

        // 2) Find the section if it already exists
        Section section = null;
        for (Section s : course.getSections()) {
            if (s.getName().equals(name)) {
                section = s;
            }
        }

        // 3) Build the new time slots and make sure they do not overlap the section's own times
        List<TimeSlot> newSlots = new ArrayList<>();
        for (int day : days) {
            TimeSlot slot = new TimeSlot(day, startMinute, endMinute);
            if (section != null) {
                for (TimeSlot existing : section.getSlots()) {
                    if (existing.overlapsWith(slot)) {
                        UiUtils.toast(this, getString(R.string.msg_section_overlaps, name,
                                UiUtils.formatSlot(this, existing)));
                        return;
                    }
                }
            }
            newSlots.add(slot);
        }

        // 4) Create the section if it is new, then add the slots
        if (section == null) {
            section = course.addSection(name);
        }
        for (TimeSlot slot : newSlots) {
            section.addSlot(slot);
        }
        AppData.clearResults();

        // 5) Reset the form and refresh the list
        inputSectionName.getText().clear();
        for (CheckBox box : dayBoxes) {
            box.setChecked(false);
        }
        refreshSectionList();
        UiUtils.toast(this, getString(R.string.msg_section_added, name));
    }

    /** Rebuilds the list of sections of this course: one card per section. */
    private void refreshSectionList() {
        sectionListContainer.removeAllViews();
        textEmptySections.setVisibility(course.getSections().isEmpty() ? View.VISIBLE : View.GONE);

        for (Section s : course.getSections()) {
            View card = getLayoutInflater().inflate(R.layout.item_card, sectionListContainer, false);
            TextView title = card.findViewById(R.id.textCardTitle);
            TextView details = card.findViewById(R.id.textCardDetails);
            title.setText(getString(R.string.section_title, s.getName()));
            details.setText(UiUtils.formatSlots(this, s.getSlots()));

            card.setOnLongClickListener(v -> {
                course.removeSection(s);
                AppData.clearResults();
                refreshSectionList();
                UiUtils.toast(this, getString(R.string.msg_section_removed, s.getName()));
                return true;
            });
            sectionListContainer.addView(card);
        }
    }
}
