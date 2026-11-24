import 'dart:collection';

import 'section.dart';

/// A course (e.g. CSCI 410 - Mobile Apps) with its available sections.
class Course {
  final String code; // e.g. "CSCI 410"
  final String title; // e.g. "Mobile Apps"
  final List<Section> _sections = [];

  Course(this.code, this.title);

  /// Creates a new section for this course and returns it.
  Section addSection(String sectionName) {
    final section = Section(code, sectionName);
    _sections.add(section);
    return section;
  }

  /// Removes one section from this course.
  void removeSection(Section section) => _sections.remove(section);

  List<Section> get sections => UnmodifiableListView(_sections);

  @override
  String toString() => '$code - $title (${_sections.length} sections)';
}
