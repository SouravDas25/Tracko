import 'package:flutter/material.dart';

/// A compact pill-shaped dropdown for filter bars and inline contexts.
///
/// Renders a rounded container with the theme's card color, a subtle border,
/// and a [DropdownButton] inside. The down-arrow icon uses the theme's primary
/// color.
class AppPillDropdown<T> extends StatelessWidget {
  final T value;
  final List<T> items;
  final String Function(T) labelBuilder;
  final ValueChanged<T> onChanged;
  final bool isExpanded;

  const AppPillDropdown({
    Key? key,
    required this.value,
    required this.items,
    required this.labelBuilder,
    required this.onChanged,
    this.isExpanded = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      height: 40,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: theme.dividerColor.withOpacity(0.1),
        ),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: isExpanded,
          icon: Padding(
            padding: const EdgeInsets.only(left: 4.0),
            child: Icon(
              Icons.keyboard_arrow_down,
              size: 18,
              color: theme.primaryColor,
            ),
          ),
          isDense: true,
          dropdownColor: theme.cardColor,
          style: TextStyle(
            color: theme.textTheme.bodyLarge?.color,
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
}

/// A form-integrated dropdown with label, prefix icon, and validation support.
///
/// Renders a [DropdownButtonFormField] with a standardized [InputDecoration]:
/// rounded border (radius 16), prefix icon, filled background using the
/// theme's card color, and a focused border using the theme's primary color.
class AppFormDropdown<T> extends StatelessWidget {
  final T? value;
  final List<T> items;
  final String Function(T) labelBuilder;
  final String label;
  final IconData? icon;
  final ValueChanged<T?> onChanged;
  final String? Function(T?)? validator;
  final Widget? hint;
  final bool filled;

  const AppFormDropdown({
    Key? key,
    required this.value,
    required this.items,
    required this.labelBuilder,
    required this.label,
    this.icon,
    required this.onChanged,
    this.validator,
    this.hint,
    this.filled = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return DropdownButtonFormField<T>(
      value: value,
      hint: hint,
      validator: validator,
      dropdownColor: theme.cardColor,
      style: TextStyle(
        color: theme.textTheme.bodyLarge?.color,
        fontSize: 14,
      ),
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: icon != null ? Icon(icon, color: theme.primaryColor) : null,
        filled: filled,
        fillColor: filled ? theme.cardColor : null,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: theme.dividerColor),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: theme.dividerColor),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: theme.primaryColor, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: theme.colorScheme.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: theme.colorScheme.error, width: 2),
        ),
      ),
      items: items.map((item) {
        return DropdownMenuItem<T>(
          value: item,
          child: Text(labelBuilder(item)),
        );
      }).toList(),
      onChanged: onChanged,
    );
  }
}

/// A bottom sheet picker that displays a scrollable selection list in a modal.
///
/// Renders a trigger widget with a leading icon, label, optional clear button,
/// and a trailing down-arrow. On tap, opens a [showModalBottomSheet] constrained
/// to 50% screen height with rounded top corners. The sheet shows a header row,
/// an "All" option, and the item list with checkmarks on the selected item.
class AppBottomSheetPicker<T> extends StatelessWidget {
  final T? value;
  final List<T> items;
  final String title;
  final String Function(T) labelBuilder;
  final IconData Function(T) iconBuilder;
  final ValueChanged<T?> onSelected;
  final String allItemsLabel;
  final String Function(T?)? triggerLabelBuilder;
  final bool isExpanded;

  const AppBottomSheetPicker({
    Key? key,
    required this.value,
    required this.items,
    required this.title,
    required this.labelBuilder,
    required this.iconBuilder,
    required this.onSelected,
    this.allItemsLabel = 'All',
    this.triggerLabelBuilder,
    this.isExpanded = false,
  }) : super(key: key);

  String _triggerText() {
    if (triggerLabelBuilder != null) return triggerLabelBuilder!(value);
    return value != null ? labelBuilder(value as T) : allItemsLabel;
  }

