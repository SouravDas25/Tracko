import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/user_repository.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/services/auth_service.dart';

class SessionService {
  final AuthService _auth;
  final UserRepository _userRepo;

  User? _loggedInUser;
  String _currentCurrencySymbol = '₹';

  SessionService({required AuthService auth, required UserRepository userRepo})
      : _auth = auth,
        _userRepo = userRepo;

  String get currentCurrencySymbol => _currentCurrencySymbol;

  void clearCache() {
    _loggedInUser = null;
    _currentCurrencySymbol = '₹';
  }

  void setCurrentUser(User user) {
    _loggedInUser = user;
    _currentCurrencySymbol = ConstantUtil.getCurrencySymbol(
        user.baseCurrency.isNotEmpty ? user.baseCurrency : 'INR');
  }

  Future<void> logout() async {
    try {
      await _auth.logout();
    } catch (_) {
      // ignore
    }
    ApiClient.resetAuthSuppression();
    clearCache();
  }

  Future<User> fetchMe({bool forceRefresh = false}) async {
    if (_loggedInUser != null && !forceRefresh) {
      return _loggedInUser!;
    }

    final backendUser = await _userRepo.getMe();
    setCurrentUser(backendUser);
    return backendUser;
  }

}
