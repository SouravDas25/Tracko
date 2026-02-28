import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/models/user_currency.dart';
import 'package:tracko/repositories/user_currency_repository.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/services/exchange_rate_service.dart';
import 'package:flutter/material.dart';

class CurrencySettingsPage extends StatefulWidget {
  @override
  _CurrencySettingsPageState createState() => _CurrencySettingsPageState();
}

class _CurrencySettingsPageState extends State<CurrencySettingsPage> {
  late String baseCurrency;
  List<UserCurrency> secondaryCurrencies = [];
  final UserCurrencyRepository _repo = UserCurrencyRepository();
  final ExchangeRateService _rateService = ExchangeRateService();
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  _loadData() async {
    try {
      final user = await SessionService.fetchMe();
      baseCurrency = user.baseCurrency;
      secondaryCurrencies = await _repo.getAll();
    } catch (e) {
      print(e);
    } finally {
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    }
  }

  _showAddCurrencyDialog({UserCurrency? existing}) {
    String selectedCurrency = existing?.currencyCode ??
        ConstantUtil.CURRENCIES.firstWhere(
            (c) =>
                c != baseCurrency &&
                !secondaryCurrencies.any((sc) => sc.currencyCode == c),
            orElse: () => baseCurrency);
    TextEditingController rateController =
        TextEditingController(text: existing?.exchangeRate.toString() ?? '');

    bool isFetching = false;

    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(builder: (context, setStateDialog) {
          _fetchRate() async {
            setStateDialog(() {
              isFetching = true;
            });
            try {
              // We need to find the rate such that:
              // 1 SecondaryCurrency (selectedCurrency) = X BaseCurrency
              // So we fetch rates for selectedCurrency and look for BaseCurrency
              double? rate = await _rateService.getExchangeRate(
                  selectedCurrency, baseCurrency);

              if (rate != null) {
                rateController.text = rate.toString();
              } else {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text("Failed to fetch rate")),
                );
              }
            } catch (e) {
              print(e);
            } finally {
              if (mounted) {
                setStateDialog(() {
                  isFetching = false;
                });
              }
            }
          }

          return AlertDialog(
            title: Text(existing == null ? "Add Currency" : "Update Rate"),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (existing == null)
                  DropdownButtonFormField<String>(
                    value: selectedCurrency,
                    decoration: InputDecoration(labelText: "Currency"),
                    items: ConstantUtil.CURRENCIES
                        .where((c) =>
                            c != baseCurrency &&
                            (existing != null ||
                                !secondaryCurrencies
                                    .any((sc) => sc.currencyCode == c) ||
                                c == selectedCurrency))
                        .map((c) => DropdownMenuItem(
                              value: c,
                              child: Text(
                                  "$c (${CommonUtil.getCurrencySymbol(c)})"),
                            ))
                        .toList(),
                    onChanged: (val) {
                      setStateDialog(() {
                        selectedCurrency = val!;
                        // Optionally auto-fetch when currency changes?
                        // Let's keep it manual via button for now to avoid spamming
                        rateController.clear();
                      });
                    },
                  )
                else
                  Text(
                      "Currency: $selectedCurrency (${CommonUtil.getCurrencySymbol(selectedCurrency)})",
                      style:
                          TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: rateController,
                        keyboardType:
                            TextInputType.numberWithOptions(decimal: true),
                        decoration: InputDecoration(
                          labelText: "Exchange Rate",
                          helperText: "1 $selectedCurrency = ? $baseCurrency",
                        ),
                      ),
                    ),
                    SizedBox(width: 8),
                    if (isFetching)
                      SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(strokeWidth: 2))
                    else
                      IconButton(
                        icon: Icon(Icons.download_rounded),
                        tooltip: "Fetch live rate",
                        onPressed: _fetchRate,
                      )
                  ],
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: Text("Cancel"),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (rateController.text.isEmpty) return;
                  try {
                    LoadingDialog.show(context);
                    double rate = double.parse(rateController.text);
                    await _repo.save(UserCurrency(
                        currencyCode: selectedCurrency, exchangeRate: rate));

                    // Fetch updated list from API directly
                    final updatedList = await _repo.getAll();

                    setState(() {
                      secondaryCurrencies = updatedList;
                    });

                    Navigator.pop(context); // Close loading
                    Navigator.pop(context); // Close dialog
                  } catch (e) {
                    Navigator.pop(context); // Close loading
                    print(e);
                  }
                },
                child: Text("Save"),
              ),
            ],
          );
        });
      },
    );
  }

  _deleteCurrency(String code) async {
    try {
      LoadingDialog.show(context);
      await _repo.delete(code);

      final updatedList = await _repo.getAll();
      setState(() {
        secondaryCurrencies = updatedList;
      });

      Navigator.pop(context);
    } catch (e) {
      Navigator.pop(context);
      print(e);
    }
  }

  _refreshAllRates() async {
    if (secondaryCurrencies.isEmpty) {
      WidgetUtil.toast("No secondary currencies to update");
      return;
    }

    try {
      LoadingDialog.show(context);
      // Fetch rates where 1 Base = X Secondary
      // We need 1 Secondary = Y Base, which is 1/X
      final rates = await _rateService.getRatesForBase(baseCurrency);

      if (rates != null) {
        int updatedCount = 0;
        for (var uc in secondaryCurrencies) {
          final code = uc.currencyCode;
          if (rates.containsKey(code)) {
            final rateToSecondary = rates[code];
            if (rateToSecondary != null && rateToSecondary > 0) {
              final newRate = 1 / rateToSecondary;
              await _repo.save(
                  UserCurrency(currencyCode: code, exchangeRate: newRate));
              updatedCount++;
            }
          }
        }

        final updatedList = await _repo.getAll();
        setState(() {
          secondaryCurrencies = updatedList;
        });

        Navigator.pop(context); // hide loading
        WidgetUtil.toast("Updated $updatedCount currencies");
      } else {
        Navigator.pop(context); // hide loading
        WidgetUtil.toast("Failed to fetch rates");
      }
    } catch (e) {
      Navigator.pop(context); // hide loading
      print(e);
      WidgetUtil.toast("Error updating rates");
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return Scaffold(
        appBar: AppBar(title: Text("Currency Settings")),
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text("Currency Settings"),
        actions: [
          IconButton(
            icon: Icon(Icons.refresh),
            tooltip: "Refresh all rates",
            onPressed: _refreshAllRates,
          )
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddCurrencyDialog(),
        child: Icon(Icons.add),
      ),
      body: ListView(
        children: [
          ListTile(
            title: Text("Base Currency"),
            subtitle: Text(
                "$baseCurrency (${CommonUtil.getCurrencySymbol(baseCurrency)})"),
            leading: CircleAvatar(
                child: Text(CommonUtil.getCurrencySymbol(baseCurrency))),
          ),
          Divider(),
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Text("Secondary Currencies",
                style:
                    TextStyle(fontWeight: FontWeight.bold, color: Colors.grey)),
          ),
          if (secondaryCurrencies.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Text("No secondary currencies configured."),
            ),
          ...secondaryCurrencies.map((uc) {
            return ListTile(
              title: Text(
                  "${uc.currencyCode} (${CommonUtil.getCurrencySymbol(uc.currencyCode)})"),
              subtitle: Text(
                  "Rate: 1 ${uc.currencyCode} = ${uc.exchangeRate} $baseCurrency"),
              leading: Icon(Icons.monetization_on_outlined),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  IconButton(
                    icon: Icon(Icons.edit, color: Colors.blue),
                    onPressed: () => _showAddCurrencyDialog(existing: uc),
                  ),
                  IconButton(
                    icon: Icon(Icons.delete, color: Colors.red),
                    onPressed: () => _deleteCurrency(uc.currencyCode),
                  ),
                ],
              ),
            );
          }).toList(),
        ],
      ),
    );
  }
}
