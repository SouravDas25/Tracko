import 'package:flutter/material.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

class AnalyticsFilterSection extends StatefulWidget {
  final AnalyticsGranularity granularity;
  final DateRangePreset datePreset;
  final StatsKind kind;
  final Account? selectedAccount;
  final List<Account> accounts;
  final Category? selectedCategory;
  final List<Category> categories;
  final GroupByMode groupBy;
  final ValueChanged<AnalyticsGranularity> onGranularityChanged;
  final ValueChanged<DateRangePreset> onDatePresetChanged;
  final ValueChanged<StatsKind> onKindChanged;
  final ValueChanged<Account?> onAccountChanged;
  final ValueChanged<Category?> onCategoryChanged;
  final ValueChanged<GroupByMode> onGroupByChanged;

  const AnalyticsFilterSection({
    Key? key,
    required this.granularity,
    required this.datePreset,
    required this.kind,
    required this.selectedAccount,
    required this.accounts,
    required this.selectedCategory,
    required this.categories,
    required this.groupBy,
    required this.onGranularityChanged,
    required this.onDatePresetChanged,
    required this.onKindChanged,
    required this.onAccountChanged,
    required this.onCategoryChanged,
    required this.onGroupByChanged,
  }) : super(key: key);

  @override
  State<AnalyticsFilterSection> createState() => _AnalyticsFilterSectionState();
}

