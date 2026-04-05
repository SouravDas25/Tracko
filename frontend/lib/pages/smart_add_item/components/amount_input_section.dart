import 'package:flutter/material.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/app_dropdown.dart';

class AmountInputSection extends StatelessWidget {
  final int transactionType;
  final String baseCurrency;
  final String selectedCurrency;
  final List<String> availableCurrencies;
  final MoneyMaskedTextController amountController;
  final TextEditingController exchangeRateController;
  final double convertedAmount;
  final Map<String, double> currencyRates;
  final Function(String) onCurrencyChanged;

  const AmountInputSection({
    Key? key,
    required this.transactionType,
    required this.baseCurrency,
    required this.selectedCurrency,
    required this.availableCurrencies,
    required this.amountController,
    required this.exchangeRateController,
    required this.convertedAmount,
    required this.currencyRates,
    required this.onCurrencyChanged,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final typeColor = TransactionType.color(
      transactionType,
      brightness: Theme.of(context).brightness,
    );

    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Theme.of(context).dividerColor.withOpacity(0.1),
        ),
      ),
      padding: const EdgeInsets.symmetric(vertical: 12.0, horizontal: 16.0),
      child: Column(
        children: [
          Text(
            "Amount",
            style: TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).hintColor,
              letterSpacing: 0.5,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              if (availableCurrencies.isNotEmpty)
                AppInlineDropdown<String>(
                  value: availableCurrencies.contains(selectedCurrency)
                      ? selectedCurrency
                      : availableCurrencies.first,
                  items: availableCurrencies,
                  labelBuilder: (currency) => currency,
                  onChanged: onCurrencyChanged,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: typeColor,
                  ),
                  iconColor: typeColor,
                ),
              const SizedBox(width: 6),
              IntrinsicWidth(
                child: TextField(
                  controller: amountController,
                  keyboardType:
                      TextInputType.numberWithOptions(decimal: true),
                  style: TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.w700,
                    color: typeColor,
                    height: 1.0,
                  ),
                  decoration: const InputDecoration(
                    hintText: "0.00",
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.zero,
                    isDense: true,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),
          if (selectedCurrency != baseCurrency) ...[
            const SizedBox(height: 10),
            _buildExchangeRateSection(context),
          ],
        ],
      ),
    );
  }

  Widget _buildExchangeRateSection(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Theme.of(context).scaffoldBackgroundColor,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Column(
        children: [
          Row(
            children: [
              const Text("Exchange Rate:", style: TextStyle(fontSize: 11)),
              const SizedBox(width: 8),
              Expanded(
                child: TextField(
                  controller: exchangeRateController,
                  keyboardType:
                      TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    isDense: true,
                    border: InputBorder.none,
                    contentPadding: EdgeInsets.zero,
                  ),
                  style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.end,
                ),
              ),
            ],
          ),
          const Divider(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text("Converted:", style: TextStyle(fontSize: 11)),
              Text(
                "$baseCurrency ${convertedAmount.toStringAsFixed(2)}",
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: Theme.of(context).textTheme.bodyLarge?.color,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
