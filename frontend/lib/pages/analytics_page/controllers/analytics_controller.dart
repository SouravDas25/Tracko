import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/di/di.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/repositories/analytics_repository.dart';
import 'package:tracko/repositories/category_repository.dart';

class AnalyticsController extends ChangeNotifier {
  // Filter state
  AnalyticsGranularity _granularity = AnalyticsGranularity.monthly;
  DateRangePreset _datePreset = DateRangePreset.thisYear;
  late DateTime _startDate;
  late DateTime _endDate;
  StatsKind _kind = StatsKind.expense;
  Account? _selectedAccount;
  Category? _selectedCategory;
  GroupByMode _groupBy = GroupByMode.none;

  // Data state
  bool _loading = false;
  String? _error;
  double _total = 0.0;
  double _seriesMaxY = 0.0;
  List<NamedSeries> _groupedSeries = const [];
  DateTime? _periodStart;
  DateTime? _periodEndExclusive;

  // Dropdown data
  List<Account> _accounts = [];
  List<Category> _categories = [];

  // Getters – filter state
  AnalyticsGranularity get granularity => _granularity;
  DateRangePreset get datePreset => _datePreset;
  DateTime get startDate => _startDate;
  DateTime get endDate => _endDate;
  StatsKind get kind => _kind;
  Account? get selectedAccount => _selectedAccount;
  Category? get selectedCategory => _selectedCategory;
  GroupByMode get groupBy => _groupBy;

  // Getters – data state
  bool get loading => _loading;
  String? get error => _error;
  double get total => _total;
  double get seriesMaxY => _seriesMaxY;
  List<NamedSeries> get groupedSeries => _groupedSeries;
  DateTime? get periodStart => _periodStart;
  DateTime? get periodEndExclusive => _periodEndExclusive;

  // Getters – dropdown data
  List<Account> get accounts => _accounts;
  List<Category> get categories => _categories;

  AnalyticsController({
    AnalyticsGranularity? initialGranularity,
    DateRangePreset? initialPreset,
    StatsKind? initialKind,
    int? initialAccountId,
  }) {
    if (initialGranularity != null) _granularity = initialGranularity;
    if (initialPreset != null) _datePreset = initialPreset;
    if (initialKind != null) _kind = initialKind;

    final dates = _computeDateRange(_datePreset);
    _startDate = dates.$1;
    _endDate = dates.$2;

    _init(initialAccountId);
  }

  Future<void> _init(int? initialAccountId) async {
    await Future.wait([_loadAccounts(), _loadCategories()]);
    if (initialAccountId != null) {
      try {
        _selectedAccount =
            _accounts.firstWhere((a) => a.id == initialAccountId);
      } catch (_) {
        // Account not found
      }
    }
    load();
  }

  Future<void> _loadAccounts() async {
    try {
      final repo = sl<AccountRepository>();
      _accounts = await repo.getAllAccounts();
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading accounts for analytics: $e');
      _accounts = [];
    }
  }

  Future<void> _loadCategories() async {
    try {
      final repo = sl<CategoryRepository>();
      _categories = await repo.getAll();
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading categories for analytics: $e');
      _categories = [];
    }
  }

  // ---------------------------------------------------------------------------
  // Date range computation
  // ---------------------------------------------------------------------------

  (DateTime, DateTime) _computeDateRange(DateRangePreset preset,
      [DateTime? anchor]) {
    final now = anchor ?? DateTime.now();
    switch (preset) {
      case DateRangePreset.thisMonth:
        final start = DateTime(now.year, now.month, 1);
        final end = DateTime(now.year, now.month + 1, 0); // last day of month
        return (start, end);
      case DateRangePreset.last3Months:
        final start = DateTime(now.year, now.month - 2, 1);
        final end = DateTime(now.year, now.month + 1, 0);
        return (start, end);
      case DateRangePreset.last6Months:
        final start = DateTime(now.year, now.month - 5, 1);
        final end = DateTime(now.year, now.month + 1, 0);
        return (start, end);
      case DateRangePreset.thisYear:
        final start = DateTime(now.year, 1, 1);
        final end = DateTime(now.year, 12, 31);
        return (start, end);
      case DateRangePreset.last5Years:
        final start = DateTime(now.year - 4, 1, 1);
        final end = DateTime(now.year, 12, 31);
        return (start, end);
      case DateRangePreset.custom:
        return (_startDate, _endDate);
    }
  }

  // ---------------------------------------------------------------------------
  // Filter setters
  // ---------------------------------------------------------------------------

  void setGranularity(AnalyticsGranularity g) {
    _granularity = g;
    notifyListeners();
    load();
  }

  void setDatePreset(DateRangePreset preset) {
    _datePreset = preset;
    final dates = _computeDateRange(preset);
    _startDate = dates.$1;
    _endDate = dates.$2;
    notifyListeners();
    load();
  }

  void setCustomDateRange(DateTime start, DateTime end) {
    _datePreset = DateRangePreset.custom;
    _startDate = start;
    _endDate = end;
    notifyListeners();
    load();
  }

