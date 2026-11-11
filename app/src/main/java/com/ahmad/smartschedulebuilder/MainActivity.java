package com.ahmad.smartschedulebuilder;

import android.os.Bundle;
import android.util.Log;                                   // NEW

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmad.smartschedulebuilder.model.TimeSlot;      // NEW

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ===== NEW: temporary test for TimeSlot =====
        TimeSlot a = new TimeSlot(0, TimeSlot.toMinutes(10, 0),  TimeSlot.toMinutes(11, 15));
        TimeSlot b = new TimeSlot(0, TimeSlot.toMinutes(11, 0),  TimeSlot.toMinutes(12, 0));
        TimeSlot c = new TimeSlot(0, TimeSlot.toMinutes(11, 15), TimeSlot.toMinutes(12, 30));
        TimeSlot d = new TimeSlot(1, TimeSlot.toMinutes(10, 0),  TimeSlot.toMinutes(11, 15));

        Log.d("TEST", a + " vs " + b + " = " + a.overlapsWith(b)); // expected true
        Log.d("TEST", a + " vs " + c + " = " + a.overlapsWith(c)); // expected false
        Log.d("TEST", a + " vs " + d + " = " + a.overlapsWith(d)); // expected false
        // ===== END of test =====

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}