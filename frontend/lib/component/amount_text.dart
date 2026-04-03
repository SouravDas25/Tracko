import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';

/// A uniform widget for displaying currency amounts across the app.
///
/// Renders the currency symbol slightly larger than the numeric value,
/// both in the specified [color]. Uses [CommonUtil.toCurrency] for formatting.
///
/// Usage:
/// ```dart
/// AmountText(amount: 1500.0, color: Colors.green)
/// AmountText(amount: 1500.0, color: Colors.red, fontSize: 13)
/// AmountText(amount: 1500.0, color: Colors.green, currencyCode: 'USD')
/// ```
class AmountText extends StatelessWidget {
  final double amount;
  final Color? color;
  final double fontSize;
  final String? currencyCode;
  final TextAlign? textAlign;

  const AmountText({
    Key? key,
    required this.amount,
    this.color,
    this.fontSize = 14,
    this.currencyCode,
    this.textAlign,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final resolvedColor = color ?? (amount > 0 ? Colors.green : amount < 0 ? Colors.red : Colors.blue);
    final formatted = currencyCode != null
        ? CommonUtil.toCurrency(amount, currencyCode: currencyCode)
        : CommonUtil.toCurrency(amount);

    // Extract the leading currency symbol (first non-digit, non-minus, non-space chars)
    final symbolMatch = RegExp(r'^[^\d\-\s]+').firstMatch(formatted);
    final symbol = symbolMatch?.group(0) ?? '';
    final rest = symbol.isNotEmpty
        ? formatted.substring(symbol.length).trim()
        : formatted;

    if (symbol.isEmpty) {
      return Text(
        formatted,
        textAlign: textAlign,
        style: TextStyle(
          color: resolvedColor,
          fontWeight: FontWeight.w700,
          fontSize: fontSize,
        ),
      );
    }

    return RichText(
      textAlign: textAlign ?? TextAlign.start,
      text: TextSpan(
        children: [
          TextSpan(
            text: "$symbol ",
            style: TextStyle(
              color: resolvedColor,
              fontWeight: FontWeight.w700,
              fontSize: fontSize + 3,
            ),
          ),
          TextSpan(
            text: rest,
            style: TextStyle(
              color: resolvedColor,
              fontWeight: FontWeight.w700,
              fontSize: fontSize,
            ),
          ),
        ],
      ),
    );
  }
}
