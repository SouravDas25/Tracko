import 'package:tracko/controllers/SplitController.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/user_repository.dart';

class UserController {
  static Future<int> saveUser(User user, {bool isShadow = false}) async {
    final userRepo = UserRepository();
    
    // Save user via backend API
    String globalId = await userRepo.save(user, isShadow: isShadow);
    user.globalId = globalId;
    
    if (user.globalId == null || user.globalId!.isEmpty) {
      throw Exception("Failed to add user ${user.phoneNo}.");
    }
    
    // Parse ID from globalId
    user.id = int.tryParse(globalId);
    return user.id ?? 0;
  }

  static Future<User> findByPhoneNumber(String phoneNo) async {
    final userRepo = UserRepository();
    User? user = await userRepo.getByPhoneNumber(phoneNo);
    return user ?? User();
  }

  static Future<User> findById(int userId) async {
    final userRepo = UserRepository();
    User? user = await userRepo.getById(userId.toString());
    return user ?? User();
  }

  static Future<User> findByGlobalId(String globalId) async {
    final userRepo = UserRepository();
    if (globalId.trim().isEmpty) return User();
    User? user = await userRepo.getById(globalId);
    return user ?? User();
  }

  static getAllUsers() async {
    final userRepo = UserRepository();
    return await userRepo.getAll();
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
    // Note: Backend doesn't have delete user endpoint yet
    // This would need to be added to backend UserController
    // For now, return the id (no-op)
    return id;
  }
}
