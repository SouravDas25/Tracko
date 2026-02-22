import 'package:flutter/material.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/pages/login_page/login_page.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/Utils/HealthCheckUtil.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:dio/dio.dart';

class BackendSetupPage extends StatefulWidget {
  const BackendSetupPage({Key? key}) : super(key: key);

  @override
  _BackendSetupPageState createState() => _BackendSetupPageState();
}

class _BackendSetupPageState extends State<BackendSetupPage> {
  final _formKey = GlobalKey<FormState>();
  final _urlController = TextEditingController();
  bool _isLoading = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    // Pre-fill with last attempted URL if available, otherwise default dev URL
    _urlController.text = ApiConfig.lastAttemptedUrl ?? ApiConfig.devBaseUrl;
  }

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _connect() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _isLoading = true;
      _error = null;
    });

    String url = _urlController.text.trim();
    // Remove trailing slash if present to avoid double slashes with API paths
    if (url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }

    try {
      // 1. Save configuration
      await ApiConfig.setBaseUrl(url);

      // 2. Update running ApiClient instance
      ApiClient().updateBaseUrl(url);

      // 3. Verify connection
      // We attempt to hit the health endpoint with a short timeout.
      final isHealthy = await HealthCheckUtil.checkHealth(url);
      if (!isHealthy) {
        throw Exception(
            "Server returned invalid health status or is unreachable");
      }

      // 4. Try to fetch user session (best effort, initializes session if token exists)
      try {
        await SessionService.getCurrentUser(forceRefresh: true);
      } catch (_) {
        // Ignore session fetch errors here, as the user might just need to login
      }

      // 5. Navigate to Login Page
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (context) => LoginPage()),
        );
      }
    } catch (e) {
      setState(() {
        _error = "Failed to connect: ${e.toString()}";
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Connect to Backend'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(
                Icons.settings_ethernet,
                size: 64,
                color: Colors.blue,
              ),
              const SizedBox(height: 24),
              Text(
                'Enter Backend URL',
                style: Theme.of(context).textTheme.headlineSmall,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                'Please provide the host address of your Tracko backend server.',
                style: Theme.of(context)
                    .textTheme
                    .bodyMedium
                    ?.copyWith(color: Colors.grey),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 32),
              TextFormField(
                controller: _urlController,
                decoration: const InputDecoration(
                  labelText: 'Server URL',
                  hintText: 'http://192.168.1.x:8080',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.link),
                ),
                keyboardType: TextInputType.url,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter a URL';
                  }
                  if (!value.startsWith('http://') &&
                      !value.startsWith('https://')) {
                    return 'URL must start with http:// or https://';
                  }
                  return null;
                },
              ),
              if (_error != null) ...[
                const SizedBox(height: 16),
                Text(
                  _error!,
                  style: const TextStyle(color: Colors.red),
                  textAlign: TextAlign.center,
                ),
              ],
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: _isLoading ? null : _connect,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                ),
                child: _isLoading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Connect'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
