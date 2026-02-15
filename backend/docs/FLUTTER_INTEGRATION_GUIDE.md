# Flutter UI Integration Guide - Complete Backend Migration

This guide provides complete Dart code to migrate your Flutter UI from SQLite to the Spring Boot backend API.

## Table of Contents
1. [Setup & Dependencies](#setup--dependencies)
2. [API Client Service](#api-client-service)
3. [Model Classes](#model-classes)
4. [Authentication Service](#authentication-service)
5. [Repository Pattern](#repository-pattern)
6. [Migration Examples](#migration-examples)
7. [Error Handling](#error-handling)

---

## Setup & Dependencies

### 1. Update `pubspec.yaml`

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # HTTP & API
  dio: ^5.4.0
  retrofit: ^4.0.3
  json_annotation: ^4.8.1
  
  # Storage
  flutter_secure_storage: ^9.0.0
  shared_preferences: ^2.2.2
  
  # State Management (choose one)
  provider: ^6.1.1
  # OR riverpod: ^2.4.9
  # OR bloc: ^8.1.3

dev_dependencies:
  build_runner: ^2.4.7
  retrofit_generator: ^8.0.4
  json_serializable: ^6.7.1
```

### 2. Environment Configuration

Create `lib/config/api_config.dart`:

```dart
class ApiConfig {
  // Development
  static const String DEV_BASE_URL = 'http://localhost:8080';
  
  // Production
  static const String PROD_BASE_URL = 'https://your-production-url.com';
  
  // Current environment
  static const bool IS_PRODUCTION = false;
  
  static String get baseUrl => IS_PRODUCTION ? PROD_BASE_URL : DEV_BASE_URL;
  
  // API Endpoints
  static const String AUTH_LOGIN = '/api/oauth/token';
  static const String AUTH_SIGNUP = '/api/signUp';
  static const String ACCOUNTS = '/api/accounts';
  static const String CATEGORIES = '/api/categories';
  static const String TRANSACTIONS = '/api/transactions';
  static const String SPLITS = '/api/splits';
  static const String JSON_STORE = '/api/json-store';
}
```

---

## API Client Service

### Create `lib/services/api_client.dart`:

```dart
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/api_config.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;
  
  late Dio _dio;
  final _storage = const FlutterSecureStorage();
  
  ApiClient._internal() {
    _dio = Dio(BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    ));
    
    _setupInterceptors();
  }
  
  void _setupInterceptors() {
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        // Add JWT token to all requests
        final token = await _storage.read(key: 'jwt_token');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onResponse: (response, handler) {
        // Handle response wrapper
        if (response.data is Map && response.data.containsKey('result')) {
          response.data = response.data['result'];
        }
        return handler.next(response);
      },
      onError: (error, handler) async {
        // Handle 401 - token expired
        if (error.response?.statusCode == 401) {
          await _storage.delete(key: 'jwt_token');
          // Navigate to login screen
        }
        return handler.next(error);
      },
    ));
  }
  
  Dio get dio => _dio;
  
  // Generic GET request
  Future<T> get<T>(String path, {Map<String, dynamic>? queryParameters}) async {
    try {
      final response = await _dio.get(path, queryParameters: queryParameters);
      return response.data as T;
    } catch (e) {
      throw _handleError(e);
    }
  }
  
  // Generic POST request
  Future<T> post<T>(String path, {dynamic data}) async {
    try {
      final response = await _dio.post(path, data: data);
      return response.data as T;
    } catch (e) {
      throw _handleError(e);
    }
  }
  
  // Generic PUT request
  Future<T> put<T>(String path, {dynamic data}) async {
    try {
      final response = await _dio.put(path, data: data);
      return response.data as T;
    } catch (e) {
      throw _handleError(e);
    }
  }
  
  // Generic DELETE request
  Future<T> delete<T>(String path) async {
    try {
      final response = await _dio.delete(path);
      return response.data as T;
    } catch (e) {
      throw _handleError(e);
    }
  }
  
  Exception _handleError(dynamic error) {
    if (error is DioException) {
      switch (error.type) {
        case DioExceptionType.connectionTimeout:
        case DioExceptionType.sendTimeout:
        case DioExceptionType.receiveTimeout:
          return Exception('Connection timeout. Please check your internet connection.');
        case DioExceptionType.badResponse:
          final statusCode = error.response?.statusCode;
          final message = error.response?.data?['message'] ?? 'Server error';
          return Exception('Error $statusCode: $message');
        case DioExceptionType.cancel:
          return Exception('Request cancelled');
        default:
          return Exception('Network error. Please try again.');
      }
    }
    return Exception('Unexpected error: $error');
  }
}
```

---

## Model Classes

### Create `lib/models/account.dart`:

```dart
import 'package:json_annotation/json_annotation.dart';

part 'account.g.dart';

@JsonSerializable()
class Account {
  final int? id;
  final String name;
  @JsonKey(name: 'user_id')
  final String userId;
  final String currency; // 'INR', 'USD', 'EUR', etc.
  @JsonKey(name: 'created_at')
  final DateTime? createdAt;
  @JsonKey(name: 'updated_at')
  final DateTime? updatedAt;
  
  Account({
    this.id,
    required this.name,
    required this.userId,
    this.currency = 'INR',
    this.createdAt,
    this.updatedAt,
  });
  
  factory Account.fromJson(Map<String, dynamic> json) => _$AccountFromJson(json);
  Map<String, dynamic> toJson() => _$AccountToJson(this);
}
```

### Create `lib/models/category.dart`:

```dart
import 'package:json_annotation/json_annotation.dart';

part 'category.g.dart';

@JsonSerializable()
class Category {
  final int? id;
  final String name;
  @JsonKey(name: 'user_id')
  final String userId;
  @JsonKey(name: 'created_at')
  final DateTime? createdAt;
  @JsonKey(name: 'updated_at')
  final DateTime? updatedAt;
  
  Category({
    this.id,
    required this.name,
    required this.userId,
    this.createdAt,
    this.updatedAt,
  });
  
  factory Category.fromJson(Map<String, dynamic> json) => _$CategoryFromJson(json);
  Map<String, dynamic> toJson() => _$CategoryToJson(this);
}
```

### Create `lib/models/transaction.dart`:

```dart
import 'package:json_annotation/json_annotation.dart';

part 'transaction.g.dart';

@JsonSerializable()
class Transaction {
  final int? id;
  @JsonKey(name: 'transaction_type')
  final String transactionType; // 'income' or 'expense'
  final String name;
  final double amount;
  final DateTime date;
  @JsonKey(name: 'account_id')
  final int accountId;
  @JsonKey(name: 'category_id')
  final int? categoryId;
  @JsonKey(name: 'is_countable')
  final int isCountable; // 0 or 1
  final String? description;
  @JsonKey(name: 'original_currency')
  final String? originalCurrency;
  @JsonKey(name: 'original_amount')
  final double? originalAmount;
  @JsonKey(name: 'exchange_rate')
  final double? exchangeRate;
  @JsonKey(name: 'created_at')
  final DateTime? createdAt;
  @JsonKey(name: 'updated_at')
  final DateTime? updatedAt;
  
  Transaction({
    this.id,
    required this.transactionType,
    required this.name,
    required this.amount,
    required this.date,
    required this.accountId,
    this.categoryId,
    this.isCountable = 1,
    this.description,
    this.originalCurrency,
    this.originalAmount,
    this.exchangeRate,
    this.createdAt,
    this.updatedAt,
  });
  
  factory Transaction.fromJson(Map<String, dynamic> json) => _$TransactionFromJson(json);
  Map<String, dynamic> toJson() => _$TransactionToJson(this);
}
```

### Create `lib/models/split.dart`:

```dart
import 'package:json_annotation/json_annotation.dart';

part 'split.g.dart';

@JsonSerializable()
class Split {
  final int? id;
  @JsonKey(name: 'transaction_id')
  final int transactionId;
  @JsonKey(name: 'user_id')
  final String userId;
  final double amount;
  @JsonKey(name: 'is_settled')
  final int isSettled; // 0 or 1
  @JsonKey(name: 'settled_at')
  final DateTime? settledAt;
  @JsonKey(name: 'created_at')
  final DateTime? createdAt;
  
  Split({
    this.id,
    required this.transactionId,
    required this.userId,
    required this.amount,
    this.isSettled = 0,
    this.settledAt,
    this.createdAt,
  });
  
  factory Split.fromJson(Map<String, dynamic> json) => _$SplitFromJson(json);
  Map<String, dynamic> toJson() => _$SplitToJson(this);
}
```

### Create `lib/models/json_store.dart`:

```dart
import 'package:json_annotation/json_annotation.dart';

part 'json_store.g.dart';

@JsonSerializable()
class JsonStore {
  final String name;
  @JsonKey(name: 'json_value')
  final String value;
  
  JsonStore({
    required this.name,
    required this.value,
  });
  
  factory JsonStore.fromJson(Map<String, dynamic> json) => _$JsonStoreFromJson(json);
  Map<String, dynamic> toJson() => _$JsonStoreToJson(this);
}
```

**Run code generation:**
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

---

## Authentication Service

### Create `lib/services/auth_service.dart`:

```dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/api_config.dart';
import 'api_client.dart';

class AuthService {
  final _apiClient = ApiClient();
  final _storage = const FlutterSecureStorage();
  
  // Sign up
  Future<String> signUp({
    required String phoneNo,
    required String firebaseUuid,
    required String name,
    String? email,
    String? profilePic,
    String? baseCurrency,
  }) async {
    try {
      final response = await _apiClient.post<Map<String, dynamic>>(
        ApiConfig.AUTH_SIGNUP,
        data: {
          'phoneNo': phoneNo,
          'uuid': firebaseUuid, // Maps to fireBaseId via @JsonProperty
          'name': name,
          'email': email,
          'profilePic': profilePic,
          'baseCurrency': baseCurrency ?? 'INR',
        },
      );
      
      // Store JWT token from header
      final token = response['token'] ?? response['jwtToken'];
      if (token != null) {
        await _storage.write(key: 'jwt_token', value: token);
      }
      
      return response['userId'] ?? response['id'];
    } catch (e) {
      throw Exception('Sign up failed: $e');
    }
  }
  
  // Sign in
  Future<String> signIn({
    required String phoneNo,
    required String firebaseUuid,
  }) async {
    try {
      final response = await _apiClient.post<Map<String, dynamic>>(
        ApiConfig.AUTH_LOGIN,
        data: {
          'phoneNo': phoneNo,
          'firebaseUuid': firebaseUuid,
        },
      );
      
      final token = response['token'];
      if (token != null) {
        await _storage.write(key: 'jwt_token', value: token);
      }
      
      return token;
    } catch (e) {
      throw Exception('Sign in failed: $e');
    }
  }
  
  // Check if logged in
  Future<bool> isLoggedIn() async {
    final token = await _storage.read(key: 'jwt_token');
    return token != null;
  }
  
  // Logout
  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
  }
  
  // Get current token
  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }
}
```

---

## Repository Pattern

### Create `lib/repositories/account_repository.dart`:

```dart
import '../config/api_config.dart';
import '../models/account.dart';
import '../services/api_client.dart';

class AccountRepository {
  final _apiClient = ApiClient();
  
  // Get all accounts
  Future<List<Account>> getAllAccounts() async {
    final response = await _apiClient.get<List<dynamic>>(ApiConfig.ACCOUNTS);
    return response.map((json) => Account.fromJson(json)).toList();
  }
  
  // Get accounts by user ID
  Future<List<Account>> getAccountsByUserId(String userId) async {
    final response = await _apiClient.get<List<dynamic>>(
      '${ApiConfig.ACCOUNTS}/user/$userId',
    );
    return response.map((json) => Account.fromJson(json)).toList();
  }
  
  // Get account by ID
  Future<Account> getAccountById(int id) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      '${ApiConfig.ACCOUNTS}/$id',
    );
    return Account.fromJson(response);
  }
  
  // Create account
  Future<Account> createAccount(Account account) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiConfig.ACCOUNTS,
      data: account.toJson(),
    );
    return Account.fromJson(response);
  }
  
  // Update account
  Future<Account> updateAccount(int id, Account account) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${ApiConfig.ACCOUNTS}/$id',
      data: account.toJson(),
    );
    return Account.fromJson(response);
  }
  
  // Delete account
  Future<void> deleteAccount(int id) async {
    await _apiClient.delete('${ApiConfig.ACCOUNTS}/$id');
  }
}
```

### Create `lib/repositories/transaction_repository.dart`:

```dart
import '../config/api_config.dart';
import '../models/transaction.dart';
import '../services/api_client.dart';

