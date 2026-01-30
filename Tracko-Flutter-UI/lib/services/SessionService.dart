import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/ServerUtil.dart';
import 'package:Tracko/controllers/UserController.dart';
import 'package:Tracko/dtos/GlobalAccountResponse.dart';
import 'package:Tracko/dtos/TrackoContact.dart';
import 'package:Tracko/models/user.dart';

class SessionService {
  static User? _loggedInUser;

  static void clearCache() {
    _loggedInUser = User();
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
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = UserBean(adapter);
    await userBean.remove(1);
    clearCache();
  }

  static Future<User> getCurrentUser({adapter}) async {
    if (adapter == null) {
      adapter = await DatabaseUtil.getAdapter();
      await adapter.connect();
      _loggedInUser = await UserBean(adapter).find(1) ?? User();
//      await adapter.close();
    } else {
      _loggedInUser = await UserBean(adapter).find(1) ?? User();
    }
    return _loggedInUser ?? User();
  }

  static loginUser(User user) async {
    if (user == null) return false;
    if (user.phoneNo == null || user.phoneNo.length <= 0) return false;
    if (user.fireBaseId == null || user.fireBaseId.length <= 0) return false;
    GlobalAccountResponse? globalAccount;
    int count = 0;
    while (globalAccount == null) {
      try {
        String token = await ServerUtil.getAuthToken(user) ?? '';
        if (token == null || token.length <= 0) {
          throw Exception("Unverified User token not fetched.");
        }
        globalAccount = await ServerUtil.getGlobalAccount(user.phoneNo);
      } catch (e) {
        await Future<void>.delayed(new Duration(seconds: 1));
        print("Reattempting user verification : ${count++}");
      }
    }
    if (globalAccount != null && globalAccount.id == user.globalId) return true;
    return false;
  }

  static Future<User> createCurrentUser(String phoneNo, {String? uuid}) async {
    User user = new User();
    user.id = 1;
    user.profilePic = "";
    user.name = "Default Username";
    user.email = "";
    user.phoneNo = phoneNo;
    user.fireBaseId = uuid ?? '';
    user.globalId = '';
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = new UserBean(adapter);
    await userBean.upsert(user);
    return user;
  }
}
