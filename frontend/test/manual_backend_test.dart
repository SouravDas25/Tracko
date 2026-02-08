import 'package:tracko/services/SessionService.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/Utils/ServerUtil.dart';

/// Manual Backend Integration Test
/// 
/// Run this script to manually test backend integration:
/// dart run test/manual_backend_test.dart
/// 
/// Prerequisites:
/// - Backend server running on http://localhost:8080
/// - PostgreSQL database accessible

void main() async {
  print('========================================');
  print('Tracko Backend Integration Test');
  print('========================================\n');

  // Test 1: Session Service (No Database)
  print('Test 1: SessionService (No Database Required)');
  print('-------------------------------------------');
  
  try {
    final user = await SessionService.createCurrentUser('0000000000', uuid: 'bypass-test');
    print('✅ Created user in memory: ${user.phoneNo}');
    
    final currentUser = await SessionService.getCurrentUser();
    print('✅ Retrieved user from memory: ${currentUser.phoneNo}');
    
    await SessionService.logout();
    print('✅ Logged out (cleared memory cache)');
    
    print('✅ Test 1 PASSED: No database dependencies\n');
  } catch (e) {
    print('❌ Test 1 FAILED: $e\n');
  }

  // Test 2: Backend API - Sign Up
  print('Test 2: Backend API - Sign Up');
  print('-------------------------------------------');
  
  try {
    final user = User();
    user.phoneNo = '0000000000';
    user.fireBaseId = 'bypass-${DateTime.now().millisecondsSinceEpoch}';
    
    final result = await ServerUtil.signUp(user);
    
    if (result != null) {
      print('✅ Sign up successful: $result');
      print('✅ JWT Token set: ${ServerUtil.authJwtToken != null}');
      print('✅ Test 2 PASSED: Backend API working\n');
    } else {
      print('❌ Test 2 FAILED: Sign up returned null');
      print('   Check if backend is running on http://localhost:8080\n');
    }
  } catch (e) {
    print('❌ Test 2 FAILED: $e');
    print('   Make sure backend server is running\n');
  }

  // Test 3: Backend API - Create Global Account
  print('Test 3: Backend API - Create Global Account');
  print('-------------------------------------------');
  
  try {
    final user = User();
    user.phoneNo = '0000000000';
    user.fireBaseId = 'bypass-${DateTime.now().millisecondsSinceEpoch}';
    user.name = 'Test User';
    user.email = 'test@example.com';
    
    // Sign up first
    await ServerUtil.signUp(user);
    
    // Create global account
    final globalId = await ServerUtil.createGlobalAccount(user);
    
    if (globalId != null && globalId.isNotEmpty) {
      print('✅ Global account created: $globalId');
      print('✅ Test 3 PASSED: Account creation working\n');
    } else {
      print('❌ Test 3 FAILED: Global account creation returned null\n');
    }
  } catch (e) {
    print('❌ Test 3 FAILED: $e\n');
  }

  // Test 4: Backend API - Get Auth Token
  print('Test 4: Backend API - Get Auth Token');
  print('-------------------------------------------');
  
  try {
    final user = User();
    user.phoneNo = '0000000000';
    user.fireBaseId = 'bypass-${DateTime.now().millisecondsSinceEpoch}';
    
    // Sign up first
    await ServerUtil.signUp(user);
    
    // Get auth token
    final token = await ServerUtil.getAuthToken(user);
    
    if (token != null && token.isNotEmpty) {
      print('✅ Auth token retrieved: ${token.substring(0, 20)}...');
      print('✅ Test 4 PASSED: Authentication working\n');
    } else {
      print('❌ Test 4 FAILED: Auth token is null\n');
    }
  } catch (e) {
    print('❌ Test 4 FAILED: $e\n');
  }

  print('========================================');
  print('Test Summary');
  print('========================================');
  print('✅ SessionService: No database dependencies');
  print('✅ Backend APIs: Check results above');
  print('\nIf backend tests failed, ensure:');
  print('  1. Backend server is running on http://localhost:8080');
  print('  2. CORS is configured correctly');
  print('  3. PostgreSQL database is accessible');
  print('========================================\n');
}
