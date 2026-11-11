package com.ahmad.smartschedulebuilder;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ===== Temporary test for Section and Course =====
        Course csci = new Course("CSCI 410", "Mobile Apps");
        Section csciA = csci.addSection("A");
        csciA.addSlot(new TimeSlot(0, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Mon
        csciA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Wed

        Course math = new Course("MATH 210", "Calculus II");
        Section mathA = math.addSection("A");
        mathA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(11, 0), TimeSlot.toMinutes(12, 0)));  // Wed, clashes
        Section mathB = math.addSection("B");
        mathB.addSlot(new TimeSlot(1, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Tue, no clash

        Log.d("TEST", csci.toString());
        Log.d("TEST", math.toString());
        Log.d("TEST", "CSCI A vs MATH A = " + csciA.conflictsWith(mathA)); // expected true
        Log.d("TEST", "CSCI A vs MATH B = " + csciA.conflictsWith(mathB)); // expected false
        // ===== END of test =====

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}