class _AnalyticsFilterSectionState extends State<AnalyticsFilterSection>
    with SingleTickerProviderStateMixin {
  bool _expanded = false;

  bool get _hasActiveFilters =>
      widget.selectedAccount != null ||
      widget.selectedCategory != null ||
      widget.groupBy != GroupByMode.none;

  int get _activeFilterCount {
    int count = 0;
    if (widget.selectedAccount != null) count++;
    if (widget.selectedCategory != null) count++;
    if (widget.groupBy != GroupByMode.none) count++;
    return count;
  }

  String _granularityLabel(AnalyticsGranularity g) {
    switch (g) {
      case AnalyticsGranularity.weekly:
        return 'Weekly';
      case AnalyticsGranularity.monthly:
        return 'Monthly';
      case AnalyticsGranularity.yearly:
        return 'Yearly';
    }
  }

  String _presetLabel(DateRangePreset p) {
    switch (p) {
      case DateRangePreset.thisMonth:
        return 'This Month';
      case DateRangePreset.last3Months:
        return 'Last 3 Mo';
      case DateRangePreset.last6Months:
        return 'Last 6 Mo';
      case DateRangePreset.thisYear:
        return 'This Year';
      case DateRangePreset.last5Years:
        return 'Last 5 Yr';
      case DateRangePreset.custom:
        return 'Custom';
    }
  }

  String _groupByLabel(GroupByMode mode) {
    switch (mode) {
      case GroupByMode.none:
        return 'None';
      case GroupByMode.category:
        return 'Category';
      case GroupByMode.account:
        return 'Account';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Column(
        children: [
          // Row 1: Granularity + Kind toggle
          Row(
            children: [
              _buildGranularityChips(context),
              const Spacer(),
              _buildKindToggle(context),
            ],
          ),
          const SizedBox(height: 10),
          // Row 2: Date preset + Filters toggle button
          Row(
            children: [
              Expanded(
                child: _buildPillDropdown<DateRangePreset>(
                  context: context,
                  value: widget.datePreset,
                  items: DateRangePreset.values,
                  labelBuilder: _presetLabel,
                  onChanged: widget.onDatePresetChanged,
                  isExpanded: true,
                ),
              ),
              const SizedBox(width: 8),
              _buildFiltersToggle(context),
            ],
          ),
          // Collapsible filters
          AnimatedCrossFade(
            firstChild: const SizedBox.shrink(),
            secondChild: _buildExpandedFilters(context),
            crossFadeState: _expanded
                ? CrossFadeState.showSecond
                : CrossFadeState.showFirst,
            duration: const Duration(milliseconds: 250),
            sizeCurve: Curves.easeInOut,
          ),
        ],
      ),
    );
  }

  Widget _buildFiltersToggle(BuildContext context) {
    return GestureDetector(
      onTap: () => setState(() => _expanded = !_expanded),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        height: 40,
        padding: const EdgeInsets.symmetric(horizontal: 14),
        decoration: BoxDecoration(
          color: _hasActiveFilters
              ? Theme.of(context).primaryColor.withOpacity(0.15)
              : Theme.of(context).cardColor,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: _hasActiveFilters
                ? Theme.of(context).primaryColor.withOpacity(0.4)
                : Theme.of(context).dividerColor.withOpacity(0.1),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.tune_rounded,
              size: 18,
              color: _hasActiveFilters
                  ? Theme.of(context).primaryColor
                  : Theme.of(context).hintColor,
            ),
            const SizedBox(width: 6),
            Text(
              'Filters',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _hasActiveFilters
                    ? Theme.of(context).primaryColor
                    : Theme.of(context).hintColor,
              ),
            ),
            if (_hasActiveFilters) ...[
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 1),
                decoration: BoxDecoration(
                  color: Theme.of(context).primaryColor,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  '$_activeFilterCount',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
            const SizedBox(width: 4),
            AnimatedRotation(
              turns: _expanded ? 0.5 : 0,
              duration: const Duration(milliseconds: 250),
              child: Icon(
                Icons.keyboard_arrow_down,
                size: 18,
                color: _hasActiveFilters
                    ? Theme.of(context).primaryColor
                    : Theme.of(context).hintColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildExpandedFilters(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Column(
        children: [
          // Account + Category side by side
          Row(
            children: [
              Expanded(child: _buildAccountDropdown(context)),
              const SizedBox(width: 8),
              Expanded(child: _buildCategoryDropdown(context)),
            ],
          ),
          const SizedBox(height: 10),
          // Group-by chips
          _buildGroupByChips(context),
        ],
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Granularity chips
  // ---------------------------------------------------------------------------

  Widget _buildGranularityChips(BuildContext context) {
    return Container(
      height: 36,
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
            color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: AnalyticsGranularity.values.map((g) {
          final isSelected = widget.granularity == g;
          return GestureDetector(
            onTap: () => widget.onGranularityChanged(g),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 12),
              height: 36,
              decoration: BoxDecoration(
                color: isSelected
                    ? Theme.of(context).primaryColor
                    : Colors.transparent,
                borderRadius: BorderRadius.circular(18),
              ),
              alignment: Alignment.center,
              child: Text(
                _granularityLabel(g),
                style: TextStyle(
                  color: isSelected
                      ? Colors.white
                      : Theme.of(context).hintColor,
                  fontWeight: FontWeight.w600,
                  fontSize: 12,
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Expense / Income toggle
  // ---------------------------------------------------------------------------

  Widget _buildKindToggle(BuildContext context) {
    return Container(
      height: 36,
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
            color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildKindOption(context, StatsKind.expense, 'Expense',
              Colors.redAccent),
          _buildKindOption(context, StatsKind.income, 'Income', Colors.teal),
        ],
      ),
    );
  }

  Widget _buildKindOption(
      BuildContext context, StatsKind value, String label, Color color) {
    final isSelected = widget.kind == value;
    return GestureDetector(
      onTap: () => widget.onKindChanged(value),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 18),
        height: 36,
        decoration: BoxDecoration(
          color: isSelected ? color : Colors.transparent,
          borderRadius: BorderRadius.circular(18),
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

  // ---------------------------------------------------------------------------
  // Group-by chips
  // ---------------------------------------------------------------------------

  Widget _buildGroupByChips(BuildContext context) {
    return Row(
      children: [
        Text(
          'Group by',
          style: TextStyle(
            fontSize: 12,
            color: Theme.of(context).hintColor,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(width: 10),
        ...GroupByMode.values.map((mode) {
          final isSelected = widget.groupBy == mode;
          return Padding(
            padding: const EdgeInsets.only(right: 6),
            child: GestureDetector(
              onTap: () => widget.onGroupByChanged(mode),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding:
                    const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: isSelected
                      ? Theme.of(context).primaryColor
                      : Theme.of(context).cardColor,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: isSelected
                        ? Theme.of(context).primaryColor
                        : Theme.of(context).dividerColor.withOpacity(0.1),
                  ),
                ),
                child: Text(
                  _groupByLabel(mode),
                  style: TextStyle(
                    color: isSelected
                        ? Colors.white
                        : Theme.of(context).hintColor,
                    fontWeight: FontWeight.w600,
                    fontSize: 12,
                  ),
                ),
              ),
            ),
          );
        }),
      ],
    );
  }

  // ---------------------------------------------------------------------------
  // Generic pill-style dropdown
  // ---------------------------------------------------------------------------

  Widget _buildPillDropdown<T>({
    required BuildContext context,
    required T value,
    required List<T> items,
    required String Function(T) labelBuilder,
    required ValueChanged<T> onChanged,
    bool isExpanded = false,
  }) {
    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 0),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
            color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: isExpanded,
          icon: Padding(
            padding: const EdgeInsets.only(left: 4.0),
            child: Icon(Icons.keyboard_arrow_down,
                size: 18, color: Theme.of(context).primaryColor),
          ),
          isDense: true,
          dropdownColor: Theme.of(context).cardColor,
          style: TextStyle(
            color: Theme.of(context).textTheme.bodyLarge?.color,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
          items: items.map((item) {
            return DropdownMenuItem<T>(
              value: item,
              child: Text(labelBuilder(item)),
            );
          }).toList(),
          onChanged: (v) {
            if (v != null) onChanged(v);
          },
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Account dropdown
  // ---------------------------------------------------------------------------

  Widget _buildAccountDropdown(BuildContext context) {
    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
            color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<Account?>(
          value: widget.selectedAccount,
          isExpanded: true,
          icon: Icon(Icons.keyboard_arrow_down,
              size: 18, color: Theme.of(context).primaryColor),
          dropdownColor: Theme.of(context).cardColor,
          isDense: true,
          hint: Text(
            'All Accounts',
            style: TextStyle(
              color: Theme.of(context).textTheme.bodyLarge?.color,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
          style: TextStyle(
            color: Theme.of(context).textTheme.bodyLarge?.color,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
          items: [
            const DropdownMenuItem<Account?>(
              value: null,
              child: Text('All Accounts'),
            ),
            ...widget.accounts.map((a) {
              return DropdownMenuItem<Account?>(
                value: a,
                child: Text(a.name),
              );
            }),
          ],
          onChanged: widget.onAccountChanged,
        ),
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Category dropdown
  // ---------------------------------------------------------------------------

  Widget _buildCategoryDropdown(BuildContext context) {
    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
            color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<Category?>(
          value: widget.selectedCategory,
          isExpanded: true,
          icon: Icon(Icons.keyboard_arrow_down,
              size: 18, color: Theme.of(context).primaryColor),
          dropdownColor: Theme.of(context).cardColor,
          isDense: true,
          hint: Text(
            'All Categories',
            style: TextStyle(
              color: Theme.of(context).textTheme.bodyLarge?.color,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
          style: TextStyle(
            color: Theme.of(context).textTheme.bodyLarge?.color,
            fontWeight: FontWeight.w600,
            fontSize: 13,
          ),
          items: [
            const DropdownMenuItem<Category?>(
              value: null,
              child: Text('All Categories'),
            ),
            ...widget.categories.map((c) {
              return DropdownMenuItem<Category?>(
                value: c,
                child: Text(c.name),
              );
            }),
          ],
          onChanged: widget.onCategoryChanged,
        ),
      ),
    );
  }
}