class TransactionRepository {
  final _apiClient = ApiClient();
  
  // Get all transactions
  Future<List<Transaction>> getAllTransactions() async {
    final response = await _apiClient.get<List<dynamic>>(ApiConfig.TRANSACTIONS);
    return response.map((json) => Transaction.fromJson(json)).toList();
  }
  
  // Get transactions by user ID
  Future<List<Transaction>> getTransactionsByUserId(String userId) async {
    final response = await _apiClient.get<List<dynamic>>(
      '${ApiConfig.TRANSACTIONS}/user/$userId',
    );
    return response.map((json) => Transaction.fromJson(json)).toList();
  }
  
  // Get transactions by date range
  Future<List<Transaction>> getTransactionsByDateRange({
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    final response = await _apiClient.get<Map<String, dynamic>>(
      ApiConfig.TRANSACTIONS,
      queryParameters: {
        'startDate': startDate.toIso8601String().split('T')[0],
        'endDate': endDate.toIso8601String().split('T')[0],
      },
    );
    // The unified endpoint returns a paginated structure: { "result": { "transactions": [...] } }
    // But our ApiClient wrapper extracts 'result'. So response is the Map.
    // If your ApiClient unwraps 'result', verify if it returns the Map containing 'transactions'.
    // Assuming standard response for getAll:
    final transactions = response['transactions'] as List;
    return transactions.map((json) => Transaction.fromJson(json)).toList();
  }
  
  // Get transactions by account
  Future<List<Transaction>> getTransactionsByAccountId(int accountId) async {
    final response = await _apiClient.get<List<dynamic>>(
      '${ApiConfig.TRANSACTIONS}/account/$accountId',
    );
    return response.map((json) => Transaction.fromJson(json)).toList();
  }
  
  // Get transactions by category
  Future<List<Transaction>> getTransactionsByCategoryId(int categoryId) async {
    final response = await _apiClient.get<List<dynamic>>(
      '${ApiConfig.TRANSACTIONS}/category/$categoryId',
    );
    return response.map((json) => Transaction.fromJson(json)).toList();
  }
  
  // Create transaction
  Future<Transaction> createTransaction(Transaction transaction) async {
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiConfig.TRANSACTIONS,
      data: transaction.toJson(),
    );
    return Transaction.fromJson(response);
  }
  
