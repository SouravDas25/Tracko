import 'package:flutter/material.dart';

import 'package:tracko/component/amount_text.dart';
import 'package:tracko/component/highlight_text.dart';
import 'package:tracko/models/search_hit.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';

/// A widget that displays a search hit result with highlighted text and relevance indicator.
///
/// Displays transaction details including:
/// - Transaction name with search highlight
/// - Amount with color based on transaction type
/// - Date formatted consistently
/// - Category and account names as subtitle
/// - Avatar with initials
/// - Relevance score indicator
/// - Highlights for all matched fields
///
/// Validates: Requirements 2.1, 2.2, 2.3, 2.5, 3.1, 3.3
class SearchHitTile extends StatelessWidget {
  final SearchHit hit;
  final VoidCallback onTap;

  const SearchHitTile({
    Key? key,
    required this.hit,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor.withOpacity(0.08),
            width: 0.5,
          ),
        ),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
            child: Row(
              children: [
                _buildAvatar(context),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildTitle(context),
                      const SizedBox(height: 2),
                      _buildSubtitle(context),
                      if (_hasMatchedFieldHighlights()) ...[
                        const SizedBox(height: 4),
                        _buildMatchedFieldsHighlights(context),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                _buildTrailing(context),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Builds the avatar with initials from transaction name.
  Widget _buildAvatar(BuildContext context) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColor,
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Text(
          CommonUtil.getInitials(hit.transaction.name),
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
    );
  }

  /// Builds the title row with transaction name and relevance indicator.
  Widget _buildTitle(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: HighlightText(
            text: hit.transaction.name,
            highlightedText: hit.getHighlight('name'),
            baseStyle: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).textTheme.bodyLarge?.color,
            ),
          ),
        ),
        const SizedBox(width: 8),
        _buildRelevanceIndicator(context),
      ],
    );
  }

  /// Builds the subtitle with category, account, and date.
  Widget _buildSubtitle(BuildContext context) {
    final parts = <String>[];

    // Add category name
    if (hit.transaction.category != null) {
      parts.add(hit.transaction.category!.name);
    }

    // Add account name
    if (hit.transaction.account != null) {
      parts.add(hit.transaction.account!.name);
    }

    // Add date
    final formattedDate = CommonUtil.humanDate(hit.transaction.date);
    parts.add(formattedDate);

    return Text(
      parts.join(' • '),
      style: TextStyle(
        fontSize: 11.5,
        color: Theme.of(context).hintColor,
        fontWeight: FontWeight.w500,
      ),
      maxLines: 1,
      overflow: TextOverflow.ellipsis,
    );
  }

  /// Builds the trailing section with amount.
  Widget _buildTrailing(BuildContext context) {
    return _buildAmount(context);
  }

  /// Builds the amount display with color based on transaction type.
  Widget _buildAmount(BuildContext context) {
    final isDebit = hit.transaction.isDebit;
    final isTransfer = hit.transaction.isTransfer;

    Color color = Colors.blue;
    if (!isTransfer) {
      color = isDebit ? Colors.red : Colors.green;
    }

    return AmountText(
      amount: hit.transaction.originalAmount ?? hit.transaction.amount,
      color: color,
      currencyCode: hit.transaction.originalCurrency,
    );
  }

  /// Builds a visual indicator for relevance score.
  Widget _buildRelevanceIndicator(BuildContext context) {
    // Normalize score to 0.0 - 1.0 range (assuming scores are typically 0-2)
    final normalizedScore = (hit.relevanceScore / 2.0).clamp(0.0, 1.0);

    // Color based on relevance: low (grey) -> medium (orange) -> high (green)
    Color indicatorColor;
    if (normalizedScore >= 0.7) {
      indicatorColor = Colors.green;
    } else if (normalizedScore >= 0.4) {
      indicatorColor = Colors.orange;
    } else {
      indicatorColor = Colors.grey;
    }

    return Container(
      width: 8,
      height: 8,
      decoration: BoxDecoration(
        color: indicatorColor,
        shape: BoxShape.circle,
      ),
    );
  }

  /// Checks if there are any matched field highlights to display.
  bool _hasMatchedFieldHighlights() {
    return hit.getHighlight('comments') != null ||
        hit.getHighlight('category') != null ||
        hit.getHighlight('account') != null;
  }

  /// Builds highlights for all matched fields (comments, category, account).
  Widget _buildMatchedFieldsHighlights(BuildContext context) {
    final highlightWidgets = <Widget>[];

    // Add comments highlight
    final commentsHighlight = hit.getHighlight('comments');
    if (commentsHighlight != null) {
      highlightWidgets.add(_buildFieldHighlight(
        context,
        label: 'Comments',
        highlightedText: commentsHighlight,
      ));
    }

    // Add category highlight
    final categoryHighlight = hit.getHighlight('category');
    if (categoryHighlight != null) {
      highlightWidgets.add(_buildFieldHighlight(
        context,
        label: 'Category',
        highlightedText: categoryHighlight,
      ));
    }

    // Add account highlight
    final accountHighlight = hit.getHighlight('account');
    if (accountHighlight != null) {
      highlightWidgets.add(_buildFieldHighlight(
        context,
        label: 'Account',
        highlightedText: accountHighlight,
      ));
    }

    if (highlightWidgets.isEmpty) {
      return const SizedBox.shrink();
    }

    return Wrap(
      spacing: 8,
      runSpacing: 4,
      children: highlightWidgets,
    );
  }

  /// Builds a single field highlight with label.
  Widget _buildFieldHighlight(
    BuildContext context, {
    required String label,
    required String highlightedText,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: Theme.of(context).highlightColor.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            '$label: ',
            style: TextStyle(
              fontSize: 10,
              color: Theme.of(context).hintColor,
              fontWeight: FontWeight.w500,
            ),
          ),
          Flexible(
            child: HighlightText(
              text: _stripEmTags(highlightedText),
              highlightedText: highlightedText,
              baseStyle: TextStyle(
                fontSize: 10,
                color: Theme.of(context).textTheme.bodySmall?.color,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Strips <em> tags from text to get plain text for display.
  String _stripEmTags(String text) {
    return text
        .replaceAll('<em>', '')
        .replaceAll('</em>', '');
  }
}
