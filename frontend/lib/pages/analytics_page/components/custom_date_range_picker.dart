import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/component/app_dropdown.dart';

/// Shows a dialog to pick a custom date range, with an optional year-mode toggle.
/// Returns the selected [DateTimeRange] or null if cancelled.
Future<DateTimeRange?> showCustomDateRangePicker({
  required BuildContext context,
  required DateTime initialStart,
  required DateTime initialEnd,
}) {
  DateTime start = initialStart;
  DateTime end = initialEnd;
  bool isYearMode = false;

  final currentYear = DateTime.now().year;
  final years = List.generate(currentYear - 2000 + 2, (index) => 2000 + index)
      .reversed
      .toList();

  return showDialog<DateTimeRange>(
    context: context,
    builder: (context) {
      return StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: const Text("Select Date Range"),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                SwitchListTile(
                  title: const Text("Select entire years"),
                  value: isYearMode,
                  activeColor: Theme.of(context).primaryColor,
                  onChanged: (val) {
                    setState(() {
                      isYearMode = val;
                      if (isYearMode) {
                        start = DateTime(start.year, 1, 1);
                        end = DateTime(end.year, 12, 31);
                      }
                    });
                  },
                ),
                const Divider(),
                if (isYearMode) ...[
                  _YearRow(
                    label: "From Year:",
                    value: years.contains(start.year) ? start.year : years.first,
                    years: years,
                    onChanged: (val) {
                      setState(() {
                        start = DateTime(val, 1, 1);
                        if (end.year < val) end = DateTime(val, 12, 31);
                      });
                    },
                  ),
                  _YearRow(
                    label: "To Year:",
                    value: years.contains(end.year) ? end.year : years.first,
                    years: years,
                    onChanged: (val) {
                      setState(() {
                        end = DateTime(val, 12, 31);
                        if (start.year > val) start = DateTime(val, 1, 1);
                      });
                    },
                  ),
                ] else ...[
                  _DateTile(
                    title: "Start Date",
                    date: start,
                    onPicked: (d) {
                      setState(() {
                        start = d;
                        if (end.isBefore(start)) end = start;
                      });
                    },
                  ),
                  _DateTile(
                    title: "End Date",
                    date: end,
                    onPicked: (d) {
                      setState(() {
                        end = d;
                        if (start.isAfter(end)) start = end;
                      });
                    },
                  ),
                ],
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text("Cancel"),
              ),
              TextButton(
                onPressed: () =>
                    Navigator.pop(context, DateTimeRange(start: start, end: end)),
                child: const Text("Apply"),
              ),
            ],
          );
        },
      );
    },
  );
}

class _YearRow extends StatelessWidget {
  final String label;
  final int value;
  final List<int> years;
  final ValueChanged<int> onChanged;

  const _YearRow({
    required this.label,
    required this.value,
    required this.years,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontWeight: FontWeight.w600)),
          AppInlineDropdown<int>(
            value: value,
            items: years,
            labelBuilder: (y) => y.toString(),
            onChanged: onChanged,
          ),
        ],
      ),
    );
  }
}

class _DateTile extends StatelessWidget {
  final String title;
  final DateTime date;
  final ValueChanged<DateTime> onPicked;

  const _DateTile({
    required this.title,
    required this.date,
    required this.onPicked,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(title),
      subtitle: Text(DateFormat('MMM dd, yyyy').format(date)),
      trailing: const Icon(Icons.calendar_today),
      onTap: () async {
        final d = await showDatePicker(
          context: context,
          initialDate: date,
          firstDate: DateTime(2000),
          lastDate: DateTime.now().add(const Duration(days: 365)),
        );
        if (d != null) onPicked(d);
      },
    );
  }
}
