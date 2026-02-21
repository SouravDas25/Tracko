import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/scratch/ChartUtil.dart';
import 'package:tracko/services/api_client.dart';

enum StatsRange { weekly, monthly, yearly, custom }

enum StatsKind { expense, income }

class CategoryStat {
  final int categoryId;
  final String categoryName;
  final double amount;

  const CategoryStat({
    required this.categoryId,
    required this.categoryName,
    required this.amount,
  });
}

class SeriesPoint {
  final double x;
  final double y;
  final String label;

  const SeriesPoint({
    required this.x,
    required this.y,
    required this.label,
  });
}

class StatsController extends ChangeNotifier {
  StatsRange _range = StatsRange.monthly;
  StatsKind _kind = StatsKind.expense;
  DateTime _anchorDate = DateTime.now();
  DateTime? _customStartDate;
  DateTime? _customEndDate;
  Account? _selectedAccount;
  List<Account> _accounts = [];

  bool _loading = false;
  String? _error;

  double _total = 0.0;
  List<CategoryStat> _stats = const [];
  List<SeriesPoint> _series = const [];
  double _seriesMaxY = 0.0;
  List<ChartEntry> _pieSeries = const [];

  DateTime? _periodStart;
  DateTime? _periodEndExclusive;

  // Getters
  StatsRange get range => _range;
  StatsKind get kind => _kind;
  DateTime get anchorDate => _anchorDate;
  DateTime? get customStartDate => _customStartDate;
  DateTime? get customEndDate => _customEndDate;
  Account? get selectedAccount => _selectedAccount;
  List<Account> get accounts => _accounts;

  bool get loading => _loading;
  String? get error => _error;
  double get total => _total;
  List<CategoryStat> get stats => _stats;
  List<SeriesPoint> get series => _series;
  double get seriesMaxY => _seriesMaxY;
  List<ChartEntry> get pieSeries => _pieSeries;
  DateTime? get periodStart => _periodStart;
  DateTime? get periodEndExclusive => _periodEndExclusive;

  StatsController({
    DateTime? initialDate,
    StatsKind? initialKind,
    int? initialAccountId,
  }) {
    if (initialDate != null) {
      _anchorDate = initialDate;
      // If initial date is provided, maybe default to weekly or monthly? Monthly is default.
    }
    if (initialKind != null) {
      _kind = initialKind;
    }
    _init(initialAccountId);
  }

  Future<void> _init(int? initialAccountId) async {
    await _loadAccounts();
    if (initialAccountId != null) {
      try {
        _selectedAccount =
            _accounts.firstWhere((a) => a.id == initialAccountId);
      } catch (_) {
        // Account not found or ID was valid but not in list
      }
    }
    load();
  }

  Future<void> _loadAccounts() async {
    try {
      final repo = AccountRepository();
      _accounts = await repo.getAllAccounts();
      notifyListeners();
    } catch (e) {
      debugPrint('Error loading accounts for stats: $e');
    }
  }

  void setRange(StatsRange range) {
    _range = range;
    _anchorDate = DateTime.now();
    load();
  }

  void setKind(StatsKind kind) {
    _kind = kind;
    load();
  }

  void setAccount(Account? account) {
    _selectedAccount = account;
    load();
  }

  void setCustomRange(DateTime start, DateTime end) {
    _range = StatsRange.custom;
    _customStartDate = start;
    _customEndDate = end;
    load();
  }

  void shiftAnchor(int delta) {
    if (_range == StatsRange.custom) return; // Cannot shift custom range
    _anchorDate = _shiftAnchorDate(_anchorDate, delta);
    load();
  }

  DateTime _shiftAnchorDate(DateTime anchor, int delta) {
    switch (_range) {
      case StatsRange.weekly:
        return anchor.add(Duration(days: 7 * delta));
      case StatsRange.monthly:
        return DateTime(anchor.year, anchor.month + delta, 15);
      case StatsRange.yearly:
        return DateTime(anchor.year + delta, anchor.month, 15);
      case StatsRange.custom:
        return anchor;
    }
  }

  String get formattedDateRange {
    final start = _periodStart;
    final end = _periodEndExclusive;
    if (start == null || end == null) return '';

    final endInclusive = end.subtract(const Duration(days: 1));

    switch (_range) {
      case StatsRange.weekly:
      case StatsRange.custom:
        // "Oct 1 - Oct 7, 2023"
        final startFormat = DateFormat('MMM d');
        final endFormat = DateFormat('MMM d, yyyy');
        if (start.year != endInclusive.year) {
          return '${DateFormat('MMM d, yyyy').format(start)} - ${endFormat.format(endInclusive)}';
        }
        return '${startFormat.format(start)} - ${endFormat.format(endInclusive)}';
      case StatsRange.monthly:
        // "October 2023"
        return DateFormat('MMMM yyyy').format(start);
      case StatsRange.yearly:
        // "2023"
        return DateFormat('yyyy').format(start);
    }
  }

