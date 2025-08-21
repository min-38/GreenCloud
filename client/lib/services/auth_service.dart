import 'dart:convert';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:http/http.dart' as http;
import 'package:logger/logger.dart';

final logger = Logger();

class AuthService {
  final String? _serverApiUrl;
  late final String _loginUrl;
  late final String _signupUrl;
  late final String _checkEmailUrl;
  late final String _logoutUrl; // Add logout URL

  AuthService()
      : _serverApiUrl = dotenv.env['SERVER_API_URL'] {
    _loginUrl = '$_serverApiUrl/auth/signin';
    _signupUrl = '$_serverApiUrl/auth/signup';
    _checkEmailUrl = '$_serverApiUrl/auth/check-email';
    _logoutUrl = '$_serverApiUrl/auth/logout'; // Initialize logout URL
  }

  Future<http.Response> login(String email, String password) async {
    final headers = {
      'Content-Type': 'application/json',
    };
    final body = jsonEncode({
      'email': email,
      'password': password,
    });

    logger.i('POST $_loginUrl, body: $body');
    return await http.post(Uri.parse(_loginUrl), headers: headers, body: body).timeout(const Duration(seconds: 10));
  }

  Future<http.Response> signup(String username, String email, String password, String password2) async {
    final headers = {
      'Content-Type': 'application/json',
    };
    final body = jsonEncode({
      'username': username,
      'email': email,
      'password': password,
      'password2': password2,
    });

    logger.i('POST $_signupUrl, body: $body');
    return await http.post(Uri.parse(_signupUrl), headers: headers, body: body).timeout(const Duration(seconds: 10));
  }

  Future<http.Response> checkEmail(String email) async {
    final url = Uri.parse('$_checkEmailUrl?email=$email');
    logger.i('GET $url');
    return await http.get(url).timeout(const Duration(seconds: 10));
  }

  // New logout method
  Future<http.Response> logout(String refreshToken) async {
    final headers = {
      'Content-Type': 'application/json',
    };
    final body = jsonEncode({
      'refreshToken': refreshToken,
    });

    logger.i('POST $_logoutUrl, body: $body');
    return await http.post(
      Uri.parse(_logoutUrl),
      headers: headers,
      body: body,
    ).timeout(const Duration(seconds: 10));
  }
}
