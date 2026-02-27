# Flutter UI Migration Guide: SQLite → Backend APIs

## Overview
Replace direct SQLite database access in the Flutter UI with HTTP calls to the Spring Boot backend. The backend now owns all data via H2 (dev) or Postgres (prod) with Liquibase migrations.

## Backend Endpoints Available

### Authentication
- **POST** `/api/oauth/token` - Login (returns JWT)
  - Body: `{ "phoneNo": "string", "firebaseUuid": "string" }`
  - Response: `{ "token": "jwt-string" }`

### Users
- **GET** `/api/user` - List all users (requires JWT)
- **GET** `/api/user/{id}` - Get user by ID (requires JWT)
- **GET** `/api/user/byPhoneNo?phone_no={phoneNo}` - Get user by phone (requires JWT)
- **POST** `/api/user/save` - Create/update user (requires JWT)

### Splits
- **GET** `/api/split` - Get all splits for logged-in user (requires JWT)
- **GET** `/api/split/{userId}` - Get splits by user ID (requires JWT)
- **POST** `/api/split` - Create splits (requires JWT)
  - Body: `[{ "userId": "string", "amount": number, "transactionAmount": number, "transactionName": "string" }]`
- **PATCH** `/api/split/settle/{splitId}` - Settle a split (requires JWT)
  - Body: `{ "amount": number }`

### Chat
- **POST** `/api/chat/create` - Create chat group (requires JWT)
  - Body: `{ "name": "string", "users": ["phoneNo1", "phoneNo2"] }`
- **POST** `/api/chat/send` - Send message (requires JWT)
  - Body: `{ "sender": "userId", "chatGroupAddress": "groupId", "message": "string" }`
- **GET/POST** `/api/chat/messages` - Retrieve messages (requires JWT)
  - Body: `{ "chatGroup": "groupId", "retrieveFrom": "messageId" }`
- **GET** `/api/chat/groups/{userId}` - Get groups for user (requires JWT)

## Flutter Implementation

### 1. Add Dependencies
Update `pubspec.yaml`:
```yaml
dependencies:
  dio: ^5.4.0  # HTTP client
  flutter_secure_storage: ^9.0.0  # Secure JWT storage
  # Remove sqflite and any SQLite dependencies
```

### 2. API Client Service
Create `lib/services/api_client.dart`:
```dart
import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class ApiClient {
  static const String baseUrl = 'http://localhost:8080'; // Dev
  // static const String baseUrl = 'https://your-prod-url.com'; // Prod
  
  final Dio _dio;
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  ApiClient({String? jwt}) : _dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    headers: jwt != null ? {'Authorization': 'Bearer $jwt'} : {},
    connectTimeout: const Duration(seconds: 10),
    receiveTimeout: const Duration(seconds: 10),
  )) {
    _dio.interceptors.add(LogInterceptor(
      requestBody: true,
      responseBody: true,
    ));
  }

  // Auth
  Future<String> signIn(String phoneNo, String firebaseUuid) async {
    final response = await _dio.post('/api/oauth/token', data: {
      'phoneNo': phoneNo,
      'firebaseUuid': firebaseUuid,
    });
    final token = response.data['token'] as String;
    await _storage.write(key: 'jwt_token', value: token);
    return token;
  }

  Future<String> signUp({
    required String phoneNo,
    required String firebaseUuid,
    String? name,
    String? email,
    String? profilePic,
  }) async {
    final response = await _dio.post('/api/signUp', data: {
      'phoneNo': phoneNo,
      'firebase_uuid': firebaseUuid,
      'name': name,
      'email': email,
      'profilePic': profilePic,
    });
    return response.data as String; // User ID
  }

  // Users
  Future<List<dynamic>> getUsers() async {
    final response = await _dio.get('/api/user');
    return response.data as List<dynamic>;
  }

  Future<dynamic> getUserById(String id) async {
    final response = await _dio.get('/api/user/$id');
    return response.data;
  }

  Future<dynamic> getUserByPhone(String phoneNo) async {
    final response = await _dio.get('/api/user/byPhoneNo', 
      queryParameters: {'phone_no': phoneNo});
    return response.data;
  }

  Future<String> saveUser(Map<String, dynamic> userData) async {
    final response = await _dio.post('/api/user/save', data: userData);
    return response.data as String;
  }

  // Splits
  Future<List<dynamic>> getSplits() async {
    final response = await _dio.get('/api/split');
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getSplitsByUser(String userId) async {
    final response = await _dio.get('/api/split/$userId');
    return response.data as List<dynamic>;
  }

  Future<void> createSplits(List<Map<String, dynamic>> splits) async {
    await _dio.post('/api/split', data: splits);
  }

  Future<void> settleSplit(String splitId, double amount) async {
    await _dio.patch('/api/split/settle/$splitId', data: {'amount': amount});
  }

  // Chat
  Future<String> createChatGroup(String name, List<String> phoneNumbers) async {
    final response = await _dio.post('/api/chat/create', data: {
      'name': name,
      'users': phoneNumbers,
    });
    return response.data as String; // Group ID
  }

  Future<void> sendMessage(String sender, String groupId, String message) async {
    await _dio.post('/api/chat/send', data: {
      'sender': sender,
      'chatGroupAddress': groupId,
      'message': message,
    });
  }

  Future<List<dynamic>> getChatMessages(String groupId, {String? retrieveFrom}) async {
    final response = await _dio.post('/api/chat/messages', data: {
      'chatGroup': groupId,
      'retrieveFrom': retrieveFrom,
    });
    return response.data as List<dynamic>;
  }

  Future<List<dynamic>> getChatGroups(String userId) async {
    final response = await _dio.get('/api/chat/groups/$userId');
    return response.data as List<dynamic>;
  }
}
```

