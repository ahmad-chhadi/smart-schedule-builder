package com.ahmad.smartschedulebuilder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.TimetableView;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Page 4: browse the generated schedules, best first. */
public class SchedulesActivity extends AppCompatActivity {

    private TextView textScheduleHeader;
    private TextView textScheduleInfo;
    private TextView textScheduleBody;
    private TimetableView timetable;
    private Button btnPrev;
    private Button btnNext;
    private Button btnShare;

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
        btnShare = findViewById(R.id.btnShare);

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
        btnShare.setOnClickListener(v -> shareCurrentSchedule());
        findViewById(R.id.btnBackToPrefs).setOnClickListener(v -> finish());

        showCurrentSchedule();
    }

    /** Displays the schedule at currentIndex, or a message if there is nothing to show. */
    private void showCurrentSchedule() {
        List<List<Section>> results = AppData.results;

        if (results.isEmpty()) {
            if (AppData.lastTotalFound == -1) {
                // Happens if Android closed the app in the background and its memory was cleared
                textScheduleHeader.setText("No schedules yet");
                textScheduleInfo.setText("Go back to Preferences and press Generate Schedules.");
            } else if (AppData.lastTotalFound == 0) {
                textScheduleHeader.setText("No clash-free schedule exists");
                textScheduleInfo.setText(String.join("\n\n", AppData.clashReasons));
            } else {
                textScheduleHeader.setText("No schedule matches your preferences");
                textScheduleInfo.setText(AppData.lastTotalFound
                        + " clash-free schedule(s) exist, but none pass your filters. Try relaxing them.");
            }
            timetable.setVisibility(View.GONE);
            textScheduleBody.setText("");
            btnPrev.setEnabled(false);
            btnNext.setEnabled(false);
            btnShare.setEnabled(false);
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
        btnShare.setEnabled(true);
    }

    /** Turns the timetable into a PNG image and opens the Android share menu. */
    private void shareCurrentSchedule() {
        if (AppData.results.isEmpty() || timetable.getWidth() == 0) {
            UiUtils.toast(this, "Nothing to share yet");
            return;
        }

        try {
            // 1) Draw the timetable onto a bitmap (an image in memory)
            Bitmap bitmap = Bitmap.createBitmap(
                    timetable.getWidth(), timetable.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);   // white background behind the grid
            timetable.draw(canvas);

            // 2) Save it as a temporary PNG in the app's cache folder
            File folder = new File(getCacheDir(), "images");
            folder.mkdirs();
            File file = new File(folder, "schedule.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            // 3) Get a safe link to the file and open the share menu
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.putExtra(Intent.EXTRA_TEXT, "My schedule, made with Smart Schedule Builder");
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "Share schedule"));

        } catch (IOException e) {
            UiUtils.toast(this, "Could not create the image");
        }
    }
}