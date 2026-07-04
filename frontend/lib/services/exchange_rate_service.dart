import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/services/api_client.dart';

class ExchangeRateService {
  final ApiClient _api;

  ExchangeRateService({ApiClient? api}) : _api = api ?? ApiClient();

  Future<double?> getExchangeRate(
      String fromCurrency, String toCurrency) async {
    try {
      AppLog.d(
          '[ExchangeRateService] getExchangeRate: $fromCurrency -> $toCurrency');
      final data = await _api.get<Map<String, dynamic>>(
          "${ApiConfig.exchangeRates}/$fromCurrency");

      AppLog.d('[ExchangeRateService] data: $data');

      // ApiClient interceptor already unwraps the envelope, so data = result object
      final rates = data['rates'] as Map<String, dynamic>?;
      AppLog.d(
          '[ExchangeRateService] rates keys count: ${rates?.length}, looking for: $toCurrency');

      if (rates != null && rates.containsKey(toCurrency)) {
        final rate = (rates[toCurrency] as num).toDouble();
        AppLog.d('[ExchangeRateService] found rate: $rate');
        return rate;
      }

      AppLog.d(
          '[ExchangeRateService] toCurrency "$toCurrency" not found in rates');
      return null;
    } catch (e, st) {
      AppLog.d('[ExchangeRateService] Error fetching exchange rate: $e\n$st');
      return null;
    }
  }

  Future<Map<String, double>?> getRatesForBase(String baseCurrency) async {
    try {
      AppLog.d('[ExchangeRateService] getRatesForBase: $baseCurrency');
      final data = await _api.get<Map<String, dynamic>>(
          "${ApiConfig.exchangeRates}/$baseCurrency");

      AppLog.d('[ExchangeRateService] data: $data');

      // ApiClient interceptor already unwraps the envelope, so data = result object
      final rates = data['rates'] as Map<String, dynamic>?;
      AppLog.d('[ExchangeRateService] rates keys count: ${rates?.length}');

      if (rates != null) {
        return rates
            .map((key, value) => MapEntry(key, (value as num).toDouble()));
      }

      AppLog.d('[ExchangeRateService] rates field is null');
      return null;
    } catch (e, st) {
      AppLog.d('[ExchangeRateService] Error fetching exchange rates: $e\n$st');
      return null;
    }
  }
}
