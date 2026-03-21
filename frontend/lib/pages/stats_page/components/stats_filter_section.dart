import 'package:flutter/material.dart';
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
          GestureDetector(
            onTap: () => _showAccountPicker(context),
            child: Container(
              height: 40,
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                    color: Theme.of(context).dividerColor.withOpacity(0.15)),
              ),
              child: Row(
                children: [
                  Icon(Icons.account_balance_wallet_outlined,
                      size: 18, color: Theme.of(context).hintColor),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      selectedAccount?.name ?? 'All Accounts',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: Theme.of(context).textTheme.bodyLarge?.color,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  if (selectedAccount != null)
                    GestureDetector(
                      onTap: () => onAccountChanged(null),
                      child: Padding(
                        padding: const EdgeInsets.only(left: 4),
                        child: Icon(Icons.close, size: 16,
                            color: Theme.of(context).hintColor),
                      ),
                    ),
                  const SizedBox(width: 4),
                  Icon(Icons.keyboard_arrow_down,
                      size: 18, color: Theme.of(context).hintColor),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showAccountPicker(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final unselectedIconColor = isDark ? Colors.white54 : Colors.black45;

    showModalBottomSheet(
      context: context,
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      isScrollControlled: true,
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.5,
      ),
      builder: (context) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Select Account',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Theme.of(context).textTheme.bodyLarge?.color,
                      ),
                    ),
                    if (selectedAccount != null)
                      TextButton(
                        onPressed: () {
                          onAccountChanged(null);
                          Navigator.pop(context);
                        },
                        child: const Text('Clear'),
                      ),
                  ],
                ),
              ),
              const Divider(height: 1),
              Flexible(
                child: ListView(
                  shrinkWrap: true,
                  children: [
                    ListTile(
                      iconColor: selectedAccount == null
                          ? Theme.of(context).primaryColor
                          : unselectedIconColor,
                      leading: Icon(Icons.select_all),
                      title: Text('All Accounts',
                          style: TextStyle(
                            color: Theme.of(context).textTheme.bodyLarge?.color,
                            fontWeight: selectedAccount == null
                                ? FontWeight.w700
                                : FontWeight.normal,
                          )),
                      trailing: selectedAccount == null
                          ? Icon(Icons.check,
                              color: Colors.blue)
                          : null,
                      onTap: () {
                        onAccountChanged(null);
                        Navigator.pop(context);
                      },
                    ),
                    ...accounts.map((a) {
                      final isSelected = selectedAccount?.id == a.id;
                      return ListTile(
                        iconColor: isSelected
                            ? Theme.of(context).primaryColor
                            : unselectedIconColor,
                        leading: const Icon(
                          Icons.account_balance_wallet_outlined,
                        ),
                        title: Text(a.name ?? 'Unknown',
                            style: TextStyle(
                              color: Theme.of(context).textTheme.bodyLarge?.color,
                              fontWeight: isSelected
                                  ? FontWeight.w700
                                  : FontWeight.normal,
                            )),
                        trailing: isSelected
                            ? Icon(Icons.check,
                                color: Colors.blue)
                            : null,
                        onTap: () {
                          onAccountChanged(a);
                          Navigator.pop(context);
                        },
                      );
                    }),
                  ],
                ),
              ),
              const SizedBox(height: 8),
            ],
          ),
        );
      },
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
