import 'package:Tracko/Utils/CommonUtil.dart';
// import 'package:contacts_service/contacts_service.dart'; // TODO: Replace with AGP 8+ compatible alternative

class TrakoContact {
  late String name, phoneNo, email;

  TrakoContact();

  TrakoContact.fromContact(dynamic contact) {
    // TODO: Re-enable after replacing contacts_service with AGP 8+ compatible alternative
    this.name = contact.displayName ?? '';
    if (contact.phones != null && contact.phones.length > 0) {
      this.phoneNo = contact.phones?.first?.value ?? '';
      this.phoneNo = CommonUtil.extractPhoneNumber(this.phoneNo);
    }
    if (contact.emails != null && contact.emails.length > 0)
      this.email = contact.emails?.first?.value;
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is TrakoContact &&
          runtimeType == other.runtimeType &&
          phoneNo == other.phoneNo;

  @override
  int get hashCode => phoneNo.hashCode;
}