  // Update transaction
  Future<Transaction> updateTransaction(int id, Transaction transaction) async {
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${ApiConfig.TRANSACTIONS}/$id',
      data: transaction.toJson(),
    );
    return Transaction.fromJson(response);
  }
  
  // Delete transaction
  Future<void> deleteTransaction(int id) async {
    await _apiClient.delete('${ApiConfig.TRANSACTIONS}/$id');
  }
}
```

### Create `lib/repositories/json_store_repository.dart`:

```dart
import 'dart:convert';
import '../config/api_config.dart';
import '../models/json_store.dart';
import '../services/api_client.dart';

class JsonStoreRepository {
  final _apiClient = ApiClient();
  
  // Get all settings
  Future<List<JsonStore>> getAllSettings() async {
    final response = await _apiClient.get<List<dynamic>>(ApiConfig.JSON_STORE);
    return response.map((json) => JsonStore.fromJson(json)).toList();
  }
  
  // Get setting by name
  Future<JsonStore?> getSetting(String name) async {
    try {
      final response = await _apiClient.get<Map<String, dynamic>>(
        '${ApiConfig.JSON_STORE}/$name',
      );
      return JsonStore.fromJson(response);
    } catch (e) {
      return null; // Setting not found
    }
  }
  
  // Save setting
  Future<JsonStore> saveSetting(String name, dynamic value) async {
    final jsonValue = value is String ? value : jsonEncode(value);
    final setting = JsonStore(name: name, value: jsonValue);
    
    final response = await _apiClient.post<Map<String, dynamic>>(
      ApiConfig.JSON_STORE,
      data: setting.toJson(),
    );
    return JsonStore.fromJson(response);
  }
  
