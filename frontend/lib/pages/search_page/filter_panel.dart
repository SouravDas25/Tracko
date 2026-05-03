import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:tracko/di/di.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/search_filters.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/repositories/category_repository.dart';

/// A collapsible panel for search filters.
///
/// Provides filtering options for:
/// - Date range (start and end dates)
/// - Amount range (min and max amounts)
/// - Account multi-select
/// - Category single-select
///
/// Displays an active filter count badge and includes a clear filters button.
/// Calls [onFiltersChanged] when any filter value changes.
///
/// Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.7, 5.8
class FilterPanel extends StatefulWidget {
  /// The current filter values.
  final SearchFilters filters;

  /// Called when any filter value changes.
  final ValueChanged<SearchFilters> onFiltersChanged;

  /// Called when the clear filters button is tapped.
  final VoidCallback onClearFilters;

  const FilterPanel({
    Key? key,
    required this.filters,
    required this.onFiltersChanged,
    required this.onClearFilters,
  }) : super(key: key);

  @override
  State<FilterPanel> createState() => _FilterPanelState();
}

class _FilterPanelState extends State<FilterPanel> {
  // Expansion state
  bool _isExpanded = false;

  // Data loaded from repositories
  List<Account> _accounts = [];
  List<Category> _categories = [];
  bool _isLoadingAccounts = false;
  bool _isLoadingCategories = false;

  // Text controllers for amount inputs
  late TextEditingController _minAmountController;
  late TextEditingController _maxAmountController;

  // Selected account IDs for multi-select
  List<int> _selectedAccountIds = [];

  @override
  void initState() {
    super.initState();
    _minAmountController = TextEditingController(
      text: widget.filters.minAmount?.toString() ?? '',
    );
    _maxAmountController = TextEditingController(
      text: widget.filters.maxAmount?.toString() ?? '',
    );
    _selectedAccountIds = List.from(widget.filters.accountIds ?? []);

    // Load accounts and categories
    _loadAccounts();
    _loadCategories();
  }

  @override
  void didUpdateWidget(FilterPanel oldWidget) {
    super.didUpdateWidget(oldWidget);

    // Update text controllers if filters changed externally
    if (oldWidget.filters.minAmount != widget.filters.minAmount) {
      _minAmountController.text = widget.filters.minAmount?.toString() ?? '';
    }
    if (oldWidget.filters.maxAmount != widget.filters.maxAmount) {
      _maxAmountController.text = widget.filters.maxAmount?.toString() ?? '';
    }
    if (oldWidget.filters.accountIds != widget.filters.accountIds) {
      _selectedAccountIds = List.from(widget.filters.accountIds ?? []);
    }
  }

  @override
  void dispose() {
    _minAmountController.dispose();
    _maxAmountController.dispose();
    super.dispose();
  }

  // ============================================================
  // Data Loading
  // ============================================================

  Future<void> _loadAccounts() async {
    setState(() => _isLoadingAccounts = true);

    try {
      final repository = sl<AccountRepository>();
      final accounts = await repository.getAllAccounts();
      if (mounted) {
        setState(() {
          _accounts = accounts;
          _isLoadingAccounts = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingAccounts = false);
      }
    }
  }

  Future<void> _loadCategories() async {
    setState(() => _isLoadingCategories = true);

    try {
      final repository = sl<CategoryRepository>();
      final categories = await repository.getAll();
      if (mounted) {
        setState(() {
          _categories = categories;
          _isLoadingCategories = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingCategories = false);
      }
    }
  }

  // ============================================================
  // Filter Update Methods
  // ============================================================

  void _updateStartDate(DateTime? date) {
    widget.onFiltersChanged(
      widget.filters.copyWith(
        startDate: date,
        clearStartDate: date == null,
      ),
    );
  }

  void _updateEndDate(DateTime? date) {
    widget.onFiltersChanged(
      widget.filters.copyWith(
        endDate: date,
        clearEndDate: date == null,
      ),
    );
  }

  void _updateMinAmount(String value) {
    final amount = double.tryParse(value);
    widget.onFiltersChanged(
      widget.filters.copyWith(
        minAmount: amount,
        clearMinAmount: amount == null,
      ),
    );
  }

  void _updateMaxAmount(String value) {
    final amount = double.tryParse(value);
    widget.onFiltersChanged(
      widget.filters.copyWith(
        maxAmount: amount,
        clearMaxAmount: amount == null,
      ),
    );
  }

  void _updateAccountIds(List<int> accountIds) {
    widget.onFiltersChanged(
      widget.filters.copyWith(
        accountIds: accountIds.isEmpty ? null : accountIds,
        clearAccountIds: accountIds.isEmpty,
      ),
    );
  }

  void _updateCategoryId(int? categoryId) {
    widget.onFiltersChanged(
      widget.filters.copyWith(
        categoryId: categoryId,
        clearCategoryId: categoryId == null,
      ),
    );
  }

  // ============================================================
  // Date Picker Helpers
  // ============================================================

  Future<void> _selectStartDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: widget.filters.startDate ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );

    if (picked != null) {
      _updateStartDate(picked);
    }
  }

