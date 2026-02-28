import 'package:tracko/component/menu_bar.dart' as TrackoMenuBar;
import 'package:flutter/material.dart';
import 'package:tracko/services/auth_service.dart';
import 'package:dio/dio.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/pages/backend_setup_page/backend_setup_page.dart';

import 'package:tracko/services/SessionService.dart';

class LoginPage extends StatelessWidget {
  LoginPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final screenHeight = MediaQuery.of(context).size.height;

    return Scaffold(
        appBar: TrackoMenuBar.MenuBar(),
        body: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 600, minWidth: 200),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: ListView(children: [
                Padding(
                  padding: EdgeInsets.symmetric(vertical: screenHeight * 0.1),
                  child: Image.asset(
                    "assets/images/expense-icon.png",
                    height: screenHeight * 0.30,
                    fit: BoxFit.contain,
                  ),
                ),
                LoginForm(),
              ]),
            ),
          ),
        ));
  }
}

class LoginForm extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _LoginPage();
  }
}

class _LoginPage extends State<LoginForm> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _submitting = false;
  bool _obscurePassword = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final loggedIn = await AuthService().isLoggedIn();
      if (!mounted) return;
      if (loggedIn) {
        // Ensure session is initialized
        await SessionService.getCurrentUser();

        // Check again if we are still logged in.
        // If SessionService.getCurrentUser() triggered a 401, ApiClient would have logged us out.
        if (mounted && await AuthService().isLoggedIn()) {
          Navigator.pushReplacementNamed(context, '/home');
        }
      }
    });
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  String _friendlyError(Object e) {
    if (e is DioException) {
      if (e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.sendTimeout ||
          e.type == DioExceptionType.receiveTimeout) {
        return 'Connection timed out. Please try again.';
      }
      if (e.type == DioExceptionType.connectionError) {
        return 'No internet connection. Please check your network.';
      }
      final status = e.response?.statusCode;
      if (status == 401 || status == 403) {
        return 'Invalid email or password.';
      }
    }
    return 'Login failed. Please try again.';
  }

  Future<void> _submit() async {
    if (_submitting) return;
    if (!(_formKey.currentState?.validate() ?? false)) return;

    setState(() => _submitting = true);
    try {
      final auth = AuthService();
      final token = await auth.signInBasic(
        username: _usernameController.text.trim(),
        password: _passwordController.text,
      );
      if (token != null && token.isNotEmpty) {
        // Fetch user profile to initialize session and currency settings
        await SessionService.getCurrentUser(forceRefresh: true);

        if (!mounted) return;
        Navigator.pushReplacementNamed(context, '/home');
      } else {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Invalid email or password.')),
        );
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_friendlyError(e))),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        children: <Widget>[
          const SizedBox(height: 20),
          TextFormField(
            controller: _usernameController,
            decoration: InputDecoration(
              labelText: 'Email',
              prefixIcon: const Icon(Icons.email_outlined),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey.shade400),
              ),
            ),
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.next,
            autofillHints: const [AutofillHints.username, AutofillHints.email],
            autocorrect: false,
            enableSuggestions: false,
            validator: (value) {
              if (value?.isEmpty ?? true) {
                return 'Email is required';
              }
              return null;
            },
          ),
          const SizedBox(height: 20),
          TextFormField(
            controller: _passwordController,
            decoration: InputDecoration(
              labelText: 'Password',
              prefixIcon: const Icon(Icons.lock_outline),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey.shade400),
              ),
              suffixIcon: IconButton(
                onPressed: () =>
                    setState(() => _obscurePassword = !_obscurePassword),
                icon: Icon(
                    _obscurePassword ? Icons.visibility : Icons.visibility_off),
              ),
            ),
            obscureText: _obscurePassword,
            textInputAction: TextInputAction.done,
            autofillHints: const [AutofillHints.password],
            autocorrect: false,
            enableSuggestions: false,
            onFieldSubmitted: (_) => _submit(),
            validator: (value) {
              if (value?.isEmpty ?? true) {
                return 'Password is required';
              }
              return null;
            },
          ),
          const SizedBox(height: 30),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.teal,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 16.0),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                elevation: 2,
              ),
              onPressed: _submitting ? null : _submit,
              child: _submitting
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Text(
                      'Login',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 16),
          if (!ApiConfig.isProduction)
            TextButton.icon(
              onPressed: _submitting
                  ? null
                  : () async {
                      await ApiConfig.reset();
                      if (context.mounted) {
                        Navigator.of(context).pushReplacement(
                          MaterialPageRoute(
                              builder: (context) => const BackendSetupPage()),
                        );
                      }
                    },
              icon: const Icon(Icons.settings_ethernet),
              label: const Text('Change Backend URL'),
              style: TextButton.styleFrom(
                foregroundColor: Colors.grey,
              ),
            ),
        ],
      ),
    );
  }
}
