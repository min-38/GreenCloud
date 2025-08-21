import 'package:flutter/material.dart';
import 'package:greencloud_client/screens/auth/login_screen.dart';
import 'package:greencloud_client/screens/main/main_screen.dart';
import 'package:greencloud_client/services/storage_service.dart';

// 앱이 시작될 때 사용자에게 가장 먼저 보여지는 로딩 화면
// 로그인 상태를 확인하고, 로그인 화면 또는 메인 화면으로 이동
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _checkLoginStatus();
  }

  Future<void> _checkLoginStatus() async {
    final token = await StorageService().getToken();
    if (token != null) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const MainScreen()),
      );
    } else {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const LoginScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: CircularProgressIndicator(),
      ),
    );
  }
}
