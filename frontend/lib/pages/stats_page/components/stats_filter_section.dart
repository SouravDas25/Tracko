import 'package:flutter/material.dart';
import 'package:tracko/component/app_dropdown.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

class StatsFilterSection extends StatelessWidget {
  final StatsRange range;
  final StatsKind kind;
  final Account? selectedAccount;
  final List<Account> accounts;
  final Function(StatsRange) onRangeChanged;
  final Function(StatsKind) onKindChanged;
  final Function(Account?) onAccountChanged;

  const StatsFilterSection({
    Key? key,
    required this.range,
    required this.kind,
    required this.selectedAccount,
    required this.accounts,
    required this.onRangeChanged,
    required this.onKindChanged,
    required this.onAccountChanged,
  }) : super(key: key);

  String _rangeLabel(StatsRange r) {
    switch (r) {
      case StatsRange.weekly:
        return 'W';
      case StatsRange.monthly:
        return 'M';
      case StatsRange.yearly:
        return 'Y';
      case StatsRange.custom:
        return 'Custom';
    }
  }

  IconData _rangeIcon(StatsRange r) {
    switch (r) {
      case StatsRange.weekly:
        return Icons.view_week_outlined;
      case StatsRange.monthly:
        return Icons.calendar_month_outlined;
      case StatsRange.yearly:
        return Icons.date_range_outlined;
      case StatsRange.custom:
        return Icons.tune_outlined;
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final chipBg = isDark ? Colors.white10 : Colors.grey.shade100;
    final chipSelectedBg = Theme.of(context).primaryColor;

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 6),
      child: Column(
        children: [
          // Row 1: Range chips + Kind toggle
          Row(
            children: [
              // Range chips
              Container(
                height: 36,
                decoration: BoxDecoration(
                  color: chipBg,
                  borderRadius: BorderRadius.circular(18),
                ),
                padding: const EdgeInsets.all(3),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: StatsRange.values.map((r) {
                    final isSelected = range == r;
                    return GestureDetector(
                      onTap: () => onRangeChanged(r),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 180),
                        padding: EdgeInsets.symmetric(
                          horizontal: r == StatsRange.custom ? 12 : 10,
                        ),
                        height: 30,
                        decoration: BoxDecoration(
                          color: isSelected ? chipSelectedBg : Colors.transparent,
                          borderRadius: BorderRadius.circular(15),
                        ),
                        alignment: Alignment.center,
                        child: Text(
                          _rangeLabel(r),
                          style: TextStyle(
                            color: isSelected
                                ? Colors.white
                                : Theme.of(context).hintColor,
                            fontWeight: FontWeight.w600,
                            fontSize: 13,
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ),
              const Spacer(),
              // Kind toggle
              Container(
                height: 36,
                decoration: BoxDecoration(
                  color: chipBg,
                  borderRadius: BorderRadius.circular(18),
                ),
                padding: const EdgeInsets.all(3),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _buildKindChip(context, StatsKind.expense, "Expense",
                        Colors.redAccent),
                    _buildKindChip(
                        context, StatsKind.income, "Income", Colors.teal),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          // Row 2: Account selector
          AppBottomSheetPicker<Account>(
            value: selectedAccount,
            items: accounts,
            title: 'Select Account',
            labelBuilder: (a) => a.name,
            iconBuilder: (a) => Icons.account_balance_wallet_outlined,
            onSelected: onAccountChanged,
            allItemsLabel: 'All Accounts',
            isExpanded: true,
          ),
        ],
      ),
    );
  }

  Widget _buildKindChip(
      BuildContext context, StatsKind value, String label, Color color) {
    final isSelected = kind == value;
    return GestureDetector(
      onTap: () => onKindChanged(value),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(horizontal: 16),
        height: 30,
        decoration: BoxDecoration(
          color: isSelected ? color : Colors.transparent,
          borderRadius: BorderRadius.circular(15),
        ),
        alignment: Alignment.center,
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? Colors.white : Theme.of(context).hintColor,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}
