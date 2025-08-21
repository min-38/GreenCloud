import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:greencloud_client/theme/app_theme.dart';

import 'package:greencloud_client/screens/auth/login_screen.dart';
import 'package:greencloud_client/screens/splash/splash_screen.dart';
import 'package:greencloud_client/services/api_service.dart';
import 'package:greencloud_client/screens/main/main_screen.dart';
import 'package:greencloud_client/providers/auth_notifier.dart';
import 'package:greencloud_client/utils/logger_observer.dart';
import 'package:logger/logger.dart';

final logger = Logger();

Future<void> main() async {
  // Flutter 앱이 위젯 트리나 엔진과 상호작용하기 전에 내부 준비 작업을 실행
  WidgetsFlutterBinding.ensureInitialized();

  // --dart-define으로 전달된 flavor 값을 읽음 (기본값: dev)
  const flavor = String.fromEnvironment('FLAVOR', defaultValue: 'dev');

  // flavor 값에 따라 해당되는 .env 파일을 로드
  await dotenv.load(fileName: '.env.$flavor');

  // 앱 실행 시 서버 헬스 체크 실행 및 결과 출력
  final serverStatus = await ApiService().checkServerHealth();
  logger.i('--- 서버 헬스 체크 ---');
  logger.i(serverStatus);
  logger.i('--------------------');

  runApp(
    ProviderScope(
      observers: [LoggerObserver()], // Riverpod 상태 변화를 로깅하는 Observer 등록
      child: const MyApp(),
    ),
  );
}

class MyApp extends ConsumerWidget { // Riverpod의 ConsumerWidget 사용
  const MyApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) { // WidgetRef를 사용해 Provider 상태 구독
    final authState = ref.watch(authNotifierProvider); // 인증 상태 구독

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'GreenCloud',
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system, // 시스템 테마 모드에 따라 자동 적용
      home: authState.when(
        data: (isLoggedIn) {
          // 로그인 여부에 따라 화면 분기
          if (isLoggedIn) {
            return const MainScreen();
          } else {
            return const LoginScreen();
          }
        },
        loading: () => const SplashScreen(), // 초기 상태 로딩 중에는 Splash 화면 표시
        error: (error, stack) {
          logger.e('인증 상태 오류', error: error, stackTrace: stack);
          return const LoginScreen();
        },
      ),
    );
  }
}