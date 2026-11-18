package com.ahmad.smartschedulebuilder.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import com.ahmad.smartschedulebuilder.R;
import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws one schedule as a weekly grid:
 * days across the top, hours down the side, one colored block per class.
 */
public class TimetableView extends View {

    // One color per course (dark enough for white text)
    private static final int[] COURSE_COLORS = {
            Color.parseColor("#C2410C"),  // orange
            Color.parseColor("#1D4ED8"),  // blue
            Color.parseColor("#047857"),  // green
            Color.parseColor("#7C3AED"),  // purple
            Color.parseColor("#BE185D"),  // pink
            Color.parseColor("#0E7490"),  // teal
            Color.parseColor("#B45309"),  // brown
            Color.parseColor("#4D7C0F")   // olive
    };

    // Sizes in dp (converted to pixels in the constructor)
    private static final float HEADER_DP = 32;       // height of the day-name row
    private static final float LABEL_WIDTH_DP = 44;  // width of the hour-label column
    private static final float HOUR_HEIGHT_DP = 64;  // height of one hour

    /** One colored rectangle on the grid, prepared before drawing. */
    private static final class Block {
        final int day;
        final int startMinute;
        final int endMinute;
        final int color;
        final String[] lines;       // e.g. {"CSCI", "410", "Sec A", "10:00"}

        Block(TimeSlot slot, int color, String courseLine, String sectionLine) {
            this.day = slot.getDay();
            this.startMinute = slot.getStartMinute();
            this.endMinute = slot.getEndMinute();
            this.color = color;
            // Each word of the course code gets its own line so it fits in narrow day columns
            String[] codeWords = courseLine.split(" ");
            this.lines = new String[codeWords.length + 2];
            System.arraycopy(codeWords, 0, lines, 0, codeWords.length);
            lines[codeWords.length] = sectionLine;
            lines[codeWords.length + 1] = UiUtils.formatClock(slot.getStartMinute());
        }
    }

    private final float dp;   // 1 dp in pixels on this phone
    private final String[] dayNames;   // from strings.xml, so they follow the phone's language
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();   // reused, so onDraw creates no objects

    // Everything onDraw needs is prepared in setSchedule(), so drawing creates no new objects
    private final List<Block> blocks = new ArrayList<>();
    private String[] hourLabels = new String[0];
    private int firstHour = 8;    // first hour shown on the grid
    private int lastHour = 17;    // grid ends at this hour
    private int dayCount = 5;     // Mon-Fri, or Mon-Sat if a class is on Saturday

    /** Used when the view is created from XML (activity_schedules.xml). */
    public TimetableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        dayNames = getResources().getStringArray(R.array.day_names);

        // Grid and label colors come from the app theme, so they work in light and dark mode
        gridPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline));
        gridPaint.setAlpha(70);
        gridPaint.setStrokeWidth(dp);

        labelPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        labelPaint.setTextSize(sp(12));

        // Same font as the rest of the app (falls back to the default font if it cannot load)
        Typeface appFont = ResourcesCompat.getFont(context, R.font.poppins);
        labelPaint.setTypeface(appFont);

        blockTextPaint.setColor(Color.WHITE);
        blockTextPaint.setTextSize(sp(11));
        blockTextPaint.setTypeface(Typeface.create(appFont, Typeface.BOLD));

        buildHourLabels();
    }

    /** The color used for the course at this position in the schedule. */
    public static int colorForCourse(int index) {
        return COURSE_COLORS[index % COURSE_COLORS.length];
    }

    /** Shows a new schedule: works out the hours, days and blocks to display, then redraws. */
    public void setSchedule(List<Section> schedule) {
        blocks.clear();
        int minStart = 24 * 60;
        int maxEnd = 0;
        boolean hasSaturday = false;

        for (int i = 0; i < schedule.size(); i++) {
            Section section = schedule.get(i);
            String sectionLine = getContext().getString(R.string.block_section, section.getName());
            for (TimeSlot slot : section.getSlots()) {
                blocks.add(new Block(slot, colorForCourse(i), section.getCourseCode(), sectionLine));
                minStart = Math.min(minStart, slot.getStartMinute());
                maxEnd = Math.max(maxEnd, slot.getEndMinute());
                if (slot.getDay() == 5) {
                    hasSaturday = true;
                }
            }
        }

        if (maxEnd == 0) {          // empty schedule
            firstHour = 8;
            lastHour = 17;
        } else {
            firstHour = minStart / 60;          // round down to the hour
            lastHour = (maxEnd + 59) / 60;      // round up to the hour
        }
        dayCount = hasSaturday ? 6 : 5;
        buildHourLabels();

        requestLayout();   // the height may change -> measure again
        invalidate();      // draw again
    }

    /** Prepares the "08:00", "09:00", ... labels for the current hour range. */
    private void buildHourLabels() {
        hourLabels = new String[lastHour - firstHour + 1];
        for (int h = firstHour; h <= lastHour; h++) {
            hourLabels[h - firstHour] = String.format(Locale.US, "%02d:00", h);
        }
    }

    /** Tells Android how big this view wants to be. */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (HEADER_DP * dp + (lastHour - firstHour) * HOUR_HEIGHT_DP * dp + 8 * dp);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = LABEL_WIDTH_DP * dp;
        float top = HEADER_DP * dp;
        float hourHeight = HOUR_HEIGHT_DP * dp;
        float dayWidth = (getWidth() - left) / dayCount;
        float gridBottom = top + (lastHour - firstHour) * hourHeight;

        // 1) Day names across the top
        labelPaint.setTextAlign(Paint.Align.CENTER);
        for (int d = 0; d < dayCount; d++) {
            float centerX = left + d * dayWidth + dayWidth / 2;
            canvas.drawText(dayNames[d], centerX, top - 10 * dp, labelPaint);
        }

        // 2) Hour lines and hour labels down the side
        labelPaint.setTextAlign(Paint.Align.LEFT);
        for (int h = firstHour; h <= lastHour; h++) {
            float y = top + (h - firstHour) * hourHeight;
            canvas.drawLine(left, y, getWidth(), y, gridPaint);
            canvas.drawText(hourLabels[h - firstHour], 4 * dp, y + 4 * dp, labelPaint);
        }

        // 3) Vertical lines between days
        for (int d = 0; d <= dayCount; d++) {
            float x = left + d * dayWidth;
            canvas.drawLine(x, top, x, gridBottom, gridPaint);
        }

        // 4) One colored block for every class
        float lineHeight = blockTextPaint.getTextSize() * 1.25f;
        for (Block b : blocks) {
            // Convert day and time into pixel positions
            float x1 = left + b.day * dayWidth + 2 * dp;
            float x2 = x1 + dayWidth - 4 * dp;
            float y1 = top + (b.startMinute - firstHour * 60) / 60f * hourHeight + dp;
            float y2 = top + (b.endMinute - firstHour * 60) / 60f * hourHeight - dp;

            blockPaint.setColor(b.color);
            rect.set(x1, y1, x2, y2);
            canvas.drawRoundRect(rect, 6 * dp, 6 * dp, blockPaint);

            // Text inside the block (clipped so it never spills outside)
            canvas.save();
            canvas.clipRect(rect);
            float textX = x1 + 4 * dp;
            float textY = y1 + lineHeight;
            for (String line : b.lines) {
                canvas.drawText(line, textX, textY, blockTextPaint);
                textY += lineHeight;
            }
            canvas.restore();
        }
    }

    /** Converts sp (text size unit) to pixels. */
    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
