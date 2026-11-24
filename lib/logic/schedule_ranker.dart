import 'dart:math' as math;

import '../models/section.dart';
import '../models/time_slot.dart';

/// How schedules are sorted (same order as the dropdown on the Preferences page).
enum RankBy { fewestGaps, fewestDays, latestStart }

/// The user's choices on the Preferences page. Defaults = no filter.
class Preferences {
  int earliestStart = 0; // no class may start before this (minutes)
  int latestEnd = 24 * 60; // no class may end after this (minutes)
  bool wantFreeDay = false; // at least one weekday (Mon-Fri) without classes
  int maxHoursPerDay = 24; // max class hours in one day
  RankBy rankBy = RankBy.fewestGaps;

  /// Back to "no filter".
  void reset() {
    earliestStart = 0;
    latestEnd = 24 * 60;
    wantFreeDay = false;
    maxHoursPerDay = 24;
    rankBy = RankBy.fewestGaps;
  }
}

/// Filters schedules by the user's preferences and sorts them best-first.
class ScheduleRanker {
  static const int _daysInWeek = 6; // Mon ... Sat
  static const int _workDays = 5; // Mon ... Fri (used for "free day")

  /// Keeps the schedules that pass every filter, sorted best-first.
  /// Lower score = better. Equal scores keep their original order.
  static List<List<Section>> filterAndSort(
    List<List<Section>> schedules,
    Preferences prefs,
  ) {
    final kept = schedules.where((s) => _passesFilters(s, prefs)).toList();
    final scores = [for (final s in kept) _score(s, prefs.rankBy)];

    final order = List<int>.generate(kept.length, (i) => i);
    order.sort((a, b) {
      final byScore = scores[a].compareTo(scores[b]);
      return byScore != 0 ? byScore : a.compareTo(b);
    });
    return [for (final i in order) kept[i]];
  }

  static bool _passesFilters(List<Section> schedule, Preferences prefs) {
    final minutesPerDay = List<int>.filled(_daysInWeek, 0);

    for (final slot in _allSlots(schedule)) {
      if (slot.startMinute < prefs.earliestStart) return false;
      if (slot.endMinute > prefs.latestEnd) return false;
      minutesPerDay[slot.day] += slot.endMinute - slot.startMinute;
    }

    for (final minutes in minutesPerDay) {
      if (minutes > prefs.maxHoursPerDay * 60) return false;
    }

    if (prefs.wantFreeDay) {
      final hasFreeDay = minutesPerDay.take(_workDays).any((m) => m == 0);
      if (!hasFreeDay) return false;
    }
    return true;
  }

  static int _score(List<Section> schedule, RankBy rankBy) {
    switch (rankBy) {
      case RankBy.fewestDays:
        // Fewer days first; if equal, fewer gaps first
        return countDays(schedule) * 10000 + countGapMinutes(schedule);
      case RankBy.latestStart:
        // Later first class = better, so use a negative number
        return -earliestStart(schedule);
      case RankBy.fewestGaps:
        return countGapMinutes(schedule);
    }
  }

  /// Total free minutes between classes on the same day.
  static int countGapMinutes(List<Section> schedule) {
    var total = 0;
    final slots = _allSlots(schedule);
    for (var day = 0; day < _daysInWeek; day++) {
      final daySlots = slots.where((s) => s.day == day).toList()
        ..sort((a, b) => a.startMinute.compareTo(b.startMinute));
      for (var i = 1; i < daySlots.length; i++) {
        final gap = daySlots[i].startMinute - daySlots[i - 1].endMinute;
        if (gap > 0) {
          total += gap;
        }
      }
    }
    return total;
  }

  /// Number of days with at least one class.
  static int countDays(List<Section> schedule) =>
      _allSlots(schedule).map((s) => s.day).toSet().length;

  /// Start time of the earliest class in the week.
  static int earliestStart(List<Section> schedule) {
    var earliest = 24 * 60;
    for (final slot in _allSlots(schedule)) {
      earliest = math.min(earliest, slot.startMinute);
    }
    return earliest;
  }

  static List<TimeSlot> _allSlots(List<Section> schedule) =>
      [for (final section in schedule) ...section.slots];
}
