import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/di/di.dart';
import 'package:tracko/models/transaction_history_entry.dart';
import 'package:tracko/repositories/transaction_repository.dart';

/// Shows a transaction's change history (when [transactionId] is provided) or the
/// global, paged history across every transaction (when it is null). In the global view an
/// operation filter is offered — selecting "Deleted" gives the recycle-bin view. The global
/// list is loaded a page at a time and grows as you scroll. Each entry can be reverted: an
/// edit is rolled back, a deleted transaction restored.
class TransactionHistoryPage extends StatefulWidget {
  /// When null, the page shows the global history across all transactions.
  final int? transactionId;

  /// Optional operation the global view opens pre-filtered to (e.g. `DELETE` for
  /// a recycle-bin entry point). Ignored for a single transaction's timeline.
  final String? initialFilter;

  const TransactionHistoryPage(
      {Key? key, this.transactionId, this.initialFilter})
      : super(key: key);

  bool get isGlobal => transactionId == null;

  @override
  State<TransactionHistoryPage> createState() => _TransactionHistoryPageState();
}

/// One selectable operation filter in the global view. A null [operation] means "all".
class _Filter {
  final String label;
  final String? operation;
  const _Filter(this.label, this.operation);
}

class _TransactionHistoryPageState extends State<TransactionHistoryPage> {
  static const int _pageSize = 30;
  static const List<_Filter> _filters = [
    _Filter('All', null),
    _Filter('Created', 'CREATE'),
    _Filter('Updated', 'UPDATE'),
    _Filter('Deleted', 'DELETE'),
    _Filter('Reverted', 'REVERT'),
  ];

  late final TransactionRepository _repository;
  final ScrollController _scrollController = ScrollController();

  List<TransactionHistoryEntry> _entries = [];
  String? _filter;
  int _page = 0;
  bool _hasNext = false;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  bool _changed = false;

