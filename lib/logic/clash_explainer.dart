import '../models/course.dart';

/// Explains WHY no schedule could be found.
/// It only finds the problem; the screens turn it into a message in the
/// app's language.
class ClashExplainer {
  /// Every pair of courses that can never be taken together.
  static List<(Course, Course)> alwaysClashingPairs(List<Course> courses) {
    final pairs = <(Course, Course)>[];
    for (var i = 0; i < courses.length; i++) {
      for (var j = i + 1; j < courses.length; j++) {
        if (_alwaysClash(courses[i], courses[j])) {
          pairs.add((courses[i], courses[j]));
        }
      }
    }
    return pairs;
  }

  /// True if EVERY section of a clashes with EVERY section of b.
  static bool _alwaysClash(Course a, Course b) {
    for (final sa in a.sections) {
      for (final sb in b.sections) {
        if (!sa.conflictsWith(sb)) {
          return false; // found one pair that fits -> not "always"
        }
      }
    }
    return true;
  }
}
