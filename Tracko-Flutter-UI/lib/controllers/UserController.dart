import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/ServerUtil.dart';
import 'package:Tracko/controllers/SplitController.dart';
import 'package:Tracko/dtos/TrackoContact.dart';
import 'package:Tracko/models/user.dart';

class UserController {
  static Future<int> saveUser(User user, {bool isShadow = false}) async {
    if (user.globalId == null) {
      String globalId = await ServerUtil.getGlobalAccountId(user.phoneNo) ?? '';
      if (globalId.isEmpty) {
        globalId = await ServerUtil.createGlobalAccount(user) ?? '';
      } else {
        await ServerUtil.updateGlobalUser(user, isShadow: isShadow);
      }
      user.globalId = globalId;
    }
    if (user.globalId == null) {
      throw Exception("Failed to add user ${user.phoneNo}.");
    }
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    user.id = await UserBean(adapter).upsert(user);
    return user.id ?? 0;
  }

  static Future<User> findByPhoneNumber(String phoneNo) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = UserBean(adapter);
    User? user = await userBean.findOneWhere(userBean.phoneNo.eq(phoneNo));
    return user ?? User();
  }

  static Future<User> findById(int userId) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = UserBean(adapter);
    User? user = await userBean.find(userId);
    return user ?? User();
  }

  static getAllUsers() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = UserBean(adapter);
    return userBean.getAll();
  }

  static Future<List<User>> getFrequentSplitters() async {
    List<User> users = await UserController.getAllUsers();
    User rootUser = await UserController.findById(1);
    print(rootUser);
    users.removeWhere((user) => rootUser.phoneNo.contains(user.phoneNo));
    for (User user in users) {
      user.splits = await SplitController.findByUserId(user.id ?? 0, preload: false);
    }
    users.sort(
            (user1, user2) =>
            user1.splits.length.compareTo(user2.splits.length));
    return users;
  }

  static TrakoContact user2Contact(User user) {
    TrakoContact contact = TrakoContact();
    contact.name = user.name;
    contact.phoneNo = user.phoneNo;
    return contact;
  }

  static Future<int> removeById(int id) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = new UserBean(adapter);
    await userBean.remove(id);
    return id;
  }
}
