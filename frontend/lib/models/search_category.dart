import 'package:equatable/equatable.dart';

/// Represents a category in search results.
/// 
/// Validates: Requirements 7.3, 7.4
class SearchCategory extends Equatable {
  final int id;
  final String name;
  final String categoryType;

  const SearchCategory({
    required this.id,
    required this.name,
    required this.categoryType,
  });

  factory SearchCategory.fromJson(Map<String, dynamic> json) {
    return SearchCategory(
      id: json['id'] as int,
      name: json['name'] as String,
      categoryType: json['categoryType'] as String? ?? 'EXPENSE',
    );
  }

  SearchCategory copyWith({
    int? id,
    String? name,
    String? categoryType,
  }) {
    return SearchCategory(
      id: id ?? this.id,
      name: name ?? this.name,
      categoryType: categoryType ?? this.categoryType,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'categoryType': categoryType,
    };
  }

  @override
  List<Object?> get props => [id, name, categoryType];

  @override
  String toString() {
    return 'SearchCategory{id: $id, name: $name, categoryType: $categoryType}';
  }
}