  Future<void> _selectEndDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: widget.filters.endDate ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );

    if (picked != null) {
      _updateEndDate(picked);
    }
  }

  // ============================================================
  // Account Multi-Select Dialog
  // ============================================================

  Future<void> _showAccountSelector(BuildContext context) async {
    await showDialog(
      context: context,
      builder: (context) => _AccountMultiSelectDialog(
        accounts: _accounts,
        selectedIds: _selectedAccountIds,
        onSelectionChanged: (ids) {
          _selectedAccountIds = ids;
          _updateAccountIds(ids);
        },
      ),
    );
  }

  // ============================================================
  // Build Methods
  // ============================================================

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeInOut,
      child: Column(
        children: [
          // Filter Header
          _buildFilterHeader(context),

          // Filter Content (when expanded)
          AnimatedCrossFade(
            duration: const Duration(milliseconds: 200),
            crossFadeState: _isExpanded
                ? CrossFadeState.showSecond
                : CrossFadeState.showFirst,
            firstChild: const SizedBox.shrink(),
            secondChild: _buildFilterContent(context),
          ),
        ],
      ),
    );
  }

  /// Builds the filter header with title, badge, and expand icon.
  Widget _buildFilterHeader(BuildContext context) {
    return InkWell(
      onTap: () => setState(() => _isExpanded = !_isExpanded),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 12.0),
        child: Row(
          children: [
            // Filters label
            Text(
              'Filters',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).textTheme.bodyLarge?.color,
              ),
            ),
            const SizedBox(width: 8),

            // Active filter count badge
            if (widget.filters.activeCount > 0)
              _buildActiveFilterBadge(context),

            const Spacer(),

            // Expand/Collapse icon
            Icon(
              _isExpanded ? Icons.expand_less : Icons.expand_more,
              color: Theme.of(context).hintColor,
            ),
          ],
        ),
      ),
    );
  }

  /// Builds the active filter count badge.
  Widget _buildActiveFilterBadge(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColor,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        '${widget.filters.activeCount}',
        style: const TextStyle(
          color: Colors.white,
          fontSize: 12,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  /// Builds the filter content when expanded.
  Widget _buildFilterContent(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16.0, 8.0, 16.0, 0.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Date Range Row
          _buildDateRangeRow(context),
          const SizedBox(height: 16),

          // Amount Range Row
          _buildAmountRangeRow(context),
          const SizedBox(height: 16),

          // Account Selector
          _buildAccountSelector(context),
          const SizedBox(height: 16),

          // Category Selector
          _buildCategorySelector(context),
          const SizedBox(height: 16),

          // Clear Filters Button
          _buildClearFiltersButton(context),
          const SizedBox(height: 8),
        ],
      ),
    );
  }

  /// Builds the date range filter row.
  Widget _buildDateRangeRow(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _buildDatePickerField(
            context,
            label: 'Start Date',
            date: widget.filters.startDate,
            onTap: () => _selectStartDate(context),
            onClear: () => _updateStartDate(null),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildDatePickerField(
            context,
            label: 'End Date',
            date: widget.filters.endDate,
            onTap: () => _selectEndDate(context),
            onClear: () => _updateEndDate(null),
          ),
        ),
      ],
    );
  }

  /// Builds a single date picker field.
  Widget _buildDatePickerField(
    BuildContext context, {
    required String label,
    required DateTime? date,
    required VoidCallback onTap,
    required VoidCallback onClear,
  }) {
    final hasValue = date != null;
    final displayText = hasValue
        ? '${date.day}/${date.month}/${date.year}'
        : 'Select';

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: label,
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          contentPadding: const EdgeInsets.fromLTRB(12, 20, 12, 12),
          suffixIcon: hasValue
              ? InkWell(
                  onTap: onClear,
                  child: const Icon(Icons.clear, size: 18),
                )
              : const Icon(Icons.calendar_today, size: 18),
        ),
        child: Text(
          displayText,
          style: TextStyle(
            fontSize: 14,
            color: hasValue
                ? Theme.of(context).textTheme.bodyLarge?.color
                : Theme.of(context).hintColor,
          ),
        ),
      ),
    );
  }

  /// Builds the amount range filter row.
  Widget _buildAmountRangeRow(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _buildAmountField(
            context,
            label: 'Min Amount',
            controller: _minAmountController,
            onChanged: _updateMinAmount,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildAmountField(
            context,
            label: 'Max Amount',
            controller: _maxAmountController,
            onChanged: _updateMaxAmount,
          ),
        ),
      ],
    );
  }

  /// Builds a single amount input field.
  Widget _buildAmountField(
    BuildContext context, {
    required String label,
    required TextEditingController controller,
    required ValueChanged<String> onChanged,
  }) {
    return TextField(
      controller: controller,
      decoration: InputDecoration(
        labelText: label,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 12,
        ),
      ),
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d*')),
      ],
      onChanged: (value) {
        // Debounce amount changes
        Future.delayed(const Duration(milliseconds: 300), () {
          if (controller.text == value) {
            onChanged(value);
          }
        });
      },
    );
  }

  /// Builds the account multi-select filter.
  Widget _buildAccountSelector(BuildContext context) {
    return InkWell(
      onTap: _isLoadingAccounts ? null : () => _showAccountSelector(context),
      borderRadius: BorderRadius.circular(8),
      child: InputDecorator(
        decoration: InputDecoration(
          labelText: 'Accounts',
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          contentPadding: const EdgeInsets.symmetric(
            horizontal: 12,
            vertical: 12,
          ),
          suffixIcon: _isLoadingAccounts
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.arrow_drop_down),
        ),
        child: Text(
          _getAccountSelectionText(),
          style: TextStyle(
            fontSize: 14,
            color: _selectedAccountIds.isNotEmpty
                ? Theme.of(context).textTheme.bodyLarge?.color
                : Theme.of(context).hintColor,
          ),
        ),
      ),
    );
  }

  /// Gets the display text for account selection.
  String _getAccountSelectionText() {
    if (_selectedAccountIds.isEmpty) {
      return 'All accounts';
    }
    if (_selectedAccountIds.length == 1) {
      final account = _accounts.firstWhere(
        (a) => a.id == _selectedAccountIds.first,
        orElse: () => Account(),
      );
      return account.name.isNotEmpty ? account.name : '1 account selected';
    }
    return '${_selectedAccountIds.length} accounts selected';
  }

  /// Builds the category single-select dropdown.
  Widget _buildCategorySelector(BuildContext context) {
    return DropdownButtonFormField<int?>(
      value: widget.filters.categoryId,
      decoration: InputDecoration(
        labelText: 'Category',
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 12,
        ),
      ),
      hint: const Text('All categories'),
      items: [
        const DropdownMenuItem<int?>(
          value: null,
          child: Text('All categories'),
        ),
        ..._categories.map((category) => DropdownMenuItem<int?>(
              value: category.id,
              child: Text(category.name),
            )),
      ],
      onChanged: _isLoadingCategories
          ? null
          : (value) => _updateCategoryId(value),
    );
  }

  /// Builds the clear filters button.
  Widget _buildClearFiltersButton(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      child: OutlinedButton.icon(
        onPressed: widget.filters.hasActiveFilters
            ? widget.onClearFilters
            : null,
        icon: const Icon(Icons.clear_all, size: 18),
        label: const Text('Clear Filters'),
        style: OutlinedButton.styleFrom(
          padding: const EdgeInsets.symmetric(vertical: 12),
        ),
      ),
    );
  }
}

