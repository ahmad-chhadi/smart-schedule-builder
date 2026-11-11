package com.ahmad.smartschedulebuilder;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ahmad.smartschedulebuilder.logic.ScheduleGenerator;
import com.ahmad.smartschedulebuilder.model.Course;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // ===== Temporary test for ScheduleGenerator =====
        Course csci = new Course("CSCI 410", "Mobile Apps");
        Section csciA = csci.addSection("A");
        csciA.addSlot(new TimeSlot(0, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Mon
        csciA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Wed
        Section csciB = csci.addSection("B");
        csciB.addSlot(new TimeSlot(1, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Tue
        csciB.addSlot(new TimeSlot(3, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Thu

        Course math = new Course("MATH 210", "Calculus II");
        Section mathA = math.addSection("A");
        mathA.addSlot(new TimeSlot(2, TimeSlot.toMinutes(11, 0), TimeSlot.toMinutes(12, 0)));  // Wed
        Section mathB = math.addSection("B");
        mathB.addSlot(new TimeSlot(1, TimeSlot.toMinutes(10, 0), TimeSlot.toMinutes(11, 15))); // Tue

        List<Course> courses = new ArrayList<>();
        courses.add(csci);
        courses.add(math);

        List<List<Section>> schedules = ScheduleGenerator.generate(courses);
        Log.d("TEST", "Found " + schedules.size() + " schedules");
        for (List<Section> schedule : schedules) {
            Log.d("TEST", schedule.toString());
        }
        // ===== END of test =====

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}