  String get rangeLabel {
    switch (_range) {
      case StatsRange.weekly:
        return 'Weekly';
      case StatsRange.monthly:
        return 'Monthly';
      case StatsRange.yearly:
        return 'Yearly';
      case StatsRange.custom:
        return 'Custom';
    }
  }

  String get kindLabel {
    switch (_kind) {
      case StatsKind.expense:
        return 'Expense';
      case StatsKind.income:
        return 'Income';
    }
  }

  Color get kindColor {
    return _kind == StatsKind.expense ? Colors.red : Colors.green;
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
      final api = ApiClient();
      final res = await api.get<Map<String, dynamic>>(
        '${ApiConfig.stats}/summary',
        query: {
          'range': _range.name,
          'transactionType': kindType,
          'date': _fmtDate(_anchorDate),
          if (_selectedAccount != null && _selectedAccount!.id != null)
            'accountId': _selectedAccount!.id,
          if (_range == StatsRange.custom && _customStartDate != null)
            'startDate': _fmtDate(_customStartDate!),
          if (_range == StatsRange.custom && _customEndDate != null)
            'endDate': _fmtDate(_customEndDate!),
        },
      );

      final total = ((res['total'] as num?) ?? 0).toDouble();
      final periodStartStr = (res['periodStart'] as String?) ?? '';
      final periodEndStr = (res['periodEnd'] as String?) ?? '';

      DateTime? pStart;
      DateTime? pEndExcl;
      try {
        if (periodStartStr.isNotEmpty) {
          pStart = DateTime.parse(periodStartStr);
        }
        if (periodEndStr.isNotEmpty) {
          final endInclusive = DateTime.parse(periodEndStr);
          pEndExcl = endInclusive.add(const Duration(days: 1));
        }
      } catch (_) {}

      final seriesJson = (res['series'] as List?) ?? const [];
      final series = <SeriesPoint>[];
      for (int i = 0; i < seriesJson.length; i++) {
        final item = seriesJson[i];
        if (item is! Map) continue;
        final label = (item['label'] as String?) ?? '';
        final value = ((item['value'] as num?) ?? 0).toDouble();
        series.add(SeriesPoint(x: i.toDouble(), y: value, label: label));
      }

      // Backend now returns contiguous, zero-filled buckets for the requested period.
      final maxY = series.isEmpty
          ? 0.0
          : series.map((p) => p.y).reduce((a, b) => a > b ? a : b);

      final catsJson = (res['categories'] as List?) ?? const [];
      final stats = <CategoryStat>[];
      for (final item in catsJson) {
        if (item is! Map) continue;
        final cid = (item['categoryId'] as num?)?.toInt() ?? 0;
        if (cid == 0) continue;
        final name = (item['categoryName'] as String?) ?? 'Category $cid';
        final amount = ((item['amount'] as num?) ?? 0).toDouble();
        stats.add(
            CategoryStat(categoryId: cid, categoryName: name, amount: amount));
      }

      // Pie Chart Logic with "Others"
      final rawPieSeries = <ChartEntry>[];
      for (final s in stats) {
        final v = s.amount.round();
        if (v <= 0) continue;
        rawPieSeries.add(ChartEntry(s.categoryId, s.categoryName, v));
      }
      rawPieSeries.sort((a, b) => b.value.compareTo(a.value));

      List<ChartEntry> finalPieSeries;
      if (rawPieSeries.length > 5) {
        final top5 = rawPieSeries.sublist(0, 5);
        final others = rawPieSeries.sublist(5);
        final othersTotal = others.fold(0, (sum, item) => sum + item.value);

        finalPieSeries = List.from(top5);
        if (othersTotal > 0) {
          // Use id -1 or similar for 'Others'
          finalPieSeries.add(ChartEntry(-1, 'Others', othersTotal));
        }
      } else {
        finalPieSeries = rawPieSeries;
      }

      _stats = stats;
      _total = total;
      _series = series;
      _seriesMaxY = maxY;
      _pieSeries = finalPieSeries;
      _periodStart = pStart;
      _periodEndExclusive = pEndExcl;
      _loading = false;
      notifyListeners();
    } catch (e) {
      _loading = false;
      _error = e.toString();
      _series = const [];
      _seriesMaxY = 0.0;
      _periodStart = null;
      _periodEndExclusive = null;
      _pieSeries = const [];
      notifyListeners();
    }
  }
}