/// A dialog for multi-selecting accounts.
class _AccountMultiSelectDialog extends StatefulWidget {
  final List<Account> accounts;
  final List<int> selectedIds;
  final ValueChanged<List<int>> onSelectionChanged;

  const _AccountMultiSelectDialog({
    required this.accounts,
    required this.selectedIds,
    required this.onSelectionChanged,
  });

  @override
  State<_AccountMultiSelectDialog> createState() =>
      _AccountMultiSelectDialogState();
}

class _AccountMultiSelectDialogState extends State<_AccountMultiSelectDialog> {
  late List<int> _tempSelectedIds;

  @override
  void initState() {
    super.initState();
    _tempSelectedIds = List.from(widget.selectedIds);
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Select Accounts'),
      content: SizedBox(
        width: double.maxFinite,
        child: ListView.builder(
          shrinkWrap: true,
          itemCount: widget.accounts.length,
          itemBuilder: (context, index) {
            final account = widget.accounts[index];
            final isSelected = _tempSelectedIds.contains(account.id);

            return CheckboxListTile(
              title: Text(account.name),
              subtitle: Text(account.currency),
              value: isSelected,
              onChanged: (checked) {
                setState(() {
                  if (checked == true) {
                    _tempSelectedIds.add(account.id!);
                  } else {
                    _tempSelectedIds.remove(account.id);
                  }
                });
              },
            );
          },
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancel'),
        ),
        TextButton(
          onPressed: () {
            widget.onSelectionChanged(_tempSelectedIds);
            Navigator.of(context).pop();
          },
          child: const Text('Apply'),
        ),
      ],
    );
  }
}
