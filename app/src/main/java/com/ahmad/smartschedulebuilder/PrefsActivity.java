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
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.ahmad.smartschedulebuilder.ui.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // Options come from res/values/strings.xml (same order as the RANK_ constants)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.rank_options, android.R.layout.simple_spinner_item);
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
                ? getString(R.string.pref_earliest_any)
                : getString(R.string.pref_earliest_value, UiUtils.formatClock(prefs.earliestStart)));
        btnLatest.setText(prefs.latestEnd == 24 * 60
                ? getString(R.string.pref_latest_any)
                : getString(R.string.pref_latest_value, UiUtils.formatClock(prefs.latestEnd)));
        switchFreeDay.setChecked(prefs.wantFreeDay);
        if (prefs.maxHoursPerDay == 24) {
            inputMaxHours.getText().clear();   // 24 = no limit, so leave the field empty
        } else {
            inputMaxHours.setText(String.format(Locale.US, "%d", prefs.maxHoursPerDay));
        }
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
        UiUtils.toast(this, getString(R.string.msg_prefs_reset));
    }

    /** Copies the values on screen into AppData.prefs. Returns false if something is invalid. */
    private boolean readPrefsFromScreen() {
        ScheduleRanker.Preferences prefs = AppData.prefs;
        if (prefs.latestEnd <= prefs.earliestStart) {
            UiUtils.toast(this, getString(R.string.msg_latest_after_earliest));
            return false;
        }
        String hoursText = inputMaxHours.getText().toString().trim();
        if (hoursText.isEmpty()) {
            prefs.maxHoursPerDay = 24;
        } else {
            int hours = Integer.parseInt(hoursText);   // safe: the field allows 2 digits only
            if (hours < 1 || hours > 24) {
                UiUtils.toast(this, getString(R.string.msg_max_hours_range));
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
        AppData.clashReasons = all.isEmpty() ? explainClashes() : new ArrayList<>();
        AppData.results = ScheduleRanker.filterAndSort(all, AppData.prefs);

        startActivity(new Intent(this, SchedulesActivity.class));
    }

    /** Turns the clashing course pairs into messages in the phone's language. */
    private List<String> explainClashes() {
        List<String> reasons = new ArrayList<>();
        for (Course[] pair : ClashExplainer.alwaysClashingPairs(AppData.courses)) {
            reasons.add(getString(R.string.msg_always_clash, pair[0].getCode(), pair[1].getCode()));
        }
        if (reasons.isEmpty()) {
            reasons.add(getString(R.string.msg_no_single_pair));
        }
        return reasons;
    }
}
