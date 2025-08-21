import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class StorageService {
  final _storage = const FlutterSecureStorage();

  // Old methods (can be kept for backward compatibility or removed if not used elsewhere)
  Future<void> saveToken(String token) async {
    await _storage.write(key: 'token', value: token);
  }

  Future<String?> getToken() async {
    return await _storage.read(key: 'token');
  }

  Future<void> deleteToken() async {
    await _storage.delete(key: 'token');
  }

  // New methods for JWT tokens
  Future<void> saveAuthTokens({
    required String accessToken,
    required String refreshToken,
    required String tokenType,
  }) async {
    await _storage.write(key: 'accessToken', value: accessToken);
    await _storage.write(key: 'refreshToken', value: refreshToken);
    await _storage.write(key: 'tokenType', value: tokenType);
  }

  Future<Map<String, String?>> getAuthTokens() async {
    final accessToken = await _storage.read(key: 'accessToken');
    final refreshToken = await _storage.read(key: 'refreshToken');
    final tokenType = await _storage.read(key: 'tokenType');
    return {
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'tokenType': tokenType,
    };
  }

  Future<void> deleteAllAuthTokens() async {
    await _storage.delete(key: 'accessToken');
    await _storage.delete(key: 'refreshToken');
    await _storage.delete(key: 'tokenType');
  }
}
