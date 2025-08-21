import 'dart:io'; // For SocketException
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:http/http.dart' as http;
import 'dart:async'; // For TimeoutException

class ApiService {
  final String? _serverApiUrl = dotenv.env['SERVER_HOST'];

  Future<String> checkServerHealth() async {
    if (_serverApiUrl == null) {
      return '서버 주소를 찾을 수 없습니다. .env 파일을 확인해주세요.';
    }

    final url = Uri.parse('$_serverApiUrl/actuator/health');
    print(url);

    try {
      final response = await http.get(url).timeout(const Duration(seconds: 5));

      if (response.statusCode == 200) {
        return '연결 성공! 응답: ${response.body}';
      } else {
        return '연결 실패! 상태 코드: ${response.statusCode}';
      }
    } on SocketException {
      return '네트워크 연결 오류 또는 호스트를 찾을 수 없음';
    } on TimeoutException {
      return '서버 응답 시간 초과';
    } catch (e) {
      return '서버 연결 중 알 수 없는 오류 발생: $e';
    }
  }
}