  void setKind(StatsKind kind) {
    _kind = kind;
    notifyListeners();
    load();
  }

  void setAccount(Account? account) {
    _selectedAccount = account;
    notifyListeners();
    load();
  }

  void setCategory(Category? category) {
    _selectedCategory = category;
    notifyListeners();
    load();
  }

  void setGroupBy(GroupByMode mode) {
    _groupBy = mode;
    notifyListeners();
    load();
  }

  void shiftDateRange(int delta) {
    if (_datePreset == DateRangePreset.custom) return;

    switch (_datePreset) {
      case DateRangePreset.thisMonth:
        _startDate = DateTime(_startDate.year, _startDate.month + delta, 1);
        _endDate = DateTime(_startDate.year, _startDate.month + 1, 0);
        break;
      case DateRangePreset.last3Months:
        _startDate =
            DateTime(_startDate.year, _startDate.month + (3 * delta), 1);
        _endDate = DateTime(_startDate.year, _startDate.month + 3, 0);
        break;
      case DateRangePreset.last6Months:
        _startDate =
            DateTime(_startDate.year, _startDate.month + (6 * delta), 1);
        _endDate = DateTime(_startDate.year, _startDate.month + 6, 0);
        break;
      case DateRangePreset.thisYear:
        _startDate = DateTime(_startDate.year + delta, 1, 1);
        _endDate = DateTime(_startDate.year, 12, 31);
        break;
      case DateRangePreset.last5Years:
        _startDate = DateTime(_startDate.year + (5 * delta), 1, 1);
        _endDate = DateTime(_startDate.year + 4, 12, 31);
        break;
      case DateRangePreset.custom:
        return;
    }
    notifyListeners();
    load();
  }

  void retry() {
    load();
  }

  // ---------------------------------------------------------------------------
  // Formatted date range for display
  // ---------------------------------------------------------------------------

  String get formattedDateRange {
    switch (_datePreset) {
      case DateRangePreset.thisMonth:
        return DateFormat('MMMM yyyy').format(_startDate);
      case DateRangePreset.last3Months:
      case DateRangePreset.last6Months:
      case DateRangePreset.custom:
        final startFmt = DateFormat('MMM d');
        final endFmt = DateFormat('MMM d, yyyy');
        if (_startDate.year != _endDate.year) {
          return '${DateFormat('MMM d, yyyy').format(_startDate)} - ${endFmt.format(_endDate)}';
        }
        return '${startFmt.format(_startDate)} - ${endFmt.format(_endDate)}';
      case DateRangePreset.thisYear:
        return DateFormat('yyyy').format(_startDate);
      case DateRangePreset.last5Years:
        return '${DateFormat('yyyy').format(_startDate)} – ${DateFormat('yyyy').format(_endDate)}';
    }
  }

  // ---------------------------------------------------------------------------
  // Data loading
  // ---------------------------------------------------------------------------

  String? _groupByApiValue() {
    switch (_groupBy) {
      case GroupByMode.none:
        return null;
      case GroupByMode.category:
        return 'category';
      case GroupByMode.account:
        return 'account';
      case GroupByMode.description:
        return 'name';
    }
  }

  String _fmtDate(DateTime d) {
    final mm = d.month.toString().padLeft(2, '0');
    final dd = d.day.toString().padLeft(2, '0');
    return '${d.year}-$mm-$dd';
  }

  Future<void> load() async {
    _loading = true;
    _error = null;
    notifyListeners();

    try {
      final kindType = _kind == StatsKind.expense
          ? TransactionType.DEBIT
          : TransactionType.CREDIT;

      final repo = sl<AnalyticsRepository>();
      final response = await repo.getChartData(
        transactionType: TransactionType.stringify(kindType),
        startDate: _fmtDate(_startDate),
        endDate: _fmtDate(_endDate),
        granularity: _granularity.name,
        groupBy: _groupByApiValue(),
        accountId: _selectedAccount?.id,
        categoryId: _selectedCategory?.id,
      );

      _groupedSeries = response.groupedSeries;
      _seriesMaxY = response.seriesMaxY;
      _total = response.total;
      _periodStart = response.periodStart;
      _periodEndExclusive = response.periodEnd;
      _loading = false;
      notifyListeners();
    } on DioException catch (e) {
      _loading = false;
      _groupedSeries = const [];
      _seriesMaxY = 0.0;
      _periodStart = null;
      _periodEndExclusive = null;
      if (e.type == DioExceptionType.connectionTimeout ||
          e.type == DioExceptionType.sendTimeout ||
          e.type == DioExceptionType.receiveTimeout) {
        _error = 'Connection timed out';
      } else {
        _error = e.message ?? e.toString();
      }
      notifyListeners();
    } catch (e) {
      _loading = false;
      _error = e.toString();
      _groupedSeries = const [];
      _seriesMaxY = 0.0;
      _periodStart = null;
      _periodEndExclusive = null;
      notifyListeners();
    }
  }
}
