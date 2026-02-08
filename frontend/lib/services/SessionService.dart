import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/user_repository.dart';

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

  static User currentUser() {
    if (_loggedInUser == null) throw Exception("User not logged-in");
    return _loggedInUser!;
  }

  static TrakoContact currentUserContact() {
    User user = currentUser();
    TrakoContact contact = UserController.user2Contact(user);
    contact.name = "You";
    return contact;
  }

  static logout() async {
    clearCache();
  }

  static Future<User> getCurrentUser({bool forceRefresh = false}) async {
    if (_loggedInUser != null && !forceRefresh) {
      return _loggedInUser!;
    }

    try {
      // Try to fetch from backend
      final userRepo = UserRepository();
      final backendUser = await userRepo.getMe();
      if (backendUser != null) {
        setCurrentUser(backendUser);
        return backendUser;
      }
    } catch (e) {
      print("Failed to fetch user profile: $e");
    }

    // Return a default user if not logged in or fetch failed
    // Do NOT cache this dummy user, so retry is possible
    User defaultUser = User();
    defaultUser.id = 1;
    defaultUser.name = "User";
    defaultUser.phoneNo = "";
    defaultUser.email = "";
    return defaultUser;
  }

  static loginUser(User user) async {
    // Bypassing Firebase auth. For integration testing, we assume any user with a phone number is valid.
    if (user.phoneNo != null && user.phoneNo.isNotEmpty) {
      return true;
    }
    return false;
  }

  static Future<User> createCurrentUser(String phoneNo, {String? uuid}) async {
    User user = new User();
    user.id = 1;
    user.profilePic = "";
    user.name = "Default Username";
    user.email = "";
    user.phoneNo = phoneNo;
    // Firebase ID is no longer used for authentication
    user.fireBaseId = 'bypass-auth';
    user.globalId = '';

    _loggedInUser = user;
    return user;
  }
}
