package com.ahmad.smartschedulebuilder.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.ahmad.smartschedulebuilder.model.Section;
import com.ahmad.smartschedulebuilder.model.TimeSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws one schedule as a weekly grid:
 * days across the top, hours down the side, one colored block per class.
 */
public class TimetableView extends View {

    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

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

    private final float dp;   // 1 dp in pixels on this phone
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();   // reused, so onDraw creates fewer objects

    private List<Section> schedule = new ArrayList<>();
    private int firstHour = 8;    // first hour shown on the grid
    private int lastHour = 17;    // grid ends at this hour
    private int dayCount = 5;     // Mon-Fri, or Mon-Sat if a class is on Saturday

    public TimetableView(Context context) {
        this(context, null);
    }

    /** This constructor is used when the view is created from XML. */
    public TimetableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;

        gridPaint.setColor(Color.parseColor("#DDDDDD"));
        gridPaint.setStrokeWidth(1 * dp);

        labelPaint.setColor(Color.parseColor("#666666"));
        labelPaint.setTextSize(sp(12));

        blockTextPaint.setColor(Color.WHITE);
        blockTextPaint.setTextSize(sp(11));
        blockTextPaint.setFakeBoldText(true);
    }

    /** The color used for the course at this position in the schedule. */
    public static int colorForCourse(int index) {
        return COURSE_COLORS[index % COURSE_COLORS.length];
    }

    /** Shows a new schedule: works out the hours and days to display, then redraws. */
    public void setSchedule(List<Section> schedule) {
        this.schedule = schedule;

        int minStart = 24 * 60;
        int maxEnd = 0;
        boolean hasSaturday = false;
        for (Section s : schedule) {
            for (TimeSlot t : s.getSlots()) {
                minStart = Math.min(minStart, t.getStartMinute());
                maxEnd = Math.max(maxEnd, t.getEndMinute());
                if (t.getDay() == 5) {
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

        requestLayout();   // the height may change -> measure again
        invalidate();      // draw again
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
            canvas.drawText(DAY_NAMES[d], centerX, top - 10 * dp, labelPaint);
        }

        // 2) Hour lines and hour labels down the side
        labelPaint.setTextAlign(Paint.Align.LEFT);
        for (int h = firstHour; h <= lastHour; h++) {
            float y = top + (h - firstHour) * hourHeight;
            canvas.drawLine(left, y, getWidth(), y, gridPaint);
            canvas.drawText(String.format(Locale.US, "%02d:00", h), 4 * dp, y + 4 * dp, labelPaint);
        }

        // 3) Vertical lines between days
        for (int d = 0; d <= dayCount; d++) {
            float x = left + d * dayWidth;
            canvas.drawLine(x, top, x, gridBottom, gridPaint);
        }

        // 4) One colored block for every class
        float lineHeight = blockTextPaint.getTextSize() * 1.25f;
        for (int i = 0; i < schedule.size(); i++) {
            Section section = schedule.get(i);
            blockPaint.setColor(colorForCourse(i));

            for (TimeSlot t : section.getSlots()) {
                if (t.getDay() >= dayCount) {
                    continue;
                }
                // Convert day and time into pixel positions
                float x1 = left + t.getDay() * dayWidth + 2 * dp;
                float x2 = x1 + dayWidth - 4 * dp;
                float y1 = top + (t.getStartMinute() - firstHour * 60) / 60f * hourHeight + 1 * dp;
                float y2 = top + (t.getEndMinute() - firstHour * 60) / 60f * hourHeight - 1 * dp;

                rect.set(x1, y1, x2, y2);
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, blockPaint);

                // Text inside the block (clipped so it never spills outside)
                canvas.save();
                canvas.clipRect(rect);
                float textX = x1 + 4 * dp;
                float textY = y1 + lineHeight;
                canvas.drawText(section.getCourseCode(), textX, textY, blockTextPaint);
                canvas.drawText("Sec " + section.getName(), textX, textY + lineHeight, blockTextPaint);
                canvas.drawText(UiUtils.formatClock(t.getStartMinute()), textX, textY + 2 * lineHeight, blockTextPaint);
                canvas.restore();
            }
        }
    }

    /** Converts sp (text size unit) to pixels. */
    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}