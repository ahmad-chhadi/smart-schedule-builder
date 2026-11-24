import 'package:flutter/widgets.dart';

/// All text shown in the app, in English and Arabic.
///
/// Usage: `final s = S.of(context);` then `s.t('btnAddCourse')`, or
/// `s.f('msgCourseExists', [code])` for text with values ({0}, {1}, ...).
class S {
  S._(this.isArabic);

  final bool isArabic;

  static S of(BuildContext context) =>
      S._(Localizations.localeOf(context).languageCode == 'ar');

  Map<String, String> get _map => isArabic ? ar : en;

  /// Plain text.
  String t(String key) => _map[key] ?? en[key] ?? key;

  /// Text with values: {0} is replaced by args[0], {1} by args[1], ...
  String f(String key, List<Object> args) {
    var text = t(key);
    for (var i = 0; i < args.length; i++) {
      text = text.replaceAll('{$i}', '${args[i]}');
    }
    return text;
  }

  /// Day names, index 0 = Monday ... 5 = Saturday (same as TimeSlot.day).
  List<String> get dayNames => isArabic ? arDays : enDays;

  /// "N clash-free schedules exist, but none pass your filters" with the right
  /// plural form (Arabic has six forms: zero, one, two, few, many, other).
  String noMatchInfo(int n) {
    String key;
    if (n == 1) {
      key = 'noMatchOne';
    } else if (!isArabic) {
      key = 'noMatchOther';
    } else if (n == 2) {
      key = 'noMatchTwo';
    } else if (n % 100 >= 3 && n % 100 <= 10) {
      key = 'noMatchFew';
    } else if (n % 100 >= 11 && n % 100 <= 99) {
      key = 'noMatchMany';
    } else {
      key = 'noMatchOther';
    }
    return f(key, [n]);
  }

  static const List<String> enDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  static const List<String> arDays = [
    'الإثنين',
    'الثلاثاء',
    'الأربعاء',
    'الخميس',
    'الجمعة',
    'السبت',
  ];

  static const Map<String, String> en = {
    // Shared
    'appName': 'Smart Schedule Builder',
    'cancel': 'Cancel',
    'slotFormat': '{0} {1}–{2}',
    'slotSeparator': ', ',
    'durationMinutes': '{0}m',
    'durationHours': '{0}h',
    'durationHoursMinutes': '{0}h {1}m',
    'langSwitch': 'عربي',

    // Page 1: courses
    'mainStep': 'Step 1 of 3 · Your courses',
    'addCourseTitle': 'Add a course',
    'hintCourseCode': 'Course code (e.g. CSCI 410)',
    'hintCourseTitle': 'Course title (e.g. Mobile Apps)',
    'btnAddCourse': 'Add Course',
    'btnExample': 'Try with example data',
    'yourCourses': 'Your courses',
    'noCourses': 'No courses yet. Add one above.',
    'coursesTip': 'Tap a course to add its sections. Long-press to remove it.',
    'btnClearAll': 'Clear All',
    'btnGoPrefs': 'Next: Preferences',
    'courseRowTitle': '{0} – {1}',
    'courseRowNoSections': 'No sections yet – tap to add',
    'courseRowSection': 'Section {0}: {1}',
    'msgEnterCourseCode': 'Please enter a course code',
    'msgCourseExists': '{0} already exists',
    'msgAddCourseFirst': 'Add at least one course first',
    'msgCourseNoSections': '{0} has no sections yet',
    'msgNothingToClear': 'There is nothing to clear',
    'clearDialogTitle': 'Clear all courses?',
    'clearDialogMessage': 'This removes every course and section you added.',
    'clearDialogConfirm': 'Clear',
    'msgAllRemoved': 'All courses removed',
    'msgCourseRemoved': 'Removed {0}',
    'msgExampleLoaded': 'Example courses loaded',

    // Page 2: sections
    'addSectionsTitle': 'Add sections',
    'hintSectionName': 'Section name (e.g. A)',
    'daysLabel': 'Days',
    'btnStartTime': 'Start time',
    'btnEndTime': 'End time',
    'btnStartTimeValue': 'Start {0}',
    'btnEndTimeValue': 'End {0}',
    'btnAddSection': 'Add Section',
    'sectionsOfCourse': 'Sections of this course',
    'noSections': 'No sections yet.',
    'sectionTitle': 'Section {0}',
    'sectionsTip':
        'Tips: add the same section name again to give it another time (e.g. Mon 10:00 and Wed 14:00). Long-press a section to remove it.',
    'btnDone': 'Done',
    'msgEnterSectionName': 'Please enter a section name',
    'msgChooseDay': 'Choose at least one day',
    'msgChooseTimes': 'Choose the start and end time',
    'msgEndAfterStart': 'End time must be after start time',
    'msgSectionOverlaps': 'Section {0} already meets at {1}',
    'msgSectionAdded': 'Added section {0}',
    'msgSectionRemoved': 'Removed section {0}',

    // Page 3: preferences
    'prefsTitle': 'Preferences',
    'prefsStep': 'Step 2 of 3 · Only schedules that match all of these are shown.',
    'prefEarliestAny': 'Earliest class: any',
    'prefEarliestValue': 'Earliest class: {0}',
    'prefLatestAny': 'Latest end: any',
    'prefLatestValue': 'Latest end: {0}',
    'prefFreeDay': 'Keep at least one free weekday (Mon–Fri)',
    'hintMaxHours': 'Max class hours per day (optional)',
    'rankBy': 'Rank schedules by',
    'rankFewestGaps': 'Fewest gaps',
    'rankFewestDays': 'Fewest days on campus',
    'rankLatestStart': 'Latest start',
    'btnResetPrefs': 'Reset preferences',
    'btnGenerate': 'Generate Schedules',
    'msgPrefsReset': 'Preferences reset',
    'msgLatestAfterEarliest': 'Latest end must be after earliest class',
    'msgMaxHoursRange': 'Max hours must be between 1 and 24',
    'msgAlwaysClash':
        '⚠ {0} and {1} always clash: every section of one overlaps every section of the other.',
    'msgNoSinglePair':
        'No two courses always clash, but no combination of all your courses fits together. Try adding more sections.',

    // Page 4: schedules
    'schedulesTitle': 'Your schedules',
    'schedulesStep': 'Step 3 of 3 · Best schedules first',
    'btnShare': 'Share as image',
    'btnPrev': 'Prev',
    'btnNext': 'Next',
    'btnBackToPrefs': 'Change preferences',
    'noSchedulesYet': 'No schedules yet',
    'noSchedulesYetInfo': 'Go back to Preferences and press Generate Schedules.',
    'noClashFree': 'No clash-free schedule exists',
    'noMatch': 'No schedule matches your preferences',
    'noMatchOne':
        '{0} clash-free schedule exists, but it does not pass your filters. Try relaxing them.',
    'noMatchTwo':
        '{0} clash-free schedules exist, but none pass your filters. Try relaxing them.',
    'noMatchFew':
        '{0} clash-free schedules exist, but none pass your filters. Try relaxing them.',
    'noMatchMany':
        '{0} clash-free schedules exist, but none pass your filters. Try relaxing them.',
    'noMatchOther':
        '{0} clash-free schedules exist, but none pass your filters. Try relaxing them.',
    'scheduleHeader': 'Schedule {0} of {1}',
    'scheduleInfo': 'Gaps: {0}   ·   Days on campus: {1}',
    'legendCourse': '{0}  (Section {1})',
    'blockSection': 'Sec {0}',
    'msgNothingToShare': 'Nothing to share yet',
    'msgImageFailed': 'Could not create the image',
    'shareText': 'My schedule, made with Smart Schedule Builder',
  };

