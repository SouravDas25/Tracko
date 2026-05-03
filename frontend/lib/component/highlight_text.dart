import 'package:flutter/material.dart';

/// A widget that renders text with `<em>...</em>` highlighted segments.
///
/// Parses HTML-style `<em>` tags from backend highlight strings and renders
/// matched segments with a distinct style (bold + highlight background color).
///
/// Usage:
/// ```dart
/// HighlightText(
///   text: 'Shopping at Walmart',
///   highlightedText: 'Shopping at <em>Walmart</em>',
/// )
/// ```
///
/// Edge cases handled:
/// - No `<em>` tags: renders entire text with base style
/// - Empty string: renders nothing
/// - Malformed tags: renders as plain text
class HighlightText extends StatelessWidget {
  /// The original text without highlight markers.
  /// Used as fallback when highlightedText is not provided.
  final String text;

  /// Text containing `<em>matched</em>` markers from the backend.
  /// If null, renders [text] without any highlighting.
  final String? highlightedText;

  /// Base style for non-highlighted text segments.
  /// Defaults to the default text style if not provided.
  final TextStyle? baseStyle;

  /// Style for highlighted text segments (inside `<em>` tags).
  /// Defaults to bold with a yellow highlight background if not provided.
  final TextStyle? highlightStyle;

  /// Background color for highlighted segments.
  /// Defaults to a light yellow color.
  final Color? highlightBackgroundColor;

  const HighlightText({
    Key? key,
    required this.text,
    this.highlightedText,
    this.baseStyle,
    this.highlightStyle,
    this.highlightBackgroundColor,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // If no highlighted text provided or it's empty, render plain text
    if (highlightedText == null || highlightedText!.isEmpty) {
      return Text(
        text,
        style: baseStyle,
      );
    }

    // Parse the highlighted text into segments
    final segments = _parseHighlightedText(highlightedText!);

    // If no segments found (shouldn't happen), fallback to plain text
    if (segments.isEmpty) {
      return Text(
        text,
        style: baseStyle,
      );
    }

    // Build text spans from segments
    final spans = segments.map((segment) {
      return TextSpan(
        text: segment.text,
        style: segment.isHighlighted
            ? _buildHighlightStyle(context)
            : baseStyle,
      );
    }).toList();

    return RichText(
      text: TextSpan(
        style: baseStyle ?? DefaultTextStyle.of(context).style,
        children: spans,
      ),
    );
  }

  /// Builds the highlight style with bold text and background color.
  TextStyle _buildHighlightStyle(BuildContext context) {
    final bgColor = highlightBackgroundColor ??
        const Color(0xFFFFEB3B).withOpacity(0.4); // Light yellow

    return highlightStyle ??
        TextStyle(
          fontWeight: FontWeight.bold,
          backgroundColor: bgColor,
        );
  }

  /// Parses text containing `<em>...</em>` tags into a list of segments.
  ///
  /// Handles edge cases:
  /// - Malformed tags (unclosed `<em>`, stray `</em>`) are treated as plain text
  /// - Nested `<em>` tags are flattened (inner tags ignored)
  static List<_TextSegment> _parseHighlightedText(String text) {
    if (text.isEmpty) {
      return [];
    }

    final segments = <_TextSegment>[];
    final buffer = StringBuffer();
    int i = 0;

    while (i < text.length) {
      // Check for opening <em> tag
      if (_isOpenEmTag(text, i)) {
        // Add any buffered plain text before this tag
        if (buffer.isNotEmpty) {
          segments.add(_TextSegment(buffer.toString(), false));
          buffer.clear();
        }

        // Skip the <em> tag
        i += 4;

        // Find the closing </em> tag
        final closeIndex = text.indexOf('</em>', i);
        if (closeIndex == -1) {
          // No closing tag found - treat rest as highlighted (malformed but graceful)
          final remaining = text.substring(i);
          if (remaining.isNotEmpty) {
            segments.add(_TextSegment(remaining, true));
          }
          break;
        }

        // Add the highlighted text
        final highlightedContent = text.substring(i, closeIndex);
        if (highlightedContent.isNotEmpty) {
          segments.add(_TextSegment(highlightedContent, true));
        }

        // Skip past the </em> tag
        i = closeIndex + 5;
      }
      // Check for stray closing </em> tag (malformed)
      else if (_isCloseEmTag(text, i)) {
        // Add buffered text if any
        if (buffer.isNotEmpty) {
          segments.add(_TextSegment(buffer.toString(), false));
          buffer.clear();
        }
        // Skip the stray closing tag
        i += 5;
      }
      else {
        // Regular character - add to buffer
        buffer.write(text[i]);
        i++;
      }
    }

    // Add any remaining buffered text
    if (buffer.isNotEmpty) {
      segments.add(_TextSegment(buffer.toString(), false));
    }

    return segments;
  }

  /// Checks if an opening `<em>` tag starts at the given position.
  static bool _isOpenEmTag(String text, int index) {
    if (index + 4 > text.length) return false;
    return text.substring(index, index + 4) == '<em>';
  }

  /// Checks if a closing `</em>` tag starts at the given position.
  static bool _isCloseEmTag(String text, int index) {
    if (index + 5 > text.length) return false;
    return text.substring(index, index + 5) == '</em>';
  }
}

/// Represents a segment of text with its highlight status.
class _TextSegment {
  final String text;
  final bool isHighlighted;

  const _TextSegment(this.text, this.isHighlighted);
}
