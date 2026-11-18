package com.ahmad.smartschedulebuilder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.TimetableView;
import com.ahmad.smartschedulebuilder.ui.UiUtils;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Background thread used to save the shared image (file work must not block the screen)
    private final ExecutorService backgroundThread = Executors.newSingleThreadExecutor();

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundThread.shutdown();
    }

    /** Displays the schedule at currentIndex, or a message if there is nothing to show. */
    private void showCurrentSchedule() {
        List<List<Section>> results = AppData.results;

        if (results.isEmpty()) {
            if (AppData.lastTotalFound == -1) {
                // Happens if Android closed the app in the background and its memory was cleared
                textScheduleHeader.setText(R.string.no_schedules_yet);
                textScheduleInfo.setText(R.string.no_schedules_yet_info);
            } else if (AppData.lastTotalFound == 0) {
                textScheduleHeader.setText(R.string.no_clash_free);
                textScheduleInfo.setText(String.join("\n\n", AppData.clashReasons));
            } else {
                textScheduleHeader.setText(R.string.no_match);
                textScheduleInfo.setText(getResources().getQuantityString(
                        R.plurals.no_match_info, AppData.lastTotalFound, AppData.lastTotalFound));
            }
            timetable.setVisibility(View.GONE);
            textScheduleBody.setText(null);
            btnPrev.setEnabled(false);
            btnNext.setEnabled(false);
            btnShare.setEnabled(false);
            return;
        }

        List<Section> schedule = results.get(currentIndex);
        textScheduleHeader.setText(getString(R.string.schedule_header, currentIndex + 1, results.size()));
        textScheduleInfo.setText(getString(R.string.schedule_info,
                UiUtils.formatDuration(this, ScheduleRanker.countGapMinutes(schedule)),
                ScheduleRanker.countDays(schedule)));

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
            legend.append(getString(R.string.legend_course, s.getCourseCode(), s.getName())).append("\n");
            for (TimeSlot slot : s.getSlots()) {
                legend.append("     ").append(UiUtils.formatSlot(this, slot)).append("\n");
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
            UiUtils.toast(this, getString(R.string.msg_nothing_to_share));
            return;
        }

        // 1) Draw the timetable onto a bitmap. Views may only be touched on the main (UI) thread.
        Bitmap bitmap = Bitmap.createBitmap(
                timetable.getWidth(), timetable.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        // Same background as the screen, so the image looks right in light and dark mode
        canvas.drawColor(MaterialColors.getColor(timetable, com.google.android.material.R.attr.colorSurface));
        timetable.draw(canvas);

        File folder = new File(getCacheDir(), "images");
        btnShare.setEnabled(false);      // avoid double taps while saving

        // 2) Saving the PNG file is slow work, so it runs on the background thread
        backgroundThread.execute(() -> {
            File file = savePng(bitmap, folder);

            // 3) Back on the main thread: open the share menu (or show an error)
            runOnUiThread(() -> {
                btnShare.setEnabled(true);
                if (file == null) {
                    UiUtils.toast(this, getString(R.string.msg_image_failed));
                } else {
                    openShareMenu(file);
                }
            });
        });
    }

    /** Saves the bitmap as images/schedule.png in the cache folder. Returns null if it fails. */
    @WorkerThread
    private static File savePng(Bitmap bitmap, File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            return null;
        }
        File file = new File(folder, "schedule.png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    /** Opens the Android share menu with a safe, temporary link to the image. */
    private void openShareMenu(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.share_chooser_title)));
    }
}
