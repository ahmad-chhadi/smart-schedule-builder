import 'package:flutter_test/flutter_test.dart';
import 'package:smart_schedule_builder/l10n/strings.dart';
import 'package:smart_schedule_builder/logic/clash_explainer.dart';
import 'package:smart_schedule_builder/logic/schedule_generator.dart';
import 'package:smart_schedule_builder/logic/schedule_ranker.dart';
import 'package:smart_schedule_builder/models/course.dart';
import 'package:smart_schedule_builder/models/section.dart';
import 'package:smart_schedule_builder/models/time_slot.dart';

/// Short helper: a slot on [day] from h1:m1 to h2:m2.
TimeSlot slot(int day, int h1, int m1, int h2, int m2) =>
    TimeSlot(day, TimeSlot.toMinutes(h1, m1), TimeSlot.toMinutes(h2, m2));

/// The same two courses as the "Try with example data" button.
List<Course> exampleCourses() {
  final csci = Course('CSCI 410', 'Mobile Apps');
  csci.addSection('A')
    ..addSlot(slot(0, 8, 0, 9, 15))
    ..addSlot(slot(2, 8, 0, 9, 15));
  csci.addSection('B')
    ..addSlot(slot(1, 11, 0, 12, 15))
    ..addSlot(slot(3, 11, 0, 12, 15));

  final math = Course('MATH 210', 'Calculus II');
  math.addSection('A')
    ..addSlot(slot(0, 9, 30, 10, 45))
    ..addSlot(slot(2, 9, 30, 10, 45));
  math.addSection('B')
    ..addSlot(slot(1, 8, 0, 9, 15))
    ..addSlot(slot(3, 8, 0, 9, 15));
  return [csci, math];
}

String names(List<Section> schedule) =>
    schedule.map((s) => '${s.courseCode} ${s.name}').join(', ');

