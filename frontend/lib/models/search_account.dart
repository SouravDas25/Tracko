import 'package:equatable/equatable.dart';

/// Represents an account in search results.
/// 
/// Validates: Requirements 7.3, 7.4
class SearchAccount extends Equatable {
  final int id;
  final String name;
  final String currency;

  const SearchAccount({
    required this.id,
    required this.name,
    required this.currency,
  });

  factory SearchAccount.fromJson(Map<String, dynamic> json) {
    return SearchAccount(
      id: json['id'] as int,
      name: json['name'] as String,
      currency: json['currency'] as String? ?? 'INR',
    );
  }

  SearchAccount copyWith({
    int? id,
    String? name,
    String? currency,
  }) {
    return SearchAccount(
      id: id ?? this.id,
      name: name ?? this.name,
      currency: currency ?? this.currency,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'currency': currency,
    };
  }

  @override
  List<Object?> get props => [id, name, currency];

  @override
  String toString() {
    return 'SearchAccount{id: $id, name: $name, currency: $currency}';
  }
}
