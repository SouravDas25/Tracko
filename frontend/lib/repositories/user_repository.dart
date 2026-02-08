import 'package:tracko/models/user.dart';
import 'package:tracko/models/user_currency.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/config/api_config.dart';

class UserRepository {
  final ApiClient _api = ApiClient();

  Future<User?> getById(String id) async {
    try {
      final res = await _api.get<List<dynamic>>("${ApiConfig.users}/$id");
      if (res.isEmpty) return null;
      return _fromBackend(res.first as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<User?> getMe() async {
    try {
      final res = await _api.get<Map<String, dynamic>>("${ApiConfig.users}/me");
      return _fromBackend(res);
    } catch (e) {
      return null;
    }
  }

  Future<User?> getByPhoneNumber(String phoneNo) async {
    try {
      final res = await _api.get<List<dynamic>>(
        "${ApiConfig.users}/byPhoneNo",
        query: {'phone_no': phoneNo},
      );
      if (res.isEmpty) return null;
      return _fromBackend(res.first as Map<String, dynamic>);
    } catch (e) {
      return null;
    }
  }

  Future<List<User>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.users);
    return res.map((e) => _fromBackend(e as Map<String, dynamic>)).toList();
  }

  Future<String> save(User user, {bool isShadow = false}) async {
    final payload = {
      'name': user.name,
      'phoneNo': user.phoneNo,
      'email': user.email ?? '',
      'profilePic': user.profilePic ?? '',
      'isShadow': isShadow,
      'baseCurrency': user.baseCurrency,
    };

    final res =
        await _api.post<String>("${ApiConfig.users}/save", data: payload);
    return res;
  }

  User _fromBackend(Map<String, dynamic> json) {
    final user = User();
    user.id = int.tryParse(json['id']?.toString() ?? '0');
    user.globalId = json['id']?.toString() ?? '';
    user.name = (json['name'] as String?) ?? '';
    user.phoneNo = (json['phoneNo'] as String?) ?? '';
    user.email = (json['email'] as String?) ?? '';
    user.profilePic = (json['profilePic'] as String?) ?? '';
    user.baseCurrency = (json['baseCurrency'] as String?) ?? 'INR';
    
    if (json['secondaryCurrencies'] != null) {
      user.secondaryCurrencies = (json['secondaryCurrencies'] as List)
          .map((e) => UserCurrency.fromJson(e as Map<String, dynamic>))
          .toList();
    }
    
    return user;
  }
}
