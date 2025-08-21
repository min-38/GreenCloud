import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:greencloud_client/screens/main/main_screen.dart';
import 'package:greencloud_client/screens/auth/signup_screen.dart';
import 'package:greencloud_client/services/auth_service.dart';
import 'package:greencloud_client/services/storage_service.dart';
import 'package:logger/logger.dart';

final logger = Logger();

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _authService = AuthService();

  bool _isLoading = false;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    FocusScope.of(context).unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() => _isLoading = true);
    try {
      final response = await _authService.login(
        _emailController.text,
        _passwordController.text,
      );
      logger.i('Login response: ${response.statusCode}, ${response.body}');

      if (response.statusCode == 200) {
        final body = jsonDecode(response.body);
        if (body['success'] == true) {
          final accessToken = body['data']['accessToken'];
          final refreshToken = body['data']['refreshToken'];
          final tokenType = body['data']['tokenType'];
          await StorageService().saveAuthTokens(
            accessToken: accessToken,
            refreshToken: refreshToken,
            tokenType: tokenType,
          );
          if (!mounted) return;
          Navigator.pushAndRemoveUntil(
            context,
            MaterialPageRoute(builder: (_) => const MainScreen()),
                (route) => false,
          );
        } else {
          _showError('이메일 또는 비밀번호가 올바르지 않습니다.');
        }
      } else {
        _showError('이메일 또는 비밀번호가 올바르지 않습니다.');
      }
    } catch (e) {
      _showError('오류 발생: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        backgroundColor: Theme.of(context).colorScheme.error,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Use LayoutBuilder to distinguish between wide and mobile layouts
    return Scaffold(
      body: LayoutBuilder(
        builder: (context, constraints) {
          if (constraints.maxWidth > 600) {
            return _buildWebLayout();
          } else {
            return _buildMobileLayout();
          }
        },
      ),
    );
  }

  Widget _buildWebLayout() {
    return Row(
      children: [
        Expanded(
          child: Container(
            color: Theme.of(context).scaffoldBackgroundColor,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _buildLogo(),
                const SizedBox(height: 16),
                Text(
                  'Welcome to GreenCloud',
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
              ],
            ),
          ),
        ),
        Expanded(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 28.0),
                  child: _buildLoginFormContainer(isWide: true),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildMobileLayout() {
    return Container(
      color: Theme.of(context).scaffoldBackgroundColor,
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 24.0),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 24.0),
              child: _buildLoginFormContainer(isWide: false),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLoginFormContainer({required bool isWide}) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        if (!isWide) ...[
          _buildLogo(),
          const SizedBox(height: 32.0),
        ],
        _buildLoginForm(),
        const SizedBox(height: 20.0),
        _buildLoginButton(),
        const SizedBox(height: 12.0),
        _buildSignUpButton(),
        _buildForgotPasswordButton(),
      ],
    );
  }

  Widget _buildLogo() {
    return Text(
      'GreenCloud',
      textAlign: TextAlign.center,
      style: Theme.of(context).textTheme.displayLarge,
    );
  }

  Widget _buildLoginForm() {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          _buildEmailField(),
          const SizedBox(height: 16.0),
          _buildPasswordField(),
        ],
      ),
    );
  }

  Widget _buildEmailField() {
    return TextFormField(
      controller: _emailController,
      keyboardType: TextInputType.emailAddress,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      decoration: const InputDecoration(hintText: 'Email'),
      validator: (value) {
        if (value == null || value.trim().isEmpty) return '이메일을 입력하세요.';
        final emailRegex = RegExp(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$");
        if (!emailRegex.hasMatch(value)) return '유효한 이메일 형식이 아닙니다.';
        return null;
      },
    );
  }

  Widget _buildPasswordField() {
    return TextFormField(
      controller: _passwordController,
      obscureText: true,
      decoration: const InputDecoration(hintText: 'Password'),
      validator: (value) {
        if (value == null || value.isEmpty) return '비밀번호를 입력하세요.';
        if (value.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
        return null;
      },
    );
  }

  Widget _buildLoginButton() {
    return ElevatedButton(
      onPressed: _isLoading ? null : _login,
      child: _isLoading
          ? const SizedBox(
        height: 20, width: 20,
        child: CircularProgressIndicator(strokeWidth: 3, color: Colors.white),
      )
          : Text('Log In', style: Theme.of(context).textTheme.labelLarge),
    );
  }

  Widget _buildSignUpButton() {
    return OutlinedButton(
      onPressed: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => const SignupScreen()),
        );
      },
      child: Text('Sign Up', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Theme.of(context).primaryColor)),
    );
  }

  Widget _buildForgotPasswordButton() {
    return TextButton(
      onPressed: () {},
      child: const Text('Forgot password?'),
    );
  }
}
