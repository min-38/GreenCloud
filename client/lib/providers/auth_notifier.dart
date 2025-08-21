import 'dart:convert'; // JSON 디코딩을 위한 import
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:greencloud_client/providers/user_provider.dart';
import 'package:greencloud_client/services/auth_service.dart';
import 'package:greencloud_client/services/storage_service.dart';
import 'package:logger/logger.dart';

final logger = Logger();

// 인증 상태를 관리하는 Provider
// AsyncNotifier<bool> → 로그인 여부를 true/false로 관리
final authNotifierProvider = AsyncNotifierProvider<AuthNotifier, bool>(() {
  return AuthNotifier();
});

class AuthNotifier extends AsyncNotifier<bool> {
  late final AuthService _authService;
  late final StorageService _storageService;

  // 앱 시작 시 실행되어 초기 로그인 상태를 결정
  @override
  Future<bool> build() async {
    _authService = ref.watch(authServiceProvider);
    _storageService = ref.watch(storageServiceProvider);

    // 저장소에 토큰이 존재하는지 확인하여 초기 로그인 상태를 반환
    final authTokens = await _storageService.getAuthTokens();
    return authTokens['accessToken'] != null && authTokens['refreshToken'] != null;
  }

  // 로그인 로직
  Future<void> login(String email, String password) async {
    state = const AsyncValue.loading(); // 상태를 로딩으로 변경
    try {
      final response = await _authService.login(email, password);
      if (response.statusCode == 200) {
        final responseBody = jsonDecode(response.body);
        if (responseBody['success'] == true) {
          // 서버로부터 받은 토큰 저장
          final accessToken = responseBody['data']['accessToken'];
          final refreshToken = responseBody['data']['refreshToken'];
          final tokenType = responseBody['data']['tokenType'];

          await _storageService.saveAuthTokens(
            accessToken: accessToken,
            refreshToken: refreshToken,
            tokenType: tokenType,
          );
          state = const AsyncValue.data(true); // 로그인 성공
        } else {
          // 서버에서 success=false 반환 시 에러 처리
          state = AsyncValue.error(
            responseBody['message'] ?? '로그인 실패',
            StackTrace.current,
          );
        }
      } else {
        // 응답 코드가 200이 아니면 로그인 실패 처리
        state = AsyncValue.error(
          '이메일 또는 비밀번호가 올바르지 않습니다.',
          StackTrace.current,
        );
      }
    } catch (e, st) {
      // 예외 발생 시 에러 상태로 설정
      logger.e('로그인 중 오류', error: e, stackTrace: st);
      state = AsyncValue.error(e, st);
    }
  }

  // 로그아웃 로직
  Future<void> logout() async {
    state = const AsyncValue.loading(); // 상태를 로딩으로 변경
    try {
      final authTokens = await _storageService.getAuthTokens();
      final refreshToken = authTokens['refreshToken'];

      if (refreshToken != null) {
        // 서버에 로그아웃 API 호출
        final response = await _authService.logout(refreshToken);
        if (response.statusCode == 200) {
          await _clearLocalData(); // 정상 로그아웃 시 로컬 데이터 삭제
        } else {
          // 서버 로그아웃 실패하더라도 로컬 토큰은 삭제 (보안상 이유)
          await _clearLocalData();
          state = AsyncValue.error(
            response.body ?? '로그아웃 실패',
            StackTrace.current,
          );
        }
      } else {
        // refreshToken이 없으면 바로 로컬 데이터 삭제
        await _clearLocalData();
      }
    } catch (e, st) {
      // 예외 발생 시에도 로컬 토큰 삭제
      logger.e('로그아웃 중 오류', error: e, stackTrace: st);
      await _clearLocalData();
      state = AsyncValue.error(e, st);
    }
  }

  // 로컬 저장소 데이터 삭제 및 상태 초기화
  Future<void> _clearLocalData() async {
    await _storageService.deleteAllAuthTokens(); // 토큰 전체 삭제
    ref.read(userNotifierProvider.notifier).clearUser(); // 유저 정보 초기화
    state = const AsyncValue.data(false); // 로그인 상태를 false로 변경
  }
}

// AuthService와 StorageService를 Provider로 등록
final authServiceProvider = Provider<AuthService>((ref) => AuthService());
final storageServiceProvider = Provider<StorageService>((ref) => StorageService());