class Contact {
  int? id;
  String name = '';
  String phoneNo = '';
  String email = '';

  Contact();

  static Contact fromJson(Map<String, dynamic> json) {
    final c = Contact();
    final idRaw = json['id'];
    if (idRaw is int) {
      c.id = idRaw;
    } else if (idRaw is num) {
      c.id = idRaw.toInt();
    } else if (idRaw is String) {
      c.id = int.tryParse(idRaw);
    }
    c.name = (json['name'] as String?) ?? '';
    c.phoneNo =
        (json['phoneNo'] as String?) ?? (json['phone_no'] as String?) ?? '';
    c.email = (json['email'] as String?) ?? '';
    return c;
  }

  @override
  String toString() =>
      'Contact{id: $id, name: $name, phoneNo: $phoneNo, email: $email}';
}
