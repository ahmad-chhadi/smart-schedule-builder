import 'package:material_ui/material_ui.dart';

/// Navy blue + warm orange, in a light and a dark version.
ThemeData buildTheme(Brightness brightness) {
  final light = brightness == Brightness.light;

  final scheme = ColorScheme.fromSeed(
    seedColor: const Color(0xFF24467F),
    brightness: brightness,
  ).copyWith(
    primary: light ? const Color(0xFF24467F) : const Color(0xFFADC6FF),
    onPrimary: light ? const Color(0xFFFFFFFF) : const Color(0xFF002E69),
    primaryContainer: light ? const Color(0xFFD8E2FF) : const Color(0xFF1C3E77),
    onPrimaryContainer: light ? const Color(0xFF001A41) : const Color(0xFFD8E2FF),
    secondary: light ? const Color(0xFF9A4A12) : const Color(0xFFFFB68E),
    onSecondary: light ? const Color(0xFFFFFFFF) : const Color(0xFF552000),
    secondaryContainer: light ? const Color(0xFFFFDBC9) : const Color(0xFF763300),
    onSecondaryContainer: light ? const Color(0xFF331200) : const Color(0xFFFFDBC9),
    surface: light ? const Color(0xFFFAF9F6) : const Color(0xFF121316),
    onSurface: light ? const Color(0xFF1A1C1E) : const Color(0xFFE3E2E6),
    onSurfaceVariant: light ? const Color(0xFF44474F) : const Color(0xFFC4C6D0),
    outline: light ? const Color(0xFF74777F) : const Color(0xFF8E9099),
  );

  return ThemeData(
    colorScheme: scheme,
    fontFamily: 'Poppins', // Arabic text falls back to the phone's Arabic font
    scaffoldBackgroundColor: scheme.surface,
  );
}