  // Update setting
  Future<JsonStore> updateSetting(String name, dynamic value) async {
    final jsonValue = value is String ? value : jsonEncode(value);
    final setting = JsonStore(name: name, value: jsonValue);
    
    final response = await _apiClient.put<Map<String, dynamic>>(
      '${ApiConfig.JSON_STORE}/$name',
      data: setting.toJson(),
    );
    return JsonStore.fromJson(response);
  }
  
  // Delete setting
  Future<void> deleteSetting(String name) async {
    await _apiClient.delete('${ApiConfig.JSON_STORE}/$name');
  }
  
  // Helper: Get parsed JSON value
  Future<T?> getSettingValue<T>(String name) async {
    final setting = await getSetting(name);
    if (setting == null) return null;
    
    try {
      return jsonDecode(setting.value) as T;
    } catch (e) {
      return setting.value as T;
    }
  }
}
```

---

## Migration Examples

### BEFORE (SQLite):

```dart
// Old DatabaseUtil.dart approach
class DatabaseUtil {
  static Future<List<Map<String, dynamic>>> getAccounts(String userId) async {
    final db = await database;
    return await db.query('accounts', where: 'user_id = ?', whereArgs: [userId]);
  }
  
  static Future<int> insertAccount(Map<String, dynamic> account) async {
    final db = await database;
    return await db.insert('accounts', account);
  }
}

// Usage in UI
final accounts = await DatabaseUtil.getAccounts(currentUserId);
```

### AFTER (Backend API):

```dart
// New approach with repository
class AccountScreen extends StatefulWidget {
  @override
  _AccountScreenState createState() => _AccountScreenState();
}

