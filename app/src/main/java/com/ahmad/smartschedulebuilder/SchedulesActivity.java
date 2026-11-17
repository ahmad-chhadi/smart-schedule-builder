package com.ahmad.smartschedulebuilder;

import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.TimetableView;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.List;

/** Page 4: browse the generated schedules, best first. */
public class SchedulesActivity extends AppCompatActivity {

    private TextView textScheduleHeader;
    private TextView textScheduleInfo;
    private TextView textScheduleBody;
    private TimetableView timetable;
    private Button btnPrev;
    private Button btnNext;

    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_schedules);
        UiUtils.applySystemBarsPadding(findViewById(R.id.main));

        textScheduleHeader = findViewById(R.id.textScheduleHeader);
        textScheduleInfo = findViewById(R.id.textScheduleInfo);
        textScheduleBody = findViewById(R.id.textScheduleBody);
        timetable = findViewById(R.id.timetable);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showCurrentSchedule();
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentIndex < AppData.results.size() - 1) {
                currentIndex++;
                showCurrentSchedule();
            }
        });
        findViewById(R.id.btnBackToPrefs).setOnClickListener(v -> finish());

        showCurrentSchedule();
    }

    /** Displays the schedule at currentIndex, or a message if there is nothing to show. */
    private void showCurrentSchedule() {
        List<List<Section>> results = AppData.results;

        if (results.isEmpty()) {
            if (AppData.lastTotalFound == 0) {
                textScheduleHeader.setText("No clash-free schedule exists");
                textScheduleInfo.setText("Every combination has a time clash. Try adding more sections.");
            } else {
                textScheduleHeader.setText("No schedule matches your preferences");
                textScheduleInfo.setText(AppData.lastTotalFound
                        + " clash-free schedule(s) exist, but none pass your filters. Try relaxing them.");
            }
            timetable.setVisibility(View.GONE);
            textScheduleBody.setText("");
            btnPrev.setEnabled(false);
            btnNext.setEnabled(false);
            return;
        }

        List<Section> schedule = results.get(currentIndex);
        textScheduleHeader.setText("Schedule " + (currentIndex + 1) + " of " + results.size());
        textScheduleInfo.setText("Gaps: " + UiUtils.formatDuration(ScheduleRanker.countGapMinutes(schedule))
                + "   ·   Days on campus: " + ScheduleRanker.countDays(schedule));

        // Draw the weekly grid
        timetable.setVisibility(View.VISIBLE);
        timetable.setSchedule(schedule);

        // Legend: a colored square + course, section and times
        SpannableStringBuilder legend = new SpannableStringBuilder();
        for (int i = 0; i < schedule.size(); i++) {
            Section s = schedule.get(i);
            int start = legend.length();
            legend.append("■ ");
            legend.setSpan(new ForegroundColorSpan(TimetableView.colorForCourse(i)),
                    start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            legend.append(s.getCourseCode()).append("  (Section ").append(s.getName()).append(")\n");
            for (TimeSlot slot : s.getSlots()) {
                legend.append("     ").append(slot.toString()).append("\n");
            }
        }
        textScheduleBody.setText(legend);

        btnPrev.setEnabled(currentIndex > 0);
        btnNext.setEnabled(currentIndex < results.size() - 1);
    }
}