  void _openSheet(BuildContext context) {
    final theme = Theme.of(context);

    showModalBottomSheet(
      context: context,
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.5,
      ),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      backgroundColor: theme.cardColor,
      builder: (sheetContext) {
        return Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header row
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    title,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  if (value != null)
                    TextButton(
                      onPressed: () {
                        Navigator.pop(sheetContext);
                        onSelected(null);
                      },
                      child: Text(
                        'Clear',
                        style: TextStyle(color: theme.primaryColor),
                      ),
                    ),
                ],
              ),
            ),
            const Divider(height: 1),
            // Item list
            Expanded(
              child: ListView(
                children: [
                  // "All" option
                  ListTile(
                    leading: Icon(Icons.select_all, color: theme.primaryColor),
                    title: Text(allItemsLabel),
                    trailing: value == null
                        ? Icon(Icons.check, color: theme.primaryColor)
                        : null,
                    onTap: () {
                      Navigator.pop(sheetContext);
                      onSelected(null);
                    },
                  ),
                  // Item entries
                  ...items.map((item) {
                    final isSelected = item == value;
                    return ListTile(
                      leading:
                          Icon(iconBuilder(item), color: theme.primaryColor),
                      title: Text(labelBuilder(item)),
                      trailing: isSelected
                          ? Icon(Icons.check, color: theme.primaryColor)
                          : null,
                      onTap: () {
                        Navigator.pop(sheetContext);
                        onSelected(item);
                      },
                    );
                  }),
                ],
              ),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final bool hasValue = value != null;

    // When used in expanded/form mode, wrap in InputDecorator for a matching
    // floating-label style identical to TextField / DateTimeField.
    if (isExpanded) {
      return GestureDetector(
        onTap: () => _openSheet(context),
        child: InputDecorator(
          decoration: InputDecoration(
            labelText: allItemsLabel,
            prefixIcon: Icon(
              hasValue ? iconBuilder(value as T) : Icons.select_all,
              size: 20,
              color: theme.hintColor,
            ),
            suffixIcon: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (hasValue)
                  GestureDetector(
                    onTap: () => onSelected(null),
                    child: Icon(Icons.close, size: 16, color: theme.hintColor),
                  ),
                const SizedBox(width: 4),
                Icon(Icons.keyboard_arrow_down, size: 18, color: theme.hintColor),
                const SizedBox(width: 4),
              ],
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                color: theme.dividerColor.withOpacity(0.1),
              ),
            ),
            filled: true,
            fillColor: theme.cardColor,
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          ),
          child: Text(
            hasValue ? _triggerText() : '',
            style: TextStyle(
              color: theme.textTheme.bodyLarge?.color,
              fontWeight: FontWeight.w600,
              fontSize: 14,
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      );
    }

    // Compact inline mode (no label, used in filter bars etc.)
    return GestureDetector(
      onTap: () => _openSheet(context),
      child: Container(
        height: 48,
        padding: const EdgeInsets.symmetric(horizontal: 14),
        decoration: BoxDecoration(
          color: theme.cardColor,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: theme.dividerColor.withOpacity(0.15),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              value != null ? iconBuilder(value as T) : Icons.select_all,
              size: 18,
              color: theme.hintColor,
            ),
            const SizedBox(width: 10),
            Text(
                    _triggerText(),
                    style: TextStyle(
                      color: theme.textTheme.bodyLarge?.color,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                    ),
                  ),
            if (value != null)
              Padding(
                padding: const EdgeInsets.only(left: 4),
                child: GestureDetector(
                  onTap: () => onSelected(null),
                  child: Icon(
                    Icons.close,
                    size: 16,
                    color: theme.hintColor,
                  ),
                ),
              ),
            const SizedBox(width: 4),
            Icon(
              Icons.keyboard_arrow_down,
              size: 18,
              color: theme.hintColor,
            ),
          ],
        ),
      ),
    );
  }
}

/// A minimal borderless dropdown for inline contexts (e.g., currency next to
/// an amount field).
///
/// Renders a [DropdownButton] with no underline, a compact drop-down arrow,
/// and customizable text style and icon color.
class AppInlineDropdown<T> extends StatelessWidget {
  final T value;
  final List<T> items;
  final String Function(T) labelBuilder;
  final ValueChanged<T> onChanged;
  final TextStyle? style;
  final Color? iconColor;

  const AppInlineDropdown({
    Key? key,
    required this.value,
    required this.items,
    required this.labelBuilder,
    required this.onChanged,
    this.style,
    this.iconColor,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return DropdownButtonHideUnderline(
      child: DropdownButton<T>(
        value: value,
        isDense: true,
        icon: Icon(
          Icons.arrow_drop_down,
          size: 20,
          color: iconColor ?? theme.primaryColor,
        ),
        dropdownColor: theme.cardColor,
        style: style ??
            TextStyle(
              color: theme.textTheme.bodyLarge?.color,
              fontSize: 14,
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
    );
  }
}
