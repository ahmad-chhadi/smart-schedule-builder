import '../l10n/strings.dart';
import '../models/time_slot.dart';

/// 630 -> "10:30"
String formatClock(int minutes) => TimeSlot.formatTime(minutes);

/// One time slot in the app's language, e.g. "Mon 10:00–11:15".
String formatSlot(S s, TimeSlot slot) => s.f('slotFormat', [
      s.dayNames[slot.day],
      formatClock(slot.startMinute),
      formatClock(slot.endMinute),
    ]);

/// Several time slots joined with commas.
String formatSlots(S s, List<TimeSlot> slots) =>
    slots.map((slot) => formatSlot(s, slot)).join(s.t('slotSeparator'));

/// 75 -> "1h 15m"
String formatDuration(S s, int minutes) {
  final h = minutes ~/ 60;
  final m = minutes % 60;
  if (h == 0) return s.f('durationMinutes', [m]);
  if (m == 0) return s.f('durationHours', [h]);
  return s.f('durationHoursMinutes', [h, m]);
}
