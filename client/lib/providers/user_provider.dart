
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:greencloud_client/models/user_model.dart';

// Defines a provider for the UserNotifier
final userNotifierProvider = StateNotifierProvider<UserNotifier, User?>((ref) {
  return UserNotifier();
});

// The StateNotifier that will hold the user's state
class UserNotifier extends StateNotifier<User?> {
  UserNotifier() : super(null); // Initial state is no user

  // Method to set the user's data (e.g., after login)
  void setUser(User user) {
    state = user;
  }

  // Method to clear the user's data (e.g., on logout)
  void clearUser() {
    state = null;
  }
}
