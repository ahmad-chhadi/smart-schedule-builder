import 'dart:ui' as ui;

import 'package:flutter/rendering.dart';
import 'package:material_ui/material_ui.dart';
import 'package:share_plus/share_plus.dart';

import '../data/app_data.dart';
import '../l10n/strings.dart';
import '../logic/schedule_ranker.dart';
import '../models/section.dart';
import '../ui/format.dart';
import '../ui/timetable_view.dart';
import '../ui/widgets.dart';

/// Page 4: shows the schedules one at a time, best first.
class SchedulesScreen extends StatefulWidget {
  const SchedulesScreen({super.key});

  @override
  State<SchedulesScreen> createState() => _SchedulesScreenState();
}

class _SchedulesScreenState extends State<SchedulesScreen> {
  int _currentIndex = 0;

  // Marks the part of the screen that is saved as an image when sharing
  final _captureKey = GlobalKey();

  List<List<Section>> get _results => AppData.results;

  void _showPrevious() {
    if (_currentIndex > 0) setState(() => _currentIndex--);
  }

  void _showNext() {
    if (_currentIndex < _results.length - 1) setState(() => _currentIndex++);
  }

  /// Turns the current schedule into a PNG picture and opens the phone's share menu.
  Future<void> _share() async {
    final s = S.of(context);
    if (_results.isEmpty) {
      showMessage(context, s.t('msgNothingToShare'));
      return;
    }
    try {
      final boundary =
          _captureKey.currentContext!.findRenderObject()! as RenderRepaintBoundary;
      final image = await boundary.toImage(pixelRatio: 3);
      final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
      image.dispose();
      if (bytes == null) throw StateError('no bytes');

      await SharePlus.instance.share(
        ShareParams(
          text: s.t('shareText'),
          files: [
            XFile.fromData(
              bytes.buffer.asUint8List(),
              mimeType: 'image/png',
              name: 'schedule.png',
            ),
          ],
          fileNameOverrides: ['schedule.png'],
        ),
      );
    } catch (_) {
      if (!mounted) return;
      showMessage(context, s.t('msgImageFailed'));
    }
  }

  @override
  Widget build(BuildContext context) {
    final s = S.of(context);

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            PageHeader(
              title: s.t('schedulesTitle'),
              subtitle: s.t('schedulesStep'),
              trailing: _results.isEmpty
                  ? null
                  : IconButton.filledTonal(
                      onPressed: _share,
                      tooltip: s.t('btnShare'),
                      icon: const Icon(Icons.share),
                    ),
            ),
            Expanded(
              child: _results.isEmpty ? _buildEmpty(s) : _buildSchedule(s),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  if (_results.length > 1)
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: _currentIndex > 0 ? _showPrevious : null,
                            icon: const Icon(Icons.chevron_left),
                            label: Text(s.t('btnPrev')),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed:
                                _currentIndex < _results.length - 1 ? _showNext : null,
                            icon: const Icon(Icons.chevron_right),
                            iconAlignment: IconAlignment.end,
                            label: Text(s.t('btnNext')),
                          ),
                        ),
                      ],
                    ),
                  const SizedBox(height: 8),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.tonalIcon(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.tune),
                      label: Text(s.t('btnBackToPrefs')),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// One schedule: header, grid and colored legend.
  Widget _buildSchedule(S s) {
    final scheme = Theme.of(context).colorScheme;
    final schedule = _results[_currentIndex];
    final gaps = ScheduleRanker.countGapMinutes(schedule);
    final days = ScheduleRanker.countDays(schedule);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(12),
      // Everything inside the RepaintBoundary goes into the shared picture
      child: RepaintBoundary(
        key: _captureKey,
        child: Container(
          color: scheme.surface, // so the picture is not transparent
          padding: const EdgeInsets.all(8),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                s.f('scheduleHeader', [_currentIndex + 1, _results.length]),
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: scheme.primary,
                ),
              ),
              HintText(s.f('scheduleInfo', [formatDuration(s, gaps), days])),
              const SizedBox(height: 12),
              TimetableView(schedule: schedule),
              const SizedBox(height: 12),
              for (var i = 0; i < schedule.length; i++)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text.rich(
                    TextSpan(
                      children: [
                        TextSpan(
                          text: '■  ',
                          style: TextStyle(color: colorForCourse(i), fontSize: 16),
                        ),
                        TextSpan(
                          text: s.f('legendCourse', [
                            schedule[i].courseCode,
                            schedule[i].name,
                          ]),
                        ),
                      ],
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  /// What to show when there is no schedule: not generated yet, nothing fits,
  /// or nothing passes the filters.
  Widget _buildEmpty(S s) {
    String title;
    final details = <String>[];

    if (AppData.lastTotalFound < 0) {
      title = s.t('noSchedulesYet');
      details.add(s.t('noSchedulesYetInfo'));
    } else if (AppData.lastTotalFound == 0) {
      title = s.t('noClashFree');
      if (AppData.clashPairs.isEmpty) {
        details.add(s.t('msgNoSinglePair'));
      } else {
        for (final pair in AppData.clashPairs) {
          details.add(s.f('msgAlwaysClash', [pair.$1.code, pair.$2.code]));
        }
      }
    } else {
      title = s.t('noMatch');
      details.add(s.noMatchInfo(AppData.lastTotalFound));
    }

    return ListView(
      padding: const EdgeInsets.all(20),
      children: [
        Icon(Icons.event_busy, size: 56, color: Theme.of(context).colorScheme.secondary),
        const SizedBox(height: 12),
        Text(
          title,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        for (final line in details)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(line, textAlign: TextAlign.center),
          ),
      ],
    );
  }
}
