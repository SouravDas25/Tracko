import 'package:tracko/config/api_config.dart';
import 'package:tracko/services/api_client.dart';

class ExchangeRateService {
  final ApiClient _api;

  ExchangeRateService({ApiClient? api}) : _api = api ?? ApiClient();

  Future<double?> getExchangeRate(
      String fromCurrency, String toCurrency) async {
    try {
      final data = await _api.get<Map<String, dynamic>>(
          "${ApiConfig.exchangeRates}/$fromCurrency");

      if (data['result'] == 'success') {
        final rates = data['rates'] as Map<String, dynamic>;
        if (rates.containsKey(toCurrency)) {
          return (rates[toCurrency] as num).toDouble();
        }
      }
      return null;
    } catch (e) {
      print('Error fetching exchange rate: $e');
      return null;
    }
  }

  Future<Map<String, double>?> getRatesForBase(String baseCurrency) async {
    try {
      final data = await _api.get<Map<String, dynamic>>(
          "${ApiConfig.exchangeRates}/$baseCurrency");

      if (data['result'] == 'success') {
        final rates = data['rates'] as Map<String, dynamic>;
        // Convert values to double
        return rates
            .map((key, value) => MapEntry(key, (value as num).toDouble()));
      }
      return null;
    } catch (e) {
      print('Error fetching exchange rates: $e');
      return null;
    }
  }
}
