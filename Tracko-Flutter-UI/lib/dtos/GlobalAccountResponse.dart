class GlobalAccountResponse {
  late String id;
  late String name;
  late String phoneNo;
  late String email;
  late String profilePic;
  late String fireBaseId;

  GlobalAccountResponse.fromJson(dynamic jsonResponse) {
    this.id = jsonResponse["id"];
    this.name = jsonResponse["name"];
    this.phoneNo = jsonResponse["phoneNo"];
    this.email = jsonResponse["email"];
    this.profilePic = jsonResponse["profilePic"];
    this.fireBaseId = jsonResponse["firebase_uuid"];
  }

  @override
  String toString() {
    return 'GlobalAccountResponse{id: $id, name: $name, phoneNo: $phoneNo, email: $email, profilePic: $profilePic, fireBaseId: $fireBaseId}';
  }


}