### 3. Auth Service
Create `lib/services/auth_service.dart`:
```dart
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'api_client.dart';

class AuthService {
  final FlutterSecureStorage _storage = const FlutterSecureStorage();
  ApiClient? _authenticatedClient;

  Future<bool> isLoggedIn() async {
    final token = await _storage.read(key: 'jwt_token');
    return token != null;
  }

  Future<String?> getToken() async {
    return await _storage.read(key: 'jwt_token');
  }

  Future<ApiClient> getAuthenticatedClient() async {
    final token = await getToken();
    if (token == null) {
      throw Exception('Not authenticated');
    }
    _authenticatedClient ??= ApiClient(jwt: token);
    return _authenticatedClient!;
  }

  Future<void> login(String phoneNo, String firebaseUuid) async {
    final client = ApiClient();
    await client.signIn(phoneNo, firebaseUuid);
    _authenticatedClient = null; // Reset to force new client with token
  }

  Future<void> logout() async {
    await _storage.delete(key: 'jwt_token');
    _authenticatedClient = null;
  }
}
```

### 4. Usage Example
Replace SQLite queries with API calls:

**Before (SQLite):**
```dart
// Old SQLite code
final db = await database;
final List<Map<String, dynamic>> maps = await db.query('users');
return List.generate(maps.length, (i) => User.fromMap(maps[i]));
```

**After (API):**
```dart
// New API code
final authService = AuthService();
final apiClient = await authService.getAuthenticatedClient();
final usersData = await apiClient.getUsers();
return usersData.map((data) => User.fromJson(data)).toList();
```

### 5. Migration Checklist
- [ ] Remove `sqflite` package from `pubspec.yaml`
- [ ] Add `dio` and `flutter_secure_storage` packages
- [ ] Create `api_client.dart` and `auth_service.dart`
- [ ] Update login flow to call `/api/oauth/token` and store JWT
- [ ] Replace all database queries with API calls:
  - [ ] User CRUD operations
  - [ ] Split operations
  - [ ] Chat operations
- [ ] Update models to use `fromJson` instead of `fromMap`
- [ ] Add error handling for network failures
- [ ] Test authentication flow end-to-end
- [ ] Test all features with backend running

### 6. Error Handling
```dart
try {
  final apiClient = await authService.getAuthenticatedClient();
  final users = await apiClient.getUsers();
  // Handle success
} on DioException catch (e) {
  if (e.response?.statusCode == 401) {
    // Token expired, redirect to login
    await authService.logout();
    // Navigate to login screen
  } else {
    // Handle other errors
    print('Error: ${e.message}');
  }
} catch (e) {
  print('Unexpected error: $e');
}
```

### 7. Environment Configuration
Create `lib/config/environment.dart`:
```dart
class Environment {
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080', // Dev default
  );
}
```

Run with different environments:
```bash
# Dev
flutter run --dart-define=API_BASE_URL=http://localhost:8080

# Prod
flutter run --dart-define=API_BASE_URL=https://your-prod-url.com
```

## Testing
1. Start backend: `mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"`
2. Backend runs on `http://localhost:8080`
3. Run Flutter app and test:
   - Sign up new user
   - Login with existing user
   - Create splits
   - Send chat messages
   - Verify all data persists in backend DB (H2 console: `http://localhost:8080/h2-console`)

## Next Steps
- Remove all SQLite database files and code
- Add offline caching if needed (using `hive` or `shared_preferences` for non-sensitive data)
- Implement refresh token flow if backend supports it
- Add loading states and retry logic for network calls
