import 'dart:collection';

import 'time_slot.dart';

/// One section of a course (e.g. CSCI 410 - Section A).
/// A section meets one or more times per week.
class Section {
  final String courseCode; // e.g. "CSCI 410"
  final String name; // e.g. "A"
  final List<TimeSlot> _slots = [];

  Section(this.courseCode, this.name);

  void addSlot(TimeSlot slot) => _slots.add(slot);

  /// Read-only view, so nobody can change the slots from outside.
  List<TimeSlot> get slots => UnmodifiableListView(_slots);

  /// True if ANY slot of this section overlaps ANY slot of the other section.
  bool conflictsWith(Section other) {
    for (final mine in _slots) {
      for (final theirs in other._slots) {
        if (mine.overlapsWith(theirs)) {
          return true; // one clash is enough
        }
      }
    }
    return false;
  }

  @override
  String toString() => '$courseCode ($name) $_slots';
}
