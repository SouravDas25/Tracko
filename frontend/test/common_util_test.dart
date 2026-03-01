import 'package:flutter_test/flutter_test.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/di/di.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:mockito/mockito.dart';
import 'package:tracko/services/auth_service.dart';
import 'package:tracko/repositories/user_repository.dart';
import 'package:tracko/services/api_client.dart';

class MockSessionService extends Mock implements SessionService {
  @override
  String get currentCurrencySymbol => '₹';
}

void main() {
  setUp(() {
    sl.registerLazySingleton<SessionService>(() => MockSessionService());
  });

  tearDown(() {
    sl.reset();
  });

  group('CommonUtil.toCurrency', () {
    test('formats INR with Indian grouping and no unit', () {
      expect(CommonUtil.toCurrency(1234.0, currencyCode: 'INR'), '₹1,234');
      expect(
          CommonUtil.toCurrency(123456.0, currencyCode: 'INR'), '₹1.23 lakh');
      expect(CommonUtil.toCurrency(12345678.0, currencyCode: 'INR'),
          '₹1.23 crore');
      expect(
          CommonUtil.toCurrency(123456.78, currencyCode: 'INR'), '₹1.23 lakh');
    });
    test('formats USD with Western grouping and units', () {
      expect(CommonUtil.toCurrency(1234.56, currencyCode: 'USD'), '\$1,234.56');
      expect(CommonUtil.toCurrency(1234567.0, currencyCode: 'USD'),
          '\$1.23 million');
      expect(CommonUtil.toCurrency(1234567890.0, currencyCode: 'USD'),
          '\$1.23 billion');
    });
    test('handles negative and fractional amounts', () {
      expect(CommonUtil.toCurrency(-987654.32, currencyCode: 'INR'),
          '₹-9.88 lakh');
      expect(CommonUtil.toCurrency(1234.56, currencyCode: 'USD'), '\$1,234.56');
    });
    test('formats EUR with Western grouping and units', () {
      expect(CommonUtil.toCurrency(987654.0, currencyCode: 'EUR'), '€987,654');
      expect(CommonUtil.toCurrency(12345678.0, currencyCode: 'EUR'),
          '€12.35 million');
      expect(CommonUtil.toCurrency(1234567890.0, currencyCode: 'EUR'),
          '€1.23 billion');
      expect(
          CommonUtil.toCurrency(-54321.99, currencyCode: 'EUR'), '€-54,321.99');
    });
    test('formats GBP with Western grouping and units', () {
      expect(CommonUtil.toCurrency(7654321.0, currencyCode: 'GBP'),
          '£7.65 million');
      expect(CommonUtil.toCurrency(123.45, currencyCode: 'GBP'), '£123.45');
      expect(CommonUtil.toCurrency(-987654321.0, currencyCode: 'GBP'),
          '£-987.65 million');
    });
    test('formats JPY with Western grouping and units', () {
      expect(
          CommonUtil.toCurrency(1234567.0, currencyCode: 'JPY'), '¥1 million');
      expect(CommonUtil.toCurrency(98765.0, currencyCode: 'JPY'), '¥98,765');
      expect(CommonUtil.toCurrency(-1234567890.0, currencyCode: 'JPY'),
          '¥-1 billion');
    });
    test('formats CNY with Western grouping and units', () {
      expect(
          CommonUtil.toCurrency(8888888.0, currencyCode: 'CNY'), '¥9 million');
      expect(CommonUtil.toCurrency(100.5, currencyCode: 'CNY'), '¥101');
      expect(CommonUtil.toCurrency(-100000000.0, currencyCode: 'CNY'),
          '¥-100 million');
    });
    test('defaults to INR if no currencyCode', () {
      expect(CommonUtil.toCurrency(123456.0), '₹1.23 lakh');
    });
  });
}
