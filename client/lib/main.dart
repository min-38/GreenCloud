import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:greencloud_client/services/api_service.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized(); // Flutter 앱이 위젯 트리나 엔진과 상호작용하기 전에 필요한 내부 준비 작업을 강제로 해주는 코드

  // --dart-define으로 전달된 flavor 값을 읽어옴 (기본값: dev)
  const flavor = String.fromEnvironment('FLAVOR', defaultValue: 'dev');

  // flavor 값에 따라 적절한 .env 파일을 로드합니다.
  await dotenv.load(fileName: '.env.$flavor');

  // 앱 시작 시 서버 헬스 체크 실행 및 결과 출력
  final serverStatus = await ApiService().checkServerHealth();
  print('--- Server Health Check ---');
  print(serverStatus);
  print('---------------------------');

  runApp(const ProviderScope(child: MyApp()));
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    // TODO: UI 교체 필요.
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('GreenCloud'),
        ),
        body: const Center(
          child: Text('콘솔 확인'),
        ),
      ),
    );
  }
}