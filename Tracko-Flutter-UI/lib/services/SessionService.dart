import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/user.dart';

class SessionService {
  static User? _loggedInUser;

  static void clearCache() {
    _loggedInUser = null;
  }

  static void setCurrentUser(User user) {
    _loggedInUser = user;
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

  static Future<User> getCurrentUser() async {
    if (_loggedInUser != null) {
      return _loggedInUser!;
    }

    // Return a default user if not logged in
    _loggedInUser = User();
    _loggedInUser!.id = 1;
    _loggedInUser!.name = "User";
    _loggedInUser!.phoneNo = "";
    _loggedInUser!.email = "";
    return _loggedInUser!;
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
