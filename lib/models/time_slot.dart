/// One lecture time: a day of the week with a start and an end time.
///
/// Times are stored as minutes from midnight (10:30 -> 630) so they are easy
/// to compare.
class TimeSlot {
  /// Day names used by [toString]. Index 0 = Monday ... 5 = Saturday.
  static const List<String> dayNames = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

  final int day; // 0 = Mon ... 5 = Sat
  final int startMinute; // minutes from midnight
  final int endMinute; // minutes from midnight

  TimeSlot(this.day, this.startMinute, this.endMinute) {
    if (day < 0 || day >= dayNames.length) {
      throw ArgumentError('Invalid day: $day');
    }
    if (endMinute <= startMinute) {
      throw ArgumentError('End time must be after start time');
    }
  }

  /// Converts a clock time to minutes, e.g. toMinutes(10, 30) = 630.
  static int toMinutes(int hour, int minute) => hour * 60 + minute;

  /// Formats minutes as "HH:MM", e.g. 630 -> "10:30".
  static String formatTime(int minutes) {
    final hours = (minutes ~/ 60).toString().padLeft(2, '0');
    final mins = (minutes % 60).toString().padLeft(2, '0');
    return '$hours:$mins';
  }

  /// True if this slot and the other slot happen at the same time.
  bool overlapsWith(TimeSlot other) {
    if (day != other.day) {
      return false; // different days can never clash
    }
    return startMinute < other.endMinute && other.startMinute < endMinute;
  }

  @override
  String toString() =>
      '${dayNames[day]} ${formatTime(startMinute)}-${formatTime(endMinute)}';
}
