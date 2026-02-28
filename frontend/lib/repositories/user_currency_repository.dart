import 'package:tracko/config/api_config.dart';
import 'package:tracko/models/user_currency.dart';
import 'package:tracko/services/api_client.dart';

class UserCurrencyRepository {
  final ApiClient _api;

  UserCurrencyRepository({ApiClient? api}) : _api = api ?? ApiClient();

  Future<List<UserCurrency>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.userCurrencies);
    return res
        .map((e) => UserCurrency.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> save(UserCurrency userCurrency) async {
    await _api.post<void>(
      ApiConfig.userCurrencies,
      data: userCurrency.toJson(),
    );
  }

  Future<void> delete(String currencyCode) async {
    await _api.delete<void>("${ApiConfig.userCurrencies}/$currencyCode");
  }
}
