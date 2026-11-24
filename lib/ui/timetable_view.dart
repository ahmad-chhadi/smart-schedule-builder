import 'dart:math' as math;

import 'package:material_ui/material_ui.dart';

import '../l10n/strings.dart';
import '../models/section.dart';
import 'format.dart';

/// One color per course (dark enough for white text).
const List<Color> courseColors = [
  Color(0xFFC2410C), // orange
  Color(0xFF1D4ED8), // blue
  Color(0xFF047857), // green
  Color(0xFF7C3AED), // purple
  Color(0xFFBE185D), // pink
  Color(0xFF0E7490), // teal
  Color(0xFFB45309), // brown
  Color(0xFF4D7C0F), // olive
];

/// The color used for the course at this position in the schedule.
Color colorForCourse(int index) => courseColors[index % courseColors.length];

/// One colored rectangle on the grid, prepared before drawing.
class _Block {
  _Block(this.day, this.startMinute, this.endMinute, this.color, this.lines);

  final int day;
  final int startMinute;
  final int endMinute;
  final Color color;
  final List<String> lines; // e.g. ["CSCI", "410", "Sec A", "10:00"]
}

/// Draws one schedule as a weekly grid:
/// days across the top, hours down the side, one colored block per class.
class TimetableView extends StatelessWidget {
  const TimetableView({super.key, required this.schedule});

  final List<Section> schedule;

  static const double headerHeight = 32; // the day-name row
  static const double labelWidth = 44; // the hour-label column
  static const double hourHeight = 64; // one hour

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);
    final scheme = Theme.of(context).colorScheme;

    // 1) Work out which hours and days to show
    var minStart = 24 * 60;
    var maxEnd = 0;
    var hasSaturday = false;
    final blocks = <_Block>[];

    for (var i = 0; i < schedule.length; i++) {
      final section = schedule[i];
      final sectionLine = s.f('blockSection', [section.name]);
      for (final slot in section.slots) {
        // Each word of the course code gets its own line ("CSCI" / "410")
        final lines = [
          ...section.courseCode.split(' '),
          sectionLine,
          formatClock(slot.startMinute),
        ];
        blocks.add(_Block(slot.day, slot.startMinute, slot.endMinute, colorForCourse(i), lines));
        minStart = math.min(minStart, slot.startMinute);
        maxEnd = math.max(maxEnd, slot.endMinute);
        if (slot.day == 5) hasSaturday = true;
      }
    }

    final firstHour = maxEnd == 0 ? 8 : minStart ~/ 60; // round down to the hour
    final lastHour = maxEnd == 0 ? 17 : (maxEnd + 59) ~/ 60; // round up to the hour
    final height = headerHeight + (lastHour - firstHour) * hourHeight + 8;

    // 2) Draw it
    return SizedBox(
      width: double.infinity,
      height: height,
      child: CustomPaint(
        painter: _TimetablePainter(
          blocks: blocks,
          firstHour: firstHour,
          lastHour: lastHour,
          dayCount: hasSaturday ? 6 : 5,
          dayNames: s.dayNames,
          gridColor: scheme.outline.withValues(alpha: 0.3),
          labelColor: scheme.onSurfaceVariant,
        ),
      ),
    );
  }
}

class _TimetablePainter extends CustomPainter {
  _TimetablePainter({
    required this.blocks,
    required this.firstHour,
    required this.lastHour,
    required this.dayCount,
    required this.dayNames,
    required this.gridColor,
    required this.labelColor,
  });

  final List<_Block> blocks;
  final int firstHour;
  final int lastHour;
  final int dayCount;
  final List<String> dayNames;
  final Color gridColor;
  final Color labelColor;

  @override
  void paint(Canvas canvas, Size size) {
    const left = TimetableView.labelWidth;
    const top = TimetableView.headerHeight;
    const hourHeight = TimetableView.hourHeight;
    final dayWidth = (size.width - left) / dayCount;
    final gridBottom = top + (lastHour - firstHour) * hourHeight;

    final gridPaint = Paint()
      ..color = gridColor
      ..strokeWidth = 1;
    final labelStyle = TextStyle(fontSize: 12, color: labelColor, fontFamily: 'Poppins');
    const blockStyle = TextStyle(
      fontSize: 11,
      color: Color(0xFFFFFFFF),
      fontWeight: FontWeight.bold,
      fontFamily: 'Poppins',
    );

    // 1) Day names across the top
    for (var d = 0; d < dayCount; d++) {
      final centerX = left + d * dayWidth + dayWidth / 2;
      _drawText(canvas, dayNames[d], labelStyle, Offset(centerX, top - 22), center: true);
    }

    // 2) Hour lines and hour labels down the side
    for (var h = firstHour; h <= lastHour; h++) {
      final y = top + (h - firstHour) * hourHeight;
      canvas.drawLine(Offset(left, y), Offset(size.width, y), gridPaint);
      _drawText(canvas, formatClock(h * 60), labelStyle, Offset(2, y - 8));
    }

    // 3) Vertical lines between days
    for (var d = 0; d <= dayCount; d++) {
      final x = left + d * dayWidth;
      canvas.drawLine(Offset(x, top), Offset(x, gridBottom), gridPaint);
    }

    // 4) One colored block for every class
    for (final b in blocks) {
      // Convert day and time into pixel positions
      final x1 = left + b.day * dayWidth + 2;
      final y1 = top + (b.startMinute - firstHour * 60) / 60 * hourHeight + 1;
      final y2 = top + (b.endMinute - firstHour * 60) / 60 * hourHeight - 1;
      final rect = RRect.fromLTRBR(x1, y1, x1 + dayWidth - 4, y2, const Radius.circular(6));

      canvas.drawRRect(rect, Paint()..color = b.color);

      // Text inside the block (clipped so it never spills outside)
      canvas.save();
      canvas.clipRRect(rect);
      var textY = y1 + 4;
      for (final line in b.lines) {
        textY += _drawText(canvas, line, blockStyle, Offset(x1 + 4, textY));
      }
      canvas.restore();
    }
  }

  /// Draws one line of text and returns its height.
  double _drawText(Canvas canvas, String text, TextStyle style, Offset at, {bool center = false}) {
    final painter = TextPainter(
      text: TextSpan(text: text, style: style),
      textDirection: TextDirection.ltr,
    )..layout();
    final dx = center ? at.dx - painter.width / 2 : at.dx;
    painter.paint(canvas, Offset(dx, at.dy));
    final height = painter.height;
    painter.dispose();
    return height;
  }

  @override
  bool shouldRepaint(covariant _TimetablePainter oldDelegate) => true;
}
