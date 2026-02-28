import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/ServerUtil.dart';
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

  // static User currentUser() {
  //   if (_loggedInUser == null) throw Exception("User not logged-in");
  //   return _loggedInUser!;
  // }

  Future<void> logout() async {
    try {
      await _auth.logout();
    } catch (_) {
      // ignore
    }
    // Backward-compat: older parts of the app store auth token here.
    ServerUtil.authJwtToken = null;
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

  // static loginUser(User user) async {
  //   // For integration testing, we assume any user with a phone number is valid.
  //   if (user.phoneNo != null && user.phoneNo.isNotEmpty) {
  //     return true;
  //   }
  //   return false;
  // }

  // static Future<User> createCurrentUser(String phoneNo, {String? uuid}) async {
  //   User user = new User();
  //   user.id = 1;
  //   user.profilePic = "";
  //   user.name = "Default Username";
  //   user.email = "";
  //   user.phoneNo = phoneNo;
  //   user.globalId = '';

  //   _loggedInUser = user;
  //   return user;
  // }
}
