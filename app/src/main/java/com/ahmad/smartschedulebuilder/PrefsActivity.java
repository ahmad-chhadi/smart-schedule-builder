package com.ahmad.smartschedulebuilder;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.ahmad.smartschedulebuilder.data.AppData;
import com.ahmad.smartschedulebuilder.logic.ClashExplainer;
import com.ahmad.smartschedulebuilder.logic.ScheduleGenerator;
import com.ahmad.smartschedulebuilder.logic.ScheduleRanker;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** Page 3: filters and ranking, then Generate. */
public class PrefsActivity extends AppCompatActivity {

    private Button btnEarliest;
    private Button btnLatest;
    private SwitchCompat switchFreeDay;
    private EditText inputMaxHours;
    private Spinner spinnerRank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_prefs);
        UiUtils.applySystemBarsPadding(findViewById(R.id.main));

        btnEarliest = findViewById(R.id.btnEarliest);
        btnLatest = findViewById(R.id.btnLatest);
        switchFreeDay = findViewById(R.id.switchFreeDay);
        inputMaxHours = findViewById(R.id.inputMaxHours);
        spinnerRank = findViewById(R.id.spinnerRank);

        // Same order as RANK_FEWEST_GAPS, RANK_FEWEST_DAYS, RANK_LATEST_START
        String[] rankOptions = {"Fewest gaps", "Fewest days on campus", "Latest start"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, rankOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRank.setAdapter(adapter);

        btnEarliest.setOnClickListener(v -> pickPrefTime(true));
        btnLatest.setOnClickListener(v -> pickPrefTime(false));
        findViewById(R.id.btnResetPrefs).setOnClickListener(v -> resetPrefs());
        findViewById(R.id.btnGenerate).setOnClickListener(v -> generateSchedules());

        showPrefsOnScreen();   // show the values chosen last time
    }

    /** Copies the saved preferences into the views. */
    private void showPrefsOnScreen() {
        ScheduleRanker.Preferences prefs = AppData.prefs;
        btnEarliest.setText(prefs.earliestStart == 0
                ? "Earliest class: any"
                : "Earliest class: " + UiUtils.formatClock(prefs.earliestStart));
        btnLatest.setText(prefs.latestEnd == 24 * 60
                ? "Latest end: any"
                : "Latest end: " + UiUtils.formatClock(prefs.latestEnd));
        switchFreeDay.setChecked(prefs.wantFreeDay);
        inputMaxHours.setText(prefs.maxHoursPerDay == 24 ? "" : String.valueOf(prefs.maxHoursPerDay));
        spinnerRank.setSelection(prefs.rankBy);
    }

    /** Opens a clock dialog. isEarliest = true for "earliest class", false for "latest end". */
    private void pickPrefTime(boolean isEarliest) {
        int hour = isEarliest ? 8 : 17;
        new TimePickerDialog(this, (view, h, m) -> {
            if (isEarliest) {
                AppData.prefs.earliestStart = TimeSlot.toMinutes(h, m);
            } else {
                AppData.prefs.latestEnd = TimeSlot.toMinutes(h, m);
            }
            showPrefsOnScreen();
        }, hour, 0, true).show();
    }

    private void resetPrefs() {
        AppData.prefs.earliestStart = 0;
        AppData.prefs.latestEnd = 24 * 60;
        AppData.prefs.wantFreeDay = false;
        AppData.prefs.maxHoursPerDay = 24;
        AppData.prefs.rankBy = ScheduleRanker.RANK_FEWEST_GAPS;
        showPrefsOnScreen();
        UiUtils.toast(this, "Preferences reset");
    }

    /** Copies the values on screen into AppData.prefs. Returns false if something is invalid. */
    private boolean readPrefsFromScreen() {
        ScheduleRanker.Preferences prefs = AppData.prefs;
        if (prefs.latestEnd <= prefs.earliestStart) {
            UiUtils.toast(this, "Latest end must be after earliest class");
            return false;
        }
        String hoursText = inputMaxHours.getText().toString().trim();
        if (hoursText.isEmpty()) {
            prefs.maxHoursPerDay = 24;
        } else {
            int hours = Integer.parseInt(hoursText);
            if (hours < 1 || hours > 24) {
                UiUtils.toast(this, "Max hours must be between 1 and 24");
                return false;
            }
            prefs.maxHoursPerDay = hours;
        }
        prefs.wantFreeDay = switchFreeDay.isChecked();
        prefs.rankBy = spinnerRank.getSelectedItemPosition();
        return true;
    }

    private void generateSchedules() {
        if (!readPrefsFromScreen()) {
            return;
        }

        List<List<Section>> all = ScheduleGenerator.generate(AppData.courses);
        AppData.lastTotalFound = all.size();
        // If nothing fits, find out why
        AppData.clashReasons = all.isEmpty()
                ? ClashExplainer.explain(AppData.courses)
                : new ArrayList<>();
        AppData.results = ScheduleRanker.filterAndSort(all, AppData.prefs);

        startActivity(new Intent(this, SchedulesActivity.class));
    }
}