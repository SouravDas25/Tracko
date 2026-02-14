import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/month_picker_dialog.dart';
import 'package:tracko/models/budget_response.dart';
import 'package:tracko/pages/budget_page/widgets/allocation_dialog.dart';
import 'package:tracko/pages/budget_page/widgets/budget_category_tile.dart';
import 'package:tracko/services/budget_service.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class BudgetPage extends StatefulWidget {
  @override
  _BudgetPageState createState() => _BudgetPageState();
}

class _BudgetPageState extends State<BudgetPage> {
  DateTime _selectedDate = DateTime.now();
  final BudgetService _budgetService = BudgetService();
  BudgetResponse? _budgetData;
  bool _isLoading = true;
  final RefreshController _refreshController = RefreshController();
  final DateTime _minDate = DateTime(2020, 1);
  final DateTime _maxDate = DateTime(2030, 12);

  @override
  void initState() {
    super.initState();
    _loadBudgetData();
  }

  @override
  void dispose() {
    _refreshController.dispose();
    super.dispose();
  }

  Future<void> _loadBudgetData() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final data = await _budgetService.getBudgetDetails(
        _selectedDate.month,
        _selectedDate.year,
      );
      if (mounted) {
        setState(() {
          _budgetData = data;
          _isLoading = false;
        });
        _refreshController.refreshCompleted();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        _refreshController.refreshFailed();
        WidgetUtil.toast("Failed to load budget data");
      }
    }
  }

  Future<void> _selectMonth() async {
    final DateTime? picked = await showMonthPicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
      _loadBudgetData();
    }
  }

  void _changeMonth(int delta) {
    int y = _selectedDate.year;
    int m = _selectedDate.month + delta;
    while (m <= 0) {
      m += 12;
      y -= 1;
    }
    while (m > 12) {
      m -= 12;
      y += 1;
    }
    final candidate = DateTime(y, m, 1);
    if (candidate.isBefore(_minDate) || candidate.isAfter(_maxDate)) {
      return;
    }
    setState(() {
      _selectedDate = candidate;
    });
    _loadBudgetData();
  }

  void _showAllocationDialog(
      int categoryId, String categoryName, double currentAllocation) {
    if (_budgetData == null) return;

    showDialog(
      context: context,
      builder: (context) => AllocationDialog(
        categoryId: categoryId,
        categoryName: categoryName,
        month: _selectedDate.month,
        year: _selectedDate.year,
        currentAllocation: currentAllocation,
        availableToAssign: _budgetData!.availableToAssign,
        onSuccess: _loadBudgetData,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: false,
        leading: IconButton(
          icon: Icon(Icons.chevron_left),
          tooltip: 'Previous month',
          onPressed: () => _changeMonth(-1),
        ),
        title: GestureDetector(
          onTap: _selectMonth,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                DateFormat('MMMM yyyy').format(_selectedDate),
                style: TextStyle(fontSize: 18),
              ),
              Icon(Icons.arrow_drop_down),
            ],
          ),
        ),
        centerTitle: true,
        titleSpacing: 16,
        actions: [
          IconButton(
            icon: Icon(Icons.chevron_right),
            tooltip: 'Next month',
            onPressed: () => _changeMonth(1),
          ),
        ],
      ),
      body: _isLoading
          ? Center(child: WidgetUtil.spinLoader())
          : _budgetData == null
              ? Center(child: Text("No data available"))
              : SmartRefresher(
                  controller: _refreshController,
                  enablePullDown: true,
                  enablePullUp: false,
                  onRefresh: () async {
                    await _loadBudgetData();
                  },
                  child: ListView.builder(
                    physics: const AlwaysScrollableScrollPhysics(),
                    itemCount: _budgetData!.categories.length + 1,
                    itemBuilder: (context, index) {
                      if (index == 0) return _buildSummaryCard();

                      final category = _budgetData!.categories[index - 1];
                      return BudgetCategoryTile(
                        category: category,
                        onTap: () => _showAllocationDialog(
                          category.categoryId,
                          category.categoryName,
                          category.allocatedAmount,
                        ),
                      );
                    },
                  ),
                ),
    );
  }

  Widget _buildSummaryCard() {
    if (_budgetData == null) return Container();

    return Card(
      elevation: 0,
      margin: EdgeInsets.all(16),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side:
            BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      color: Theme.of(context).cardColor,
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 16, horizontal: 16),
        child: Column(
          children: [
            Text(
              "Available to Assign",
              style: TextStyle(
                fontSize: 14,
                color: Theme.of(context).hintColor,
                fontWeight: FontWeight.w500,
              ),
            ),
            SizedBox(height: 2),
            Text(
              CommonUtil.toCurrency(_budgetData!.availableToAssign),
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: _budgetData!.availableToAssign >= 0
                    ? Colors.green[700]
                    : Colors.red[700],
              ),
            ),
            SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildSummaryItem(
                  "Total Income",
                  _budgetData!.totalIncome,
                  Colors.green,
                ),
                _buildSummaryItem(
                  "Rollover",
                  _budgetData!.rolloverAmount,
                  Colors.orange,
                ),
              ],
            ),
            SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildSummaryItem(
                  "Total Budgeted",
                  _budgetData!.totalBudget,
                  Colors.blue,
                ),
                _buildSummaryItem(
                  "Total Spent",
                  _budgetData!.totalSpent,
                  Colors.red,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryItem(String label, double amount, Color color) {
    return Column(
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: Colors.grey[600],
          ),
        ),
        SizedBox(height: 2),
        Text(
          CommonUtil.toCurrency(amount),
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: color,
          ),
        ),
      ],
    );
  }
}
