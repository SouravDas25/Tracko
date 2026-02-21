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
        return 'Weekly';
      case StatsRange.monthly:
        return 'Monthly';
      case StatsRange.yearly:
        return 'Yearly';
      case StatsRange.custom:
        return 'Custom';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
      child: Column(
        children: [
          Row(
            children: [
              // Range Dropdown Pill
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                decoration: BoxDecoration(
                  color: Theme.of(context).cardColor,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(
                      color: Theme.of(context).dividerColor.withOpacity(0.1)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.02),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    )
                  ],
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<StatsRange>(
                    value: range,
                    icon: Padding(
                      padding: const EdgeInsets.only(left: 8.0),
                      child: Icon(Icons.keyboard_arrow_down,
                          size: 20, color: Theme.of(context).primaryColor),
                    ),
                    isDense: true,
                    dropdownColor: Theme.of(context).cardColor,
                    style: TextStyle(
                      color: Theme.of(context).textTheme.bodyLarge?.color,
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                    items: StatsRange.values.map((r) {
                      return DropdownMenuItem(
                        value: r,
                        child: Text(_rangeLabel(r)),
                      );
                    }).toList(),
                    onChanged: (v) {
                      if (v != null) onRangeChanged(v);
                    },
                  ),
                ),
              ),
              const Spacer(),
              // Kind Toggle Pill
              Container(
                height: 40,
                decoration: BoxDecoration(
                  color: Theme.of(context).cardColor,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                      color: Theme.of(context).dividerColor.withOpacity(0.1)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.02),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    )
                  ],
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _buildKindOption(context, StatsKind.expense, "Expense",
                        Colors.redAccent),
                    _buildKindOption(
                        context, StatsKind.income, "Income", Colors.teal),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // Account Filter Dropdown (Modern Filled Style)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
            decoration: BoxDecoration(
              color: Theme.of(context).cardColor,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                  color: Theme.of(context).dividerColor.withOpacity(0.1)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.02),
                  blurRadius: 8,
                  offset: const Offset(0, 2),
                )
              ],
            ),
            child: DropdownButtonHideUnderline(
              child: DropdownButton<Account?>(
                value: selectedAccount,
                isExpanded: true,
                icon: Icon(Icons.keyboard_arrow_down,
                    size: 20, color: Theme.of(context).primaryColor),
                dropdownColor: Theme.of(context).cardColor,
                hint: Text(
                  'All Accounts',
                  style: TextStyle(
                    color: Theme.of(context).textTheme.bodyLarge?.color,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: TextStyle(
                  color: Theme.of(context).textTheme.bodyLarge?.color,
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
                items: [
                  const DropdownMenuItem<Account?>(
                    value: null,
                    child: Text('All Accounts'),
                  ),
                  ...accounts.map((a) {
                    return DropdownMenuItem<Account?>(
                      value: a,
                      child: Text(a.name ?? 'Unknown Account'),
                    );
                  }).toList(),
                ],
                onChanged: onAccountChanged,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildKindOption(
      BuildContext context, StatsKind value, String label, Color color) {
    bool isSelected = kind == value;
    return GestureDetector(
      onTap: () => onKindChanged(value),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 20),
        height: 40,
        decoration: BoxDecoration(
          color: isSelected ? color : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
        ),
        alignment: Alignment.center,
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? Colors.white : Theme.of(context).hintColor,
            fontWeight: FontWeight.w600,
            fontSize: 14,
          ),
        ),
      ),
    );
  }
}
