class UserCurrency {
  String currencyCode;
  double exchangeRate;

  UserCurrency({required this.currencyCode, required this.exchangeRate});

  factory UserCurrency.fromJson(Map<String, dynamic> json) {
    return UserCurrency(
      currencyCode: json['currencyCode'],
      exchangeRate: (json['exchangeRate'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'currencyCode': currencyCode,
      'exchangeRate': exchangeRate,
    };
  }
}