  @override
  void initState() {
    super.initState();
    _repository = sl<TransactionRepository>();
    _filter = widget.isGlobal ? widget.initialFilter : null;
    _scrollController.addListener(_onScroll);
    _reload();
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (!widget.isGlobal || !_hasNext || _isLoadingMore) return;
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 300) {
      _loadMore();
    }
  }

  /// Loads the first page (global) or the full timeline (single transaction), replacing the list.
  Future<void> _reload() async {
    setState(() => _isLoading = true);
    try {
      if (widget.isGlobal) {
        final result = await _repository.getAllHistory(
            operation: _filter, page: 0, size: _pageSize);
        setState(() {
          _entries = result.entries;
          _page = result.page;
          _hasNext = result.hasNext;
          _isLoading = false;
        });
      } else {
        final list = await _repository.getHistory(widget.transactionId!, operation: _filter);
        setState(() {
          _entries = list;
          _hasNext = false;
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() => _isLoading = false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to load history: $e')),
      );
    }
  }

  /// Appends the next page of the global history.
  Future<void> _loadMore() async {
    if (_isLoadingMore || !_hasNext) return;
    setState(() => _isLoadingMore = true);
    try {
      final result = await _repository.getAllHistory(
          operation: _filter, page: _page + 1, size: _pageSize);
      setState(() {
        _entries = [..._entries, ...result.entries];
        _page = result.page;
        _hasNext = result.hasNext;
        _isLoadingMore = false;
      });
    } catch (e) {
      setState(() => _isLoadingMore = false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to load more: $e')),
      );
    }
  }

  void _selectFilter(String? operation) {
    if (_filter == operation) return;
    setState(() => _filter = operation);
    _reload();
  }

  Future<void> _revert(TransactionHistoryEntry entry) async {
    if (entry.id == null) return;
    final isDelete = entry.operation == 'DELETE';
    final actionWord = isDelete ? 'Restore' : 'Revert';
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('$actionWord transaction'),
        content: Text(isDelete
            ? 'Restore "${entry.name ?? 'this transaction'}" from the recycle bin?'
            : 'Roll "${entry.name ?? 'this transaction'}" back to this version?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(actionWord),
          ),
        ],
      ),
    );

    if (confirm != true) return;

    try {
      await _repository.revertHistory(entry.id!);
      _changed = true;
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
            content: Text(
                isDelete ? 'Transaction restored' : 'Transaction reverted')),
      );
      _reload();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to $actionWord: $e')),
      );
    }
  }

  ({IconData icon, Color color}) _operationStyle(String? operation) {
    switch (operation) {
      case 'DELETE':
        return (icon: Icons.delete_outline, color: Colors.red);
      case 'UPDATE':
        return (icon: Icons.edit_outlined, color: Colors.orange);
      case 'CREATE':
        return (icon: Icons.add_circle_outline, color: Colors.green);
      case 'REVERT':
        return (icon: Icons.undo, color: Colors.blue);
      default:
        return (icon: Icons.history, color: Colors.grey);
    }
  }

  String get _emptyMessage {
    if (!widget.isGlobal) return 'No history for this transaction';
    if (_filter == 'DELETE') return 'Recycle bin is empty';
    return 'No history yet';
  }

  Widget _buildFilterBar() {
    return SizedBox(
      height: 52,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        itemCount: _filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final f = _filters[index];
          return ChoiceChip(
            label: Text(f.label),
            selected: _filter == f.operation,
            onSelected: (_) => _selectFilter(f.operation),
          );
        },
      ),
    );
  }

  Widget _buildEntryCard(TransactionHistoryEntry entry) {
    final style = _operationStyle(entry.operation);
    final isDelete = entry.operation == 'DELETE';
    final when = entry.changedAt != null
        ? DateFormat('MMM dd, yyyy • HH:mm').format(entry.changedAt!.toLocal())
        : '';
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: style.color.withOpacity(0.12),
          child: Icon(style.icon, color: style.color),
        ),
        title: Text(
          entry.name ?? '(unnamed)',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${entry.operation ?? ''} • ${CommonUtil.toCurrency(entry.amount ?? 0)}',
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
            Text(
              when,
              style: const TextStyle(fontSize: 12, color: Colors.grey),
            ),
            if (entry.changes.isNotEmpty) ...[
              const SizedBox(height: 6),
              ...entry.changes.map(_buildChangeRow),
            ],
          ],
        ),
        trailing: TextButton.icon(
          icon: Icon(isDelete ? Icons.restore : Icons.undo, size: 18),
          label: Text(isDelete ? 'Restore' : 'Revert'),
          onPressed: () => _revert(entry),
        ),
      ),
    );
  }

  /// Renders one diff row, e.g. `Type: DEBIT → CREDIT` (old value struck through).
  Widget _buildChangeRow(HistoryFieldChange c) {
    final before = (c.before == null || c.before!.isEmpty) ? '—' : c.before!;
    final after = (c.after == null || c.after!.isEmpty) ? '—' : c.after!;
    return Padding(
      padding: const EdgeInsets.only(top: 2),
      child: Text.rich(
        TextSpan(
          style: const TextStyle(fontSize: 12),
          children: [
            TextSpan(
              text: '${c.field}: ',
              style: const TextStyle(
                  fontWeight: FontWeight.w600, color: Colors.blueGrey),
            ),
            TextSpan(
              text: before,
              style: const TextStyle(
                  color: Colors.grey,
                  decoration: TextDecoration.lineThrough),
            ),
            const TextSpan(text: '  →  ', style: TextStyle(color: Colors.grey)),
            TextSpan(
              text: after,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    if (_entries.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(_filter == 'DELETE' ? Icons.delete_outline : Icons.history,
                size: 64, color: Colors.grey),
            const SizedBox(height: 16),
            Text(_emptyMessage, style: const TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    final showTrailingLoader = widget.isGlobal && _hasNext;
    return RefreshIndicator(
      onRefresh: _reload,
      child: ListView.builder(
        controller: _scrollController,
        padding: const EdgeInsets.all(16),
        itemCount: _entries.length + (showTrailingLoader ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= _entries.length) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(child: CircularProgressIndicator()),
            );
          }
          return _buildEntryCard(_entries[index]);
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.isGlobal ? 'Transaction History' : 'History';
    return WillPopScope(
      onWillPop: () async {
        Navigator.pop(context, _changed);
        return false;
      },
      child: Scaffold(
        appBar: AppBar(title: Text(title)),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : Column(
                children: [
                  _buildFilterBar(),
                  Expanded(child: _buildList()),
                ],
              ),
      ),
    );
  }
}
