import 'package:material_ui/material_ui.dart';

import '../data/app_data.dart';
import '../l10n/strings.dart';
import '../models/course.dart';
import '../models/time_slot.dart';
import '../ui/format.dart';
import '../ui/widgets.dart';
import 'add_section_screen.dart';
import 'prefs_screen.dart';

/// Page 1: add courses and see the list of courses.
class CoursesScreen extends StatefulWidget {
  const CoursesScreen({super.key});

  @override
  State<CoursesScreen> createState() => _CoursesScreenState();
}

class _CoursesScreenState extends State<CoursesScreen> {
  final _codeController = TextEditingController();
  final _titleController = TextEditingController();

  @override
  void dispose() {
    _codeController.dispose();
    _titleController.dispose();
    super.dispose();
  }

  void _addCourse() {
    final s = S.of(context);
    final code = _codeController.text.trim().toUpperCase();
    final title = _titleController.text.trim();

    if (code.isEmpty) {
      showMessage(context, s.t('msgEnterCourseCode'));
      return;
    }
    if (AppData.findCourse(code) != null) {
      showMessage(context, s.f('msgCourseExists', [code]));
      return;
    }

    final course = Course(code, title.isEmpty ? '-' : title);
    AppData.courses.add(course);
    AppData.clearResults();
    _codeController.clear();
    _titleController.clear();

    // Go straight to adding sections for the new course
    _openAddSection(course);
  }

  /// Opens the Add Sections page for one course, then refreshes this page.
  Future<void> _openAddSection(Course course) async {
    await Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => AddSectionScreen(course: course)),
    );
    if (!mounted) return;
    setState(() {}); // show the new sections
  }

  void _goToPrefs() {
    final s = S.of(context);
    if (AppData.courses.isEmpty) {
      showMessage(context, s.t('msgAddCourseFirst'));
      return;
    }
    for (final c in AppData.courses) {
      if (c.sections.isEmpty) {
        showMessage(context, s.f('msgCourseNoSections', [c.code]));
        return;
      }
    }
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const PrefsScreen()),
    );
  }

  /// Asks for confirmation, then removes every course.
  Future<void> _confirmClearAll() async {
    final s = S.of(context);
    if (AppData.courses.isEmpty) {
      showMessage(context, s.t('msgNothingToClear'));
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(s.t('clearDialogTitle')),
        content: Text(s.t('clearDialogMessage')),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(s.t('cancel')),
          ),
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(s.t('clearDialogConfirm')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() {
      AppData.courses.clear();
      AppData.clearResults();
    });
    showMessage(context, s.t('msgAllRemoved'));
  }

  void _removeCourse(Course course) {
    setState(() {
      AppData.courses.remove(course);
      AppData.clearResults();
    });
    showMessage(context, S.of(context).f('msgCourseRemoved', [course.code]));
  }

  /// Fills in two sample courses (useful for a quick demo). Replaces the current courses.
  void _loadExampleData() {
    TimeSlot slot(int day, int h1, int m1, int h2, int m2) =>
        TimeSlot(day, TimeSlot.toMinutes(h1, m1), TimeSlot.toMinutes(h2, m2));

    final csci = Course('CSCI 410', 'Mobile Apps');
    csci.addSection('A')
      ..addSlot(slot(0, 8, 0, 9, 15)) // Mon
      ..addSlot(slot(2, 8, 0, 9, 15)); // Wed
    csci.addSection('B')
      ..addSlot(slot(1, 11, 0, 12, 15)) // Tue
      ..addSlot(slot(3, 11, 0, 12, 15)); // Thu

    final math = Course('MATH 210', 'Calculus II');
    math.addSection('A')
      ..addSlot(slot(0, 9, 30, 10, 45)) // Mon
      ..addSlot(slot(2, 9, 30, 10, 45)); // Wed
    math.addSection('B')
      ..addSlot(slot(1, 8, 0, 9, 15)) // Tue
      ..addSlot(slot(3, 8, 0, 9, 15)); // Thu

    setState(() {
      AppData.courses
        ..clear()
        ..addAll([csci, math]);
      AppData.clearResults();
    });
    showMessage(context, S.of(context).t('msgExampleLoaded'));
  }

  /// Switches the app between English and Arabic.
  void _switchLanguage() {
    final isArabic = AppData.locale.value.languageCode == 'ar';
    AppData.locale.value = Locale(isArabic ? 'en' : 'ar');
  }

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            PageHeader(
              title: s.t('appName'),
              subtitle: s.t('mainStep'),
              trailing: FilledButton.tonal(
                onPressed: _switchLanguage,
                child: Text(s.t('langSwitch')),
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  SectionHeading(s.t('addCourseTitle'), top: 0),
                  TextField(
                    controller: _codeController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: InputDecoration(labelText: s.t('hintCourseCode')),
                  ),
                  TextField(
                    controller: _titleController,
                    textCapitalization: TextCapitalization.words,
                    decoration: InputDecoration(labelText: s.t('hintCourseTitle')),
                  ),
                  const SizedBox(height: 12),
                  FilledButton.icon(
                    onPressed: _addCourse,
                    icon: const Icon(Icons.add),
                    label: Text(s.t('btnAddCourse')),
                  ),
                  TextButton.icon(
                    onPressed: _loadExampleData,
                    icon: const Icon(Icons.auto_awesome),
                    label: Text(s.t('btnExample')),
                  ),
                  SectionHeading(s.t('yourCourses')),
                  if (AppData.courses.isEmpty) HintText(s.t('noCourses')),
                  for (final course in AppData.courses)
                    ItemCard(
                      title: s.f('courseRowTitle', [course.code, course.title]),
                      details: course.sections.isEmpty
                          ? s.t('courseRowNoSections')
                          : [
                              for (final section in course.sections)
                                s.f('courseRowSection', [section.name, formatSlots(s, section.slots)]),
                            ].join('\n'),
                      onTap: () => _openAddSection(course),
                      onLongPress: () => _removeCourse(course),
                    ),
                  HintText(s.t('coursesTip'), size: 12),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: _confirmClearAll,
                    icon: const Icon(Icons.delete_outline),
                    label: Text(s.t('btnClearAll')),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _goToPrefs,
                  icon: const Icon(Icons.arrow_forward),
                  iconAlignment: IconAlignment.end,
                  label: Text(s.t('btnGoPrefs')),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
