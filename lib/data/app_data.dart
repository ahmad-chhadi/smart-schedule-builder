import 'package:flutter/widgets.dart';

import '../logic/schedule_ranker.dart';
import '../models/course.dart';
import '../models/section.dart';

/// Data shared by all screens of the app.
///
/// It lives in memory (RAM) only while the app is open - it is NOT a database
/// and nothing is saved to the phone or the internet.
class AppData {
  AppData._(); // no objects of this class

  static final List<Course> courses = [];
  static final Preferences prefs = Preferences();

  // Results of the last "Generate"
  static List<List<Section>> results = [];
  static int lastTotalFound = -1; // -1 = Generate not pressed yet

  // Why nothing fits (filled only when no clash-free schedule exists)
  static List<(Course, Course)> clashPairs = [];

  /// The app's language. Changing it rebuilds the whole app.
  static final ValueNotifier<Locale> locale = ValueNotifier(const Locale('en'));

  /// The course with this code, or null if it does not exist.
  static Course? findCourse(String code) {
    for (final c in courses) {
      if (c.code.toUpperCase() == code.toUpperCase()) {
        return c;
      }
    }
    return null;
  }

  /// Old results are no longer valid when the courses change.
  static void clearResults() {
    results = [];
    lastTotalFound = -1;
    clashPairs = [];
  }
}
