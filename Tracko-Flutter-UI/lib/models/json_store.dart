class JsonStoreModel {
  final String name;
  final String value; // stored JSON/text

  JsonStoreModel({required this.name, required this.value});

  factory JsonStoreModel.fromJson(Map<String, dynamic> json) {
    // Backend uses json_value as column name
    final v = (json['json_value'] ?? json['value'] ?? '') as String;
    return JsonStoreModel(
      name: json['name'] as String,
      value: v,
    );
  }

  Map<String, dynamic> toJson() => {
    'name': name,
    // Send as json_value to match backend
    'json_value': value,
  };
}