class _AccountScreenState extends State<AccountScreen> {
  final _accountRepo = AccountRepository();
  List<Account> _accounts = [];
  bool _loading = false;
  
  @override
  void initState() {
    super.initState();
    _loadAccounts();
  }
  
  Future<void> _loadAccounts() async {
    setState(() => _loading = true);
    try {
      final accounts = await _accountRepo.getAccountsByUserId(currentUserId);
      setState(() {
        _accounts = accounts;
        _loading = false;
      });
    } catch (e) {
      setState(() => _loading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error loading accounts: $e')),
      );
    }
  }
  
  Future<void> _createAccount(String name) async {
    try {
      final account = Account(name: name, userId: currentUserId);
      await _accountRepo.createAccount(account);
      await _loadAccounts(); // Refresh list
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Account created successfully')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error creating account: $e')),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return Center(child: CircularProgressIndicator());
    }
    
    return ListView.builder(
      itemCount: _accounts.length,
      itemBuilder: (context, index) {
        final account = _accounts[index];
        return ListTile(
          title: Text(account.name),
          subtitle: Text('ID: ${account.id}'),
        );
      },
    );
  }
}
```

### Settings/Preferences Migration:

```dart
// BEFORE: Local SharedPreferences
final prefs = await SharedPreferences.getInstance();
await prefs.setString('theme', 'dark');
final theme = prefs.getString('theme');

// AFTER: Backend JsonStore
final jsonStoreRepo = JsonStoreRepository();

// Save setting
await jsonStoreRepo.saveSetting('theme', {'mode': 'dark'});

// Get setting
final themeValue = await jsonStoreRepo.getSettingValue<Map<String, dynamic>>('theme');
final themeMode = themeValue?['mode'] ?? 'light';
```

---

## Error Handling

### Create `lib/utils/error_handler.dart`:

```dart
import 'package:flutter/material.dart';

class ErrorHandler {
  static void showError(BuildContext context, dynamic error) {
    String message = 'An error occurred';
    
    if (error is Exception) {
      message = error.toString().replaceAll('Exception: ', '');
    }
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: Duration(seconds: 3),
        action: SnackBarAction(
          label: 'DISMISS',
          textColor: Colors.white,
          onPressed: () {},
        ),
      ),
    );
  }
  
  static Future<T?> handleAsync<T>(
    BuildContext context,
    Future<T> Function() operation, {
    String? successMessage,
  }) async {
    try {
      final result = await operation();
      if (successMessage != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(successMessage), backgroundColor: Colors.green),
        );
      }
      return result;
    } catch (e) {
      showError(context, e);
      return null;
    }
  }
}

// Usage:
await ErrorHandler.handleAsync(
  context,
  () => accountRepo.createAccount(account),
  successMessage: 'Account created successfully',
);
```

---

## Complete Migration Checklist

### 1. Remove SQLite Dependencies
```yaml
# Remove from pubspec.yaml:
# sqflite: ^x.x.x
# path: ^x.x.x
```

### 2. Delete Old Database Files
- Delete `lib/Utils/DatabaseUtil.dart`
- Delete any SQLite helper classes

### 3. Add New Dependencies
```bash
flutter pub add dio flutter_secure_storage json_annotation
flutter pub add --dev build_runner json_serializable
```

### 4. Generate Model Code
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

### 5. Update Main App
```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // Initialize API client
  ApiClient();
  
  runApp(MyApp());
}
```

### 6. Test Each Screen
- [ ] Login/Signup screens
- [ ] Accounts list and CRUD
- [ ] Categories list and CRUD
- [ ] Transactions list and CRUD
- [ ] Splits management
- [ ] Settings/preferences

---

## Testing Tips

1. **Use dev server first**: Set `IS_PRODUCTION = false` in `api_config.dart`
2. **Test with Postman**: Verify backend endpoints work before Flutter integration
3. **Handle offline mode**: Consider caching strategies for offline support
4. **Monitor network calls**: Use Dio interceptors to log all API calls during development
5. **Error scenarios**: Test network failures, 401 errors, validation errors

---

## Next Steps

1. Start with authentication - migrate login/signup first
2. Then migrate one feature at a time (accounts → categories → transactions)
3. Test thoroughly after each migration
4. Keep SQLite code temporarily until fully tested
5. Deploy backend to production before releasing Flutter app

---

## Support

For backend API documentation, see:
- Swagger UI: `http://localhost:8080/swagger-ui.html` (if enabled)
- Migration guide: `docs/MIGRATION_COMPLETE.md`
- All endpoints return: `{ "result": <data>, "message": "..." }`
