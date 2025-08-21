import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:greencloud_client/screens/auth/login_screen.dart';
import 'package:greencloud_client/providers/auth_notifier.dart';
import 'package:logger/logger.dart';

final logger = Logger(); // Logger 초기화

class MainScreen extends ConsumerWidget {
  const MainScreen({super.key});

  Future<void> _logout(BuildContext context, WidgetRef ref) async {
    try {
      await ref.read(authNotifierProvider.notifier).logout();
      // 네비게이션은 main.dart에서 인증 상태에 따라 처리됨
    } catch (e) {
      logger.e('Logout error: $e');
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('로그아웃 중 오류 발생: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // 인증 상태 변화를 감지하여 명시적으로 네비게이션 처리
    ref.listen<AsyncValue<bool>>(authNotifierProvider, (previous, next) {
      next.whenData((isLoggedIn) {
        if (!isLoggedIn) {
          // 로그인 화면으로 이동하고 모든 라우트 제거
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(builder: (context) => const LoginScreen()),
            (route) => false,
          );
        }
      });
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('Main Screen'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () => _logout(context, ref), // 로그아웃 버튼
          ),
        ],
      ),
      body: const Center(
        child: Text('Welcome!'),
      ),
    );
  }
}