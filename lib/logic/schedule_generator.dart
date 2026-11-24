import '../models/course.dart';
import '../models/section.dart';

/// Finds every combination of sections (one per course) with no time clashes,
/// using backtracking.
class ScheduleGenerator {
  /// Safety limit so the app never freezes with too many combinations.
  static const int maxResults = 500;

  /// Entry point: returns all valid schedules. Each schedule = one section per course.
  static List<List<Section>> generate(List<Course> courses) {
    final results = <List<Section>>[];
    _backtrack(courses, 0, <Section>[], results);
    return results;
  }

  /// index   = which course we are choosing a section for now
  /// chosen  = sections picked so far (one for each earlier course)
  /// results = where complete schedules are saved
  static void _backtrack(
    List<Course> courses,
    int index,
    List<Section> chosen,
    List<List<Section>> results,
  ) {
    // Stop early if we already have enough results.
    if (results.length >= maxResults) {
      return;
    }

    // 1) BASE CASE: every course already has a section -> save the schedule.
    if (index == courses.length) {
      results.add(List<Section>.of(chosen)); // save a COPY
      return;
    }

    // 2) RECURSIVE CASE: try each section of the current course.
    for (final section in courses[index].sections) {
      if (_fitsWith(section, chosen)) {
        chosen.add(section); // choose
        _backtrack(courses, index + 1, chosen, results); // go to next course
        chosen.removeLast(); // un-choose (backtrack)
      }
    }
  }

  /// True if the candidate section clashes with NONE of the chosen sections.
  static bool _fitsWith(Section candidate, List<Section> chosen) {
    for (final s in chosen) {
      if (candidate.conflictsWith(s)) {
        return false;
      }
    }
    return true;
  }
}
