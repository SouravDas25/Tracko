import 'package:flutter/material.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
import 'package:tracko/Utils/enums.dart';

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
    bool isDarkMode = Theme.of(context).brightness == Brightness.dark;
    Color typeColor;
    if (transactionType == TransactionType.DEBIT) {
      typeColor = isDarkMode ? Colors.redAccent : Colors.red;
    } else if (transactionType == TransactionType.TRANSFER) {
      typeColor = isDarkMode ? Colors.lightBlueAccent : Colors.blueGrey;
    } else {
      typeColor = isDarkMode ? Colors.tealAccent : Colors.teal;
    }

    return Card(
      elevation: 0,
      color: Theme.of(context).cardColor,
      shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.1))),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0, horizontal: 20.0),
        child: Column(
          children: [
            Text(
              "Amount",
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
                letterSpacing: 0.5,
              ),
            ),
            SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                DropdownButton<String>(
                  value: availableCurrencies.contains(selectedCurrency)
                      ? selectedCurrency
                      : (availableCurrencies.isNotEmpty
                          ? availableCurrencies.first
                          : null),
                  underline: SizedBox(),
                  icon: Icon(Icons.arrow_drop_down, size: 20, color: typeColor),
                  style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: typeColor),
                  items: availableCurrencies.map((String value) {
                    return DropdownMenuItem<String>(
                      value: value,
                      child: Text(value),
                    );
                  }).toList(),
                  onChanged: (newValue) {
                    if (newValue != null) {
                      onCurrencyChanged(newValue);
                    }
                  },
                ),
                SizedBox(width: 8),
                IntrinsicWidth(
                  child: TextField(
                    controller: amountController,
                    keyboardType:
                        TextInputType.numberWithOptions(decimal: true),
                    style: TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.w700,
                      color: typeColor,
                      height: 1.0,
                    ),
                    decoration: InputDecoration(
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
              SizedBox(height: 20),
              Container(
                padding: EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Theme.of(context).scaffoldBackgroundColor,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Text("Exchange Rate:", style: TextStyle(fontSize: 12)),
                        SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: exchangeRateController,
                            keyboardType:
                                TextInputType.numberWithOptions(decimal: true),
                            decoration: InputDecoration(
                              isDense: true,
                              border: InputBorder.none,
                              contentPadding: EdgeInsets.zero,
                            ),
                            style: TextStyle(
                                fontSize: 14, fontWeight: FontWeight.bold),
                            textAlign: TextAlign.end,
                          ),
                        ),
                      ],
                    ),
                    Divider(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text("Converted:", style: TextStyle(fontSize: 12)),
                        Text(
                          "$baseCurrency ${convertedAmount.toStringAsFixed(2)}",
                          style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color:
                                  Theme.of(context).textTheme.bodyLarge?.color),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
