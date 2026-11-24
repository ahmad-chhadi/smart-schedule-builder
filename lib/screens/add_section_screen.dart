import 'package:material_ui/material_ui.dart';

import '../data/app_data.dart';
import '../l10n/strings.dart';
import '../models/course.dart';
import '../models/section.dart';
import '../models/time_slot.dart';
import '../ui/format.dart';
import '../ui/widgets.dart';

/// Page 2: add sections (days + times) to ONE course.
class AddSectionScreen extends StatefulWidget {
  const AddSectionScreen({super.key, required this.course});

  final Course course;

  @override
  State<AddSectionScreen> createState() => _AddSectionScreenState();
}

class _AddSectionScreenState extends State<AddSectionScreen> {
  final _nameController = TextEditingController();
  final _selectedDays = <int>{}; // 0 = Mon ... 5 = Sat
  int? _startMinute; // null = not chosen yet
  int? _endMinute;

  Course get _course => widget.course;

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  /// Opens a clock dialog. isStart = true for the start time, false for the end time.
  Future<void> _pickTime(bool isStart) async {
    final current = isStart ? _startMinute : _endMinute;
    final picked = await pickMinutes(context, current ?? 8 * 60);
    if (picked == null || !mounted) return;
    setState(() {
      if (isStart) {
        _startMinute = picked;
      } else {
        _endMinute = picked;
      }
    });
  }

  void _addSection() {
    final s = S.of(context);

    // 1) Validate the input
    final name = _nameController.text.trim().toUpperCase();
    if (name.isEmpty) {
      showMessage(context, s.t('msgEnterSectionName'));
      return;
    }
    if (_selectedDays.isEmpty) {
      showMessage(context, s.t('msgChooseDay'));
      return;
    }
    final start = _startMinute;
    final end = _endMinute;
    if (start == null || end == null) {
      showMessage(context, s.t('msgChooseTimes'));
      return;
    }
    if (end <= start) {
      showMessage(context, s.t('msgEndAfterStart'));
      return;
    }

    // 2) Find the section if it already exists
    Section? section;
    for (final existing in _course.sections) {
      if (existing.name == name) {
        section = existing;
      }
    }

    // 3) Build the new time slots and make sure they do not overlap the section's own times
    final days = _selectedDays.toList()..sort();
    final newSlots = <TimeSlot>[];
    for (final day in days) {
      final slot = TimeSlot(day, start, end);
      if (section != null) {
        for (final existing in section.slots) {
          if (existing.overlapsWith(slot)) {
            showMessage(context, s.f('msgSectionOverlaps', [name, formatSlot(s, existing)]));
            return;
          }
        }
      }
      newSlots.add(slot);
    }

    // 4) Create the section if it is new, then add the slots
    setState(() {
      final target = section ?? _course.addSection(name);
      for (final slot in newSlots) {
        target.addSlot(slot);
      }
      AppData.clearResults();

      // 5) Reset the form
      _nameController.clear();
      _selectedDays.clear();
    });
    showMessage(context, s.f('msgSectionAdded', [name]));
  }

  void _removeSection(Section section) {
    setState(() {
      _course.removeSection(section);
      AppData.clearResults();
    });
    showMessage(context, S.of(context).f('msgSectionRemoved', [section.name]));
  }

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);
    final start = _startMinute;
    final end = _endMinute;

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            PageHeader(
              title: s.t('addSectionsTitle'),
              subtitle: s.f('courseRowTitle', [_course.code, _course.title]),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  TextField(
                    controller: _nameController,
                    textCapitalization: TextCapitalization.characters,
                    decoration: InputDecoration(labelText: s.t('hintSectionName')),
                  ),
                  SectionHeading(s.t('daysLabel'), top: 16),
                  // Chips wrap to a new line by themselves, so they fit any screen or font size
                  Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    children: [
                      for (var day = 0; day < s.dayNames.length; day++)
                        FilterChip(
                          label: Text(s.dayNames[day]),
                          selected: _selectedDays.contains(day),
                          onSelected: (selected) => setState(() {
                            if (selected) {
                              _selectedDays.add(day);
                            } else {
                              _selectedDays.remove(day);
                            }
                          }),
                        ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => _pickTime(true),
                          icon: const Icon(Icons.schedule),
                          label: Text(start == null
                              ? s.t('btnStartTime')
                              : s.f('btnStartTimeValue', [formatClock(start)])),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () => _pickTime(false),
                          icon: const Icon(Icons.schedule),
                          label: Text(end == null
                              ? s.t('btnEndTime')
                              : s.f('btnEndTimeValue', [formatClock(end)])),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  FilledButton.icon(
                    onPressed: _addSection,
                    icon: const Icon(Icons.add),
                    label: Text(s.t('btnAddSection')),
                  ),
                  SectionHeading(s.t('sectionsOfCourse')),
                  if (_course.sections.isEmpty) HintText(s.t('noSections')),
                  for (final section in _course.sections)
                    ItemCard(
                      title: s.f('sectionTitle', [section.name]),
                      details: formatSlots(s, section.slots),
                      onLongPress: () => _removeSection(section),
                    ),
                  HintText(s.t('sectionsTip'), size: 12),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.check),
                  label: Text(s.t('btnDone')),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
