import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:logger/logger.dart';

final logger = Logger();

class LoggerObserver extends ProviderObserver {
  @override
  void didAddProvider(ProviderBase provider, Object? value, ProviderContainer container) {
    logger.d('Provider added: ${provider.name ?? provider.runtimeType}');
  }

  @override
  void didUpdateProvider(ProviderBase provider, Object? previousValue, Object? newValue, ProviderContainer container) {
    logger.d(
      'Provider updated: ${provider.name ?? provider.runtimeType}, ' +
      'previous: $previousValue, newValue: $newValue',
    );
  }

  @override
  void didDisposeProvider(ProviderBase provider, ProviderContainer container) {
    logger.d('Provider disposed: ${provider.name ?? provider.runtimeType}');
  }

  @override
  void providerDidFail(ProviderBase provider, Object error, StackTrace stackTrace, ProviderContainer container) {
    logger.e(
      'Provider failed: ${provider.name ?? provider.runtimeType}, ' +
      'error: $error, stackTrace: $stackTrace',
    );
  }
}
