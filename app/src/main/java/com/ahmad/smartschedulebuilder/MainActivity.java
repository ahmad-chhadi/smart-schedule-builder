package com.ahmad.smartschedulebuilder;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    // The 3 pages of the app
    private View pageCourses;
    private View pagePrefs;
    private View pageSchedules;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Connect Java variables to the views in the XML
        pageCourses = findViewById(R.id.pageCourses);
        pagePrefs = findViewById(R.id.pagePrefs);
        pageSchedules = findViewById(R.id.pageSchedules);

        // Switch pages when a tab is selected
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPage(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /** Shows the page at the given position (0, 1 or 2) and hides the others. */
    private void showPage(int position) {
        pageCourses.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        pagePrefs.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
        pageSchedules.setVisibility(position == 2 ? View.VISIBLE : View.GONE);
    }
}