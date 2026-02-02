import 'package:tracko/config/api_config.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/services/api_client.dart';

class ContactRepository {
  final _api = ApiClient();

  Future<List<Contact>> listMine() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.contacts);
    return res.map((e) => Contact.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Contact?> getById(int id) async {
    final res =
        await _api.get<Map<String, dynamic>>("${ApiConfig.contacts}/$id");
    return Contact.fromJson(res);
  }

  Future<Contact> create(Contact contact) async {
    final res = await _api.post<Map<String, dynamic>>(
      ApiConfig.contacts,
      data: {
        'name': contact.name,
        'phoneNo': contact.phoneNo,
        'email': contact.email,
      },
    );
    return Contact.fromJson(res);
  }

  Future<Contact> update(int id, Contact contact) async {
    final res = await _api.put<Map<String, dynamic>>(
      "${ApiConfig.contacts}/$id",
      data: {
        'name': contact.name,
        'phoneNo': contact.phoneNo,
        'email': contact.email,
      },
    );
    return Contact.fromJson(res);
  }

  Future<void> delete(int id) async {
    await _api.delete<void>("${ApiConfig.contacts}/$id");
  }
}
