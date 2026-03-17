import 'package:get_it/get_it.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/services/auth_service.dart';
import 'package:tracko/services/budget_service.dart';
import 'package:tracko/services/exchange_rate_service.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/repositories/analytics_repository.dart';
import 'package:tracko/repositories/category_repository.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/repositories/json_store_repository.dart';
import 'package:tracko/repositories/recurring_transaction_repository.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/repositories/user_currency_repository.dart';
import 'package:tracko/repositories/user_repository.dart';

final sl = GetIt.instance;

Future<void> setupDI() async {
  if (sl.isRegistered<ApiClient>()) {
    return;
  }

  sl.registerLazySingleton<ApiClient>(() => ApiClient());
  sl.registerLazySingleton<AuthService>(
      () => AuthService(api: sl<ApiClient>()));

  sl.registerLazySingleton<BudgetService>(
      () => BudgetService(apiClient: sl<ApiClient>()));
  sl.registerLazySingleton<ExchangeRateService>(
      () => ExchangeRateService(api: sl<ApiClient>()));

  sl.registerLazySingleton<UserRepository>(
      () => UserRepository(api: sl<ApiClient>()));

  sl.registerLazySingleton<SessionService>(() =>
      SessionService(auth: sl<AuthService>(), userRepo: sl<UserRepository>()));

  sl.registerLazySingleton<AccountRepository>(
      () => AccountRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<AnalyticsRepository>(
      () => AnalyticsRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<CategoryRepository>(
      () => CategoryRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<ContactRepository>(
      () => ContactRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<TransactionRepository>(
      () => TransactionRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<SplitRepository>(
      () => SplitRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<RecurringTransactionRepository>(
      () => RecurringTransactionRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<UserCurrencyRepository>(
      () => UserCurrencyRepository(api: sl<ApiClient>()));
  sl.registerLazySingleton<JsonStoreRepository>(
      () => JsonStoreRepository(api: sl<ApiClient>()));
}