  static const Map<String, String> ar = {
    // Shared
    'appName': 'منشئ الجدول الذكي',
    'cancel': 'إلغاء',
    'slotFormat': '{0} {1}–{2}',
    'slotSeparator': '، ',
    'durationMinutes': '{0} د',
    'durationHours': '{0} س',
    'durationHoursMinutes': '{0} س {1} د',
    'langSwitch': 'English',

    // Page 1: courses
    'mainStep': 'الخطوة 1 من 3 · موادك',
    'addCourseTitle': 'إضافة مادة',
    'hintCourseCode': 'رمز المادة (مثلاً CSCI 410)',
    'hintCourseTitle': 'اسم المادة (مثلاً Mobile Apps)',
    'btnAddCourse': 'إضافة المادة',
    'btnExample': 'جرّب ببيانات مثال',
    'yourCourses': 'موادك',
    'noCourses': 'لا توجد مواد بعد. أضف مادة في الأعلى.',
    'coursesTip': 'اضغط على المادة لإضافة شعبها، واضغط مطوّلاً لحذفها.',
    'btnClearAll': 'حذف الكل',
    'btnGoPrefs': 'التالي: التفضيلات',
    'courseRowTitle': '{0} – {1}',
    'courseRowNoSections': 'لا توجد شعب بعد – اضغط للإضافة',
    'courseRowSection': 'الشعبة {0}: {1}',
    'msgEnterCourseCode': 'الرجاء إدخال رمز المادة',
    'msgCourseExists': 'المادة {0} موجودة مسبقاً',
    'msgAddCourseFirst': 'أضف مادة واحدة على الأقل أولاً',
    'msgCourseNoSections': 'المادة {0} لا تحتوي على شعب بعد',
    'msgNothingToClear': 'لا يوجد شيء لحذفه',
    'clearDialogTitle': 'حذف كل المواد؟',
    'clearDialogMessage': 'سيتم حذف كل المواد والشعب التي أضفتها.',
    'clearDialogConfirm': 'حذف',
    'msgAllRemoved': 'تم حذف كل المواد',
    'msgCourseRemoved': 'تم حذف {0}',
    'msgExampleLoaded': 'تم تحميل مواد المثال',

    // Page 2: sections
    'addSectionsTitle': 'إضافة الشعب',
    'hintSectionName': 'اسم الشعبة (مثلاً A)',
    'daysLabel': 'الأيام',
    'btnStartTime': 'وقت البداية',
    'btnEndTime': 'وقت النهاية',
    'btnStartTimeValue': 'البداية {0}',
    'btnEndTimeValue': 'النهاية {0}',
    'btnAddSection': 'إضافة الشعبة',
    'sectionsOfCourse': 'شعب هذه المادة',
    'noSections': 'لا توجد شعب بعد.',
    'sectionTitle': 'الشعبة {0}',
    'sectionsTip':
        'نصائح: أضف اسم الشعبة نفسه مرة أخرى لتعطيها وقتاً آخر (مثلاً الإثنين 10:00 والأربعاء 14:00). اضغط مطوّلاً على الشعبة لحذفها.',
    'btnDone': 'تم',
    'msgEnterSectionName': 'الرجاء إدخال اسم الشعبة',
    'msgChooseDay': 'اختر يوماً واحداً على الأقل',
    'msgChooseTimes': 'اختر وقت البداية والنهاية',
    'msgEndAfterStart': 'يجب أن يكون وقت النهاية بعد وقت البداية',
    'msgSectionOverlaps': 'الشعبة {0} لديها محاضرة في {1}',
    'msgSectionAdded': 'تمت إضافة الشعبة {0}',
    'msgSectionRemoved': 'تم حذف الشعبة {0}',

    // Page 3: preferences
    'prefsTitle': 'التفضيلات',
    'prefsStep': 'الخطوة 2 من 3 · تظهر فقط الجداول التي تطابق كل هذه الشروط.',
    'prefEarliestAny': 'أبكر محاضرة: أي وقت',
    'prefEarliestValue': 'أبكر محاضرة: {0}',
    'prefLatestAny': 'آخر نهاية: أي وقت',
    'prefLatestValue': 'آخر نهاية: {0}',
    'prefFreeDay': 'أبقِ يوماً واحداً على الأقل فارغاً (الإثنين–الجمعة)',
    'hintMaxHours': 'أقصى عدد ساعات في اليوم (اختياري)',
    'rankBy': 'ترتيب الجداول حسب',
    'rankFewestGaps': 'أقل فراغات',
    'rankFewestDays': 'أقل أيام دوام',
    'rankLatestStart': 'البداية الأكثر تأخراً',
    'btnResetPrefs': 'إعادة ضبط التفضيلات',
    'btnGenerate': 'إنشاء الجداول',
    'msgPrefsReset': 'تمت إعادة ضبط التفضيلات',
    'msgLatestAfterEarliest': 'يجب أن تكون آخر نهاية بعد أبكر محاضرة',
    'msgMaxHoursRange': 'يجب أن يكون عدد الساعات بين 1 و24',
    'msgAlwaysClash':
        '⚠ المادتان {0} و{1} تتعارضان دائماً: كل شعبة من الأولى تتداخل مع كل شعبة من الثانية.',
    'msgNoSinglePair':
        'لا توجد مادتان تتعارضان دائماً، لكن لا توجد طريقة تجمع كل موادك معاً. جرّب إضافة شعب أخرى.',

    // Page 4: schedules
    'schedulesTitle': 'جداولك',
    'schedulesStep': 'الخطوة 3 من 3 · الأفضل أولاً',
    'btnShare': 'مشاركة كصورة',
    'btnPrev': 'السابق',
    'btnNext': 'التالي',
    'btnBackToPrefs': 'تغيير التفضيلات',
    'noSchedulesYet': 'لا توجد جداول بعد',
    'noSchedulesYetInfo': 'ارجع إلى التفضيلات واضغط على إنشاء الجداول.',
    'noClashFree': 'لا يوجد جدول بدون تعارض',
    'noMatch': 'لا يوجد جدول يطابق تفضيلاتك',
    'noMatchOne': 'يوجد {0} جدول بدون تعارض، لكنه لا يطابق فلاترك. جرّب تخفيفها.',
    'noMatchTwo': 'يوجد {0} جدولان بدون تعارض، لكنهما لا يطابقان فلاترك. جرّب تخفيفها.',
    'noMatchFew': 'توجد {0} جداول بدون تعارض، لكنها لا تطابق فلاترك. جرّب تخفيفها.',
    'noMatchMany': 'يوجد {0} جدولاً بدون تعارض، لكنها لا تطابق فلاترك. جرّب تخفيفها.',
    'noMatchOther': 'يوجد {0} جدول بدون تعارض، لكنها لا تطابق فلاترك. جرّب تخفيفها.',
    'scheduleHeader': 'الجدول {0} من {1}',
    'scheduleInfo': 'الفراغات: {0}   ·   أيام الدوام: {1}',
    'legendCourse': '{0}  (الشعبة {1})',
    'blockSection': 'شعبة {0}',
    'msgNothingToShare': 'لا يوجد شيء للمشاركة بعد',
    'msgImageFailed': 'تعذّر إنشاء الصورة',
    'shareText': 'جدولي، من تطبيق منشئ الجدول الذكي',
  };
}