void main() {
  group('TimeSlot', () {
    test('converts and formats times', () {
      expect(TimeSlot.toMinutes(10, 30), 630);
      expect(TimeSlot.formatTime(630), '10:30');
      expect(TimeSlot.formatTime(8 * 60), '08:00');
      expect(slot(0, 10, 0, 11, 15).toString(), 'Mon 10:00-11:15');
    });

    test('rejects a bad day or an end before the start', () {
      expect(() => TimeSlot(6, 600, 700), throwsArgumentError);
      expect(() => TimeSlot(-1, 600, 700), throwsArgumentError);
      expect(() => TimeSlot(0, 700, 700), throwsArgumentError);
      expect(() => TimeSlot(0, 700, 600), throwsArgumentError);
    });

    test('overlap rules', () {
      final a = slot(0, 10, 0, 11, 0);
      expect(a.overlapsWith(slot(0, 10, 30, 11, 30)), isTrue);
      expect(a.overlapsWith(slot(0, 9, 0, 12, 0)), isTrue); // fully inside
      expect(a.overlapsWith(slot(0, 11, 0, 12, 0)), isFalse); // touching is OK
      expect(a.overlapsWith(slot(1, 10, 0, 11, 0)), isFalse); // other day
    });
  });

  group('Section and Course', () {
    test('sections clash when any of their slots overlap', () {
      final a = Section('X', 'A')..addSlot(slot(0, 8, 0, 9, 0));
      final b = Section('Y', 'B')
        ..addSlot(slot(1, 8, 0, 9, 0))
        ..addSlot(slot(0, 8, 30, 9, 30));
      final c = Section('Z', 'C')..addSlot(slot(2, 8, 0, 9, 0));
      expect(a.conflictsWith(b), isTrue);
      expect(a.conflictsWith(c), isFalse);
    });

    test('a course can add and remove sections', () {
      final course = Course('CSCI 410', 'Mobile Apps');
      final a = course.addSection('A');
      course.addSection('B');
      expect(course.sections.length, 2);
      course.removeSection(a);
      expect(course.sections.map((s) => s.name), ['B']);
      expect(a.courseCode, 'CSCI 410');
    });

    test('the lists cannot be changed from outside', () {
      final course = Course('C', 'T');
      final section = course.addSection('A');
      expect(() => course.sections.add(section), throwsUnsupportedError);
      expect(() => section.slots.add(slot(0, 8, 0, 9, 0)), throwsUnsupportedError);
    });
  });

  group('ScheduleGenerator', () {
    test('example data gives 4 clash-free schedules', () {
      final results = ScheduleGenerator.generate(exampleCourses());
      // Every pair fits (CSCI A 8:00-9:15 ends before MATH A 9:30-10:45)
      expect(results.length, 4);
      for (final schedule in results) {
        expect(schedule.length, 2);
        expect(schedule[0].conflictsWith(schedule[1]), isFalse);
      }
    });

    test('no courses gives one empty schedule', () {
      final results = ScheduleGenerator.generate([]);
      expect(results.length, 1);
      expect(results.first, isEmpty);
    });

    test('a course with no sections gives no schedule', () {
      final courses = exampleCourses()..add(Course('EMPTY', '-'));
      expect(ScheduleGenerator.generate(courses), isEmpty);
    });

    test('two courses at the same time give no schedule', () {
      final a = Course('A', '-');
      a.addSection('1').addSlot(slot(0, 8, 0, 9, 0));
      final b = Course('B', '-');
      b.addSection('1').addSlot(slot(0, 8, 30, 9, 30));
      expect(ScheduleGenerator.generate([a, b]), isEmpty);
    });

    test('stops at maxResults', () {
      // 4 courses x 6 sections on different days/times = 1296 combinations
      final courses = <Course>[];
      for (var c = 0; c < 4; c++) {
        final course = Course('C$c', '-');
        for (var s = 0; s < 6; s++) {
          course.addSection('$s').addSlot(slot(s, 8 + c * 2, 0, 9 + c * 2, 0));
        }
        courses.add(course);
      }
      expect(ScheduleGenerator.generate(courses).length, ScheduleGenerator.maxResults);
    });
  });

  group('ScheduleRanker', () {
    test('counts gaps and days', () {
      final a = Section('A', '1')
        ..addSlot(slot(0, 8, 0, 9, 0))
        ..addSlot(slot(2, 8, 0, 9, 0));
      final b = Section('B', '1')..addSlot(slot(0, 10, 30, 11, 0));
      expect(ScheduleRanker.countGapMinutes([a, b]), 90);
      expect(ScheduleRanker.countDays([a, b]), 2);
      expect(ScheduleRanker.earliestStart([a, b]), 8 * 60);
    });

    test('no filters keeps every schedule', () {
      final all = ScheduleGenerator.generate(exampleCourses());
      expect(ScheduleRanker.filterAndSort(all, Preferences()).length, all.length);
    });

    test('earliest 09:00 keeps only CSCI B + MATH A', () {
      final all = ScheduleGenerator.generate(exampleCourses());
      final prefs = Preferences()..earliestStart = 9 * 60;
      final kept = ScheduleRanker.filterAndSort(all, prefs);
      expect(kept.length, 1);
      expect(names(kept.first), 'CSCI 410 B, MATH 210 A');
    });

    test('latest end 10:00 removes MATH A', () {
      final all = ScheduleGenerator.generate(exampleCourses());
      final prefs = Preferences()..latestEnd = 10 * 60;
      final kept = ScheduleRanker.filterAndSort(all, prefs);
      for (final schedule in kept) {
        expect(names(schedule), isNot(contains('MATH 210 A')));
      }
    });

    test('free day filter', () {
      // A Mon-Fri course leaves no free weekday
      final busy = Section('X', '1');
      for (var d = 0; d < 5; d++) {
        busy.addSlot(slot(d, 8, 0, 9, 0));
      }
      final prefs = Preferences()..wantFreeDay = true;
      expect(ScheduleRanker.filterAndSort([[busy]], prefs), isEmpty);
      final light = Section('Y', '1')..addSlot(slot(0, 8, 0, 9, 0));
      expect(ScheduleRanker.filterAndSort([[light]], prefs).length, 1);
    });

    test('max hours per day filter', () {
      final long = Section('X', '1')..addSlot(slot(0, 8, 0, 11, 0)); // 3 h
      final prefs = Preferences()..maxHoursPerDay = 2;
      expect(ScheduleRanker.filterAndSort([[long]], prefs), isEmpty);
      prefs.maxHoursPerDay = 3;
      expect(ScheduleRanker.filterAndSort([[long]], prefs).length, 1);
    });

    test('fewest gaps comes first', () {
      final all = ScheduleGenerator.generate(exampleCourses());
      final sorted = ScheduleRanker.filterAndSort(all, Preferences());
      final gaps = sorted.map(ScheduleRanker.countGapMinutes).toList();
      for (var i = 1; i < gaps.length; i++) {
        expect(gaps[i] >= gaps[i - 1], isTrue);
      }
    });

    test('fewest days and latest start', () {
      final all = ScheduleGenerator.generate(exampleCourses());
      final byDays = ScheduleRanker.filterAndSort(
          all, Preferences()..rankBy = RankBy.fewestDays);
      expect(ScheduleRanker.countDays(byDays.first), 2);

      final byStart = ScheduleRanker.filterAndSort(
          all, Preferences()..rankBy = RankBy.latestStart);
      expect(ScheduleRanker.earliestStart(byStart.first), 9 * 60 + 30);
    });

    test('equal scores keep their original order', () {
      final a = Section('A', '1')..addSlot(slot(0, 8, 0, 9, 0));
      final b = Section('B', '1')..addSlot(slot(1, 8, 0, 9, 0));
      final c = Section('C', '1')..addSlot(slot(2, 8, 0, 9, 0));
      final sorted = ScheduleRanker.filterAndSort([[a], [b], [c]], Preferences());
      expect(sorted.map((s) => s.first.courseCode), ['A', 'B', 'C']);
    });

    test('reset puts back the defaults', () {
      final prefs = Preferences()
        ..earliestStart = 600
        ..wantFreeDay = true
        ..rankBy = RankBy.fewestDays;
      prefs.reset();
      expect(prefs.earliestStart, 0);
      expect(prefs.latestEnd, 24 * 60);
      expect(prefs.wantFreeDay, isFalse);
      expect(prefs.maxHoursPerDay, 24);
      expect(prefs.rankBy, RankBy.fewestGaps);
    });
  });

  group('ClashExplainer', () {
    test('finds the pair that always clashes', () {
      final a = Course('A', '-');
      a.addSection('1').addSlot(slot(0, 8, 0, 9, 0));
      final b = Course('B', '-');
      b.addSection('1').addSlot(slot(0, 8, 30, 9, 30));
      final c = Course('C', '-');
      c.addSection('1').addSlot(slot(1, 8, 0, 9, 0));
      final pairs = ClashExplainer.alwaysClashingPairs([a, b, c]);
      expect(pairs.length, 1);
      expect(pairs.first.$1.code, 'A');
      expect(pairs.first.$2.code, 'B');
    });

    test('no pairs for the example data', () {
      expect(ClashExplainer.alwaysClashingPairs(exampleCourses()), isEmpty);
    });
  });

  group('Strings', () {
    test('English and Arabic have exactly the same keys', () {
      expect(S.ar.keys.toSet(), S.en.keys.toSet());
    });
  });
}
