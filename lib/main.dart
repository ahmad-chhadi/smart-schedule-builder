import 'package:material_ui/material_ui.dart';

import 'data/app_data.dart';
import 'screens/courses_screen.dart';
import 'ui/theme.dart';

void main() {
  runApp(const SmartScheduleApp());
}

/// The whole app. It rebuilds when the language changes (English <-> Arabic).
class SmartScheduleApp extends StatelessWidget {
  const SmartScheduleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<Locale>(
      valueListenable: AppData.locale,
      builder: (context, locale, _) => MaterialApp(
        title: 'Smart Schedule Builder',
        debugShowCheckedModeBanner: false,
        theme: buildTheme(Brightness.light),
        darkTheme: buildTheme(Brightness.dark),
        locale: locale,
        supportedLocales: const [Locale('en'), Locale('ar')],
        // Built-in texts (time picker, dialogs) + right-to-left layout for Arabic
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        home: const CoursesScreen(),
      ),
    );
  }
}
