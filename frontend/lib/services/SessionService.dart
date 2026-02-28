import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/user_repository.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/services/auth_service.dart';

class SessionService {
  static User? _loggedInUser;
  static String currentCurrencySymbol = '₹';

  static void clearCache() {
    _loggedInUser = null;
    currentCurrencySymbol = '₹';
  }

  static void setCurrentUser(User user) {
    _loggedInUser = user;
    currentCurrencySymbol = ConstantUtil.getCurrencySymbol(
        user.baseCurrency.isNotEmpty ? user.baseCurrency : 'INR');
  }

  // static User currentUser() {
  //   if (_loggedInUser == null) throw Exception("User not logged-in");
  //   return _loggedInUser!;
  // }

  static logout() async {
    try {
      await AuthService().logout();
    } catch (_) {
      // ignore
    }
    // Backward-compat: older parts of the app store auth token here.
    ServerUtil.authJwtToken = null;
    ApiClient.resetAuthSuppression();
    clearCache();
  }

  static Future<User> fetchMe({bool forceRefresh = false}) async {
    if (_loggedInUser != null && !forceRefresh) {
      return _loggedInUser!;
    }

    final userRepo = UserRepository();
    final backendUser = await userRepo.getMe();
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
