import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../data/app_data.dart';
import '../l10n/strings.dart';
import '../logic/clash_explainer.dart';
import '../logic/schedule_generator.dart';
import '../logic/schedule_ranker.dart';
import '../ui/format.dart';
import '../ui/widgets.dart';
import 'schedules_screen.dart';

/// Page 3: filters and ranking, then "Generate".
class PrefsScreen extends StatefulWidget {
  const PrefsScreen({super.key});

  @override
  State<PrefsScreen> createState() => _PrefsScreenState();
}

class _PrefsScreenState extends State<PrefsScreen> {
  final _maxHoursController = TextEditingController();

  Preferences get _prefs => AppData.prefs;

  @override
  void initState() {
    super.initState();
    // 24 hours = no limit, so the box stays empty
    if (_prefs.maxHoursPerDay < 24) {
      _maxHoursController.text = '${_prefs.maxHoursPerDay}';
    }
  }

  @override
  void dispose() {
    _maxHoursController.dispose();
    super.dispose();
  }

  Future<void> _pickEarliest() async {
    final initial = _prefs.earliestStart > 0 ? _prefs.earliestStart : 8 * 60;
    final picked = await pickMinutes(context, initial);
    if (picked == null || !mounted) return;
    setState(() => _prefs.earliestStart = picked);
  }

  Future<void> _pickLatest() async {
    final initial = _prefs.latestEnd < 24 * 60 ? _prefs.latestEnd : 17 * 60;
    final picked = await pickMinutes(context, initial);
    if (picked == null || !mounted) return;
    setState(() => _prefs.latestEnd = picked);
  }

  void _resetPrefs() {
    setState(() {
      _prefs.reset();
      _maxHoursController.clear();
    });
    showMessage(context, S.of(context).t('msgPrefsReset'));
  }

  void _generate() {
    final s = S.of(context);

    // 1) Check the preferences
    if (_prefs.latestEnd <= _prefs.earliestStart) {
      showMessage(context, s.t('msgLatestAfterEarliest'));
      return;
    }
    final text = _maxHoursController.text.trim();
    if (text.isEmpty) {
      _prefs.maxHoursPerDay = 24; // no limit
    } else {
      final hours = int.tryParse(text);
      if (hours == null || hours < 1 || hours > 24) {
        showMessage(context, s.t('msgMaxHoursRange'));
        return;
      }
      _prefs.maxHoursPerDay = hours;
    }

    // 2) Build every clash-free schedule, then filter and sort them
    final all = ScheduleGenerator.generate(AppData.courses);
    AppData.lastTotalFound = all.length;
    AppData.results = ScheduleRanker.filterAndSort(all, _prefs);
    AppData.clashPairs =
        all.isEmpty ? ClashExplainer.alwaysClashingPairs(AppData.courses) : [];

    // 3) Show them
    Navigator.of(context).push(
      MaterialPageRoute<void>(builder: (_) => const SchedulesScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);
    final earliest = _prefs.earliestStart;
    final latest = _prefs.latestEnd;

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            PageHeader(title: s.t('prefsTitle'), subtitle: s.t('prefsStep')),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  OutlinedButton.icon(
                    onPressed: _pickEarliest,
                    icon: const Icon(Icons.wb_sunny_outlined),
                    label: Text(earliest == 0
                        ? s.t('prefEarliestAny')
                        : s.f('prefEarliestValue', [formatClock(earliest)])),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: _pickLatest,
                    icon: const Icon(Icons.nights_stay_outlined),
                    label: Text(latest == 24 * 60
                        ? s.t('prefLatestAny')
                        : s.f('prefLatestValue', [formatClock(latest)])),
                  ),
                  const SizedBox(height: 8),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(s.t('prefFreeDay')),
                    value: _prefs.wantFreeDay,
                    onChanged: (value) => setState(() => _prefs.wantFreeDay = value),
                  ),
                  TextField(
                    controller: _maxHoursController,
                    keyboardType: TextInputType.number,
                    inputFormatters: [
                      FilteringTextInputFormatter.digitsOnly,
                      LengthLimitingTextInputFormatter(2),
                    ],
                    decoration: InputDecoration(labelText: s.t('hintMaxHours')),
                  ),
                  SectionHeading(s.t('rankBy')),
                  DropdownMenu<RankBy>(
                    // A new key rebuilds the menu, so "Reset" also updates its text
                    key: ValueKey(_prefs.rankBy),
                    expandedInsets: EdgeInsets.zero,
                    initialSelection: _prefs.rankBy,
                    onSelected: (value) {
                      if (value != null) setState(() => _prefs.rankBy = value);
                    },
                    dropdownMenuEntries: [
                      DropdownMenuEntry(value: RankBy.fewestGaps, label: s.t('rankFewestGaps')),
                      DropdownMenuEntry(value: RankBy.fewestDays, label: s.t('rankFewestDays')),
                      DropdownMenuEntry(value: RankBy.latestStart, label: s.t('rankLatestStart')),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextButton.icon(
                    onPressed: _resetPrefs,
                    icon: const Icon(Icons.restart_alt),
                    label: Text(s.t('btnResetPrefs')),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _generate,
                  icon: const Icon(Icons.auto_awesome),
                  label: Text(s.t('btnGenerate')),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
