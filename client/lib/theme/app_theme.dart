import 'package:flutter/material.dart';

class AppTheme {
  // Private constructor
  AppTheme._();

  static const Color _primaryGreen = Color(0xFF16A34A);
  static const Color _dangerRed = Color(0xFFDC2626);

  // Light Theme
  static final ThemeData lightTheme = ThemeData(
    brightness: Brightness.light,
    primaryColor: _primaryGreen,
    scaffoldBackgroundColor: const Color(0xFFEFF2F5),
    colorScheme: const ColorScheme.light(
      primary: _primaryGreen,
      secondary: _primaryGreen,
      error: _dangerRed,
      background: Color(0xFFEFF2F5), // bgEnd
      surface: Colors.white, // cardColor
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onError: Colors.white,
      onBackground: Colors.black87, // onCardText
      onSurface: Colors.black87,
    ),
    textTheme: _lightTextTheme,
    elevatedButtonTheme: _elevatedButtonTheme,
    outlinedButtonTheme: _outlinedButtonTheme,
    inputDecorationTheme: _inputDecorationTheme(isDark: false),
    cardTheme: CardThemeData(
      elevation: 12,
      color: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  );

  // Dark Theme
  static final ThemeData darkTheme = ThemeData(
    brightness: Brightness.dark,
    primaryColor: _primaryGreen,
    scaffoldBackgroundColor: const Color(0xFF111826),
    colorScheme: const ColorScheme.dark(
      primary: _primaryGreen,
      secondary: _primaryGreen,
      error: _dangerRed,
      background: Color(0xFF111826), // bgEnd
      surface: Color(0xFF1F2937), // cardColor
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onError: Colors.white,
      onBackground: Colors.white, // onCardText
      onSurface: Colors.white,
    ),
    textTheme: _darkTextTheme,
    elevatedButtonTheme: _elevatedButtonTheme,
    outlinedButtonTheme: _outlinedButtonTheme,
    inputDecorationTheme: _inputDecorationTheme(isDark: true),
    cardTheme: CardThemeData(
      elevation: 12,
      color: const Color(0xFF1F2937),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
    ),
  );

  // Text Themes
  static const TextTheme _lightTextTheme = TextTheme(
    displayLarge: TextStyle(fontSize: 44, fontWeight: FontWeight.w800, fontFamily: 'Poppins', color: _primaryGreen),
    headlineSmall: TextStyle(fontSize: 20, fontFamily: 'Poppins', color: Colors.black54),
    labelLarge: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
    bodySmall: TextStyle(fontSize: 13.0, color: _dangerRed),
  );

  static const TextTheme _darkTextTheme = TextTheme(
    displayLarge: TextStyle(fontSize: 44, fontWeight: FontWeight.w800, fontFamily: 'Poppins', color: _primaryGreen),
    headlineSmall: TextStyle(fontSize: 20, fontFamily: 'Poppins', color: Colors.white70),
    labelLarge: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Colors.white),
    bodySmall: TextStyle(fontSize: 13.0, color: _dangerRed),
  );

  // Button Themes
  static final ElevatedButtonThemeData _elevatedButtonTheme = ElevatedButtonThemeData(
    style: ElevatedButton.styleFrom(
      backgroundColor: _primaryGreen,
      foregroundColor: Colors.white,
      padding: const EdgeInsets.symmetric(vertical: 18.0),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.0)),
      elevation: 3,
    ),
  );

  static final OutlinedButtonThemeData _outlinedButtonTheme = OutlinedButtonThemeData(
    style: OutlinedButton.styleFrom(
      foregroundColor: _primaryGreen,
      side: const BorderSide(color: _primaryGreen),
      padding: const EdgeInsets.symmetric(vertical: 18.0),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12.0)),
    ),
  );

  // Input Decoration Theme
  static InputDecorationTheme _inputDecorationTheme({required bool isDark}) {
    final hintColor = isDark ? Colors.white.withOpacity(0.6) : Colors.grey.shade500;
    final fillColor = isDark ? const Color(0xFF111827) : Colors.white;
    final borderColor = isDark ? const Color(0xFF374151) : Colors.grey.shade300;

    return InputDecorationTheme(
      hintStyle: TextStyle(color: hintColor),
      filled: true,
      fillColor: fillColor,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.0),
        borderSide: BorderSide(color: borderColor),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.0),
        borderSide: BorderSide(color: borderColor),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12.0),
        borderSide: const BorderSide(color: _primaryGreen, width: 1.6),
      ),
      contentPadding: const EdgeInsets.symmetric(vertical: 16.0, horizontal: 20.0),
      errorStyle: TextStyle(fontSize: 13.0, color: _dangerRed),
    );
  }
}
