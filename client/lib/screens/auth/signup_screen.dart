import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:greencloud_client/services/auth_service.dart';
import 'package:logger/logger.dart';

final logger = Logger();

enum EmailCheckStatus { unchecked, success, failed }

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _authService = AuthService();

  bool _isLoading = false;
  bool _isCheckingEmail = false;
  EmailCheckStatus _emailCheckStatus = EmailCheckStatus.unchecked;
  String _emailCheckMessage = '';
  String _lastCheckedEmail = '';

  bool _isPasswordLengthValid = false;
  bool _hasUppercase = false;
  bool _hasLowercase = false;
  bool _hasNumber = false;
  bool _hasSpecialCharacter = false;

  bool _isEmailFormatValid = true;

  @override
  void initState() {
    super.initState();
    _emailController.addListener(() {
      _validateEmail(_emailController.text);
      if (_emailCheckStatus == EmailCheckStatus.success &&
          _emailController.text != _lastCheckedEmail) {
        setState(() {
          _emailCheckStatus = EmailCheckStatus.unchecked;
        });
      }
    });
    _passwordController.addListener(() {
      _validatePassword(_passwordController.text);
    });
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _validateEmail(String email) {
    if (email.isEmpty) {
      setState(() => _isEmailFormatValid = true);
      return;
    }
    final emailRegex = RegExp(r"^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$");
    setState(() => _isEmailFormatValid = emailRegex.hasMatch(email));
  }

  void _validatePassword(String password) {
    setState(() {
      _isPasswordLengthValid = password.length >= 8 && password.length < 24;
      _hasUppercase = password.contains(RegExp(r'[A-Z]'));
      _hasLowercase = password.contains(RegExp(r'[a-z]'));
      _hasNumber = password.contains(RegExp(r'[0-9]'));
      _hasSpecialCharacter = password.contains(RegExp(r'[!@#$%^&*(),.?":{}|<>]'));
    });
  }

  Future<void> _checkEmail() async {
    FocusScope.of(context).unfocus();
    final email = _emailController.text;
    if (!_isEmailFormatValid || email.isEmpty) {
      setState(() {
        _emailCheckStatus = EmailCheckStatus.failed;
        _emailCheckMessage = '유효한 이메일 형식이 아닙니다.';
      });
      return;
    }

    setState(() => _isCheckingEmail = true);

    try {
      final response = await _authService.checkEmail(email);
      final responseBody = jsonDecode(response.body);
      if (response.statusCode == 200 && (responseBody['data']?['available'] ?? false)) {
        setState(() {
          _emailCheckStatus = EmailCheckStatus.success;
          _lastCheckedEmail = email;
        });
      } else {
        setState(() {
          _emailCheckStatus = EmailCheckStatus.failed;
          _emailCheckMessage = responseBody['message'] ?? '이미 사용중인 이메일입니다.';
        });
      }
    } catch (e) {
      setState(() {
        _emailCheckStatus = EmailCheckStatus.failed;
        _emailCheckMessage = '오류 발생: $e';
      });
    } finally {
      setState(() => _isCheckingEmail = false);
    }
  }

  Future<void> _signup() async {
    FocusScope.of(context).unfocus();
    if (!(_formKey.currentState?.validate() ?? false)) return;

    if (_emailCheckStatus != EmailCheckStatus.success) {
      _showError('이메일 중복 확인을 해주세요.');
      return;
    }

    setState(() => _isLoading = true);

    try {
      final response = await _authService.signup(
        _usernameController.text,
        _emailController.text,
        _passwordController.text,
        _confirmPasswordController.text,
      );
      final responseBody = jsonDecode(response.body);
      if (response.statusCode == 200 && responseBody['success'] == true) {
        _showSuccessDialog();
      } else {
        _showError(responseBody['message'] ?? '회원가입에 실패하였습니다.');
      }
    } catch (e) {
      _showError('오류 발생: $e');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Theme.of(context).colorScheme.error),
    );
  }

  void _showSuccessDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        final theme = Theme.of(context);
        return AlertDialog(
          shape: theme.cardTheme.shape,
          title: Column(
            children: [
              Icon(Icons.check_circle, color: theme.primaryColor, size: 48),
              const SizedBox(height: 10),
              Text('회원가입 성공', style: theme.textTheme.titleLarge),
            ],
          ),
          content: Text(
            '회원가입되었습니다. 로그인 해주세요.',
            textAlign: TextAlign.center,
            style: theme.textTheme.bodyMedium,
          ),
          actions: <Widget>[
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close dialog
                Navigator.of(context).pop(); // Go back to login screen
              },
              child: Text('확인', style: TextStyle(color: theme.primaryColor, fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
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
                Text('Create Your GreenCloud Account', style: Theme.of(context).textTheme.headlineSmall),
              ],
            ),
          ),
        ),
        Expanded(
          child: Center(
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 28.0),
                  child: _buildSignupFormContainer(),
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
              child: _buildSignupFormContainer(),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSignupFormContainer() {
    final isWide = MediaQuery.of(context).size.width > 600;
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      mainAxisSize: MainAxisSize.min,
      children: <Widget>[
        if (!isWide) ...[
          _buildLogo(),
          const SizedBox(height: 32.0),
        ],
        _buildSignupForm(),
        const SizedBox(height: 20.0),
        _buildSignupButton(),
        const SizedBox(height: 12.0),
        _buildLoginButton(),
      ],
    );
  }

  Widget _buildLogo() {
    return Text('GreenCloud', textAlign: TextAlign.center, style: Theme.of(context).textTheme.displayLarge);
  }

  Widget _buildSignupForm() {
    return Form(
      key: _formKey,
      child: Column(
        children: [
          _buildUsernameField(),
          const SizedBox(height: 16.0),
          _buildEmailField(),
          const SizedBox(height: 16.0),
          _buildPasswordField(),
          const SizedBox(height: 16.0),
          _buildConfirmPasswordField(),
        ],
      ),
    );
  }

  Widget _buildUsernameField() {
    return TextFormField(
      controller: _usernameController,
      decoration: const InputDecoration(hintText: 'Username'),
      validator: (value) => (value == null || value.trim().isEmpty) ? '유저명을 입력하세요.' : null,
    );
  }

  Widget _buildEmailField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: TextFormField(
                controller: _emailController,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(hintText: 'Email'),
                validator: (value) {
                  if (value == null || value.trim().isEmpty) return '이메일을 입력하세요.';
                  if (!_isEmailFormatValid) return '유효한 이메일 형식이 아닙니다.';
                  return null;
                },
              ),
            ),
            const SizedBox(width: 8.0),
            OutlinedButton(
              onPressed: (_isCheckingEmail || _emailCheckStatus == EmailCheckStatus.success) ? null : _checkEmail,
              style: OutlinedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16.0, horizontal: 12.0)),
              child: _isCheckingEmail
                  ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                  : _emailCheckStatus == EmailCheckStatus.success
                  ? Icon(Icons.check, color: Theme.of(context).primaryColor)
                  : const Text('중복확인'),
            ),
          ],
        ),
        if (_emailCheckStatus == EmailCheckStatus.failed)
          Padding(
            padding: const EdgeInsets.only(top: 8.0, left: 12.0),
            child: Text(_emailCheckMessage, style: Theme.of(context).textTheme.bodySmall),
          ),
      ],
    );
  }

  Widget _buildPasswordField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextFormField(
          controller: _passwordController,
          obscureText: true,
          decoration: const InputDecoration(hintText: 'Password'),
          validator: (value) {
            if (value == null || value.isEmpty) return '비밀번호를 입력하세요.';
            if (!(_isPasswordLengthValid && _hasUppercase && _hasLowercase && _hasNumber && _hasSpecialCharacter)) {
              return '비밀번호 형식이 올바르지 않습니다.';
            }
            return null;
          },
        ),
        const SizedBox(height: 10.0),
        _buildPasswordValidation(),
      ],
    );
  }

  Widget _buildPasswordValidation() {
    final labelStyle = Theme.of(context).textTheme.bodySmall?.copyWith(color: Theme.of(context).hintColor);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildValidationRow('8자 이상 24자 미만', _isPasswordLengthValid, labelStyle),
        _buildValidationRow('대문자 포함', _hasUppercase, labelStyle),
        _buildValidationRow('소문자 포함', _hasLowercase, labelStyle),
        _buildValidationRow('숫자 포함', _hasNumber, labelStyle),
        _buildValidationRow('특수문자 포함', _hasSpecialCharacter, labelStyle),
      ],
    );
  }

  Widget _buildValidationRow(String text, bool isValid, TextStyle? style) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2.0),
      child: Row(
        children: [
          Icon(isValid ? Icons.check_circle : Icons.cancel, size: 16, color: isValid ? theme.primaryColor : theme.colorScheme.error),
          const SizedBox(width: 8.0),
          Text(text, style: style),
        ],
      ),
    );
  }

  Widget _buildConfirmPasswordField() {
    return TextFormField(
      controller: _confirmPasswordController,
      obscureText: true,
      decoration: const InputDecoration(hintText: 'Confirm Password'),
      validator: (value) {
        if (value == null || value.isEmpty) return '비밀번호를 다시 입력하세요.';
        if (value != _passwordController.text) return '비밀번호가 일치하지 않습니다.';
        return null;
      },
    );
  }

  Widget _buildSignupButton() {
    return ElevatedButton(
      onPressed: _isLoading ? null : _signup,
      child: _isLoading
          ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 3, color: Colors.white))
          : Text('Sign Up', style: Theme.of(context).textTheme.labelLarge),
    );
  }

  Widget _buildLoginButton() {
    return OutlinedButton(
      onPressed: () => Navigator.pop(context),
      child: Text('Log In', style: Theme.of(context).textTheme.labelLarge?.copyWith(color: Theme.of(context).primaryColor)),
    );
  }
}
