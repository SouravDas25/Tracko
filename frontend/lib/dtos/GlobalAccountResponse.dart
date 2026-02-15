class GlobalAccountResponse {
  late String id;
  late String name;
  late String phoneNo;
  late String email;
  late String profilePic;
  late String baseCurrency;

  GlobalAccountResponse.fromJson(dynamic jsonResponse) {
    this.id = jsonResponse["id"];
    this.name = jsonResponse["name"];
    this.phoneNo = jsonResponse["phoneNo"];
    this.email = jsonResponse["email"];
    this.profilePic = jsonResponse["profilePic"];
    this.baseCurrency = jsonResponse["baseCurrency"] ?? "INR";
  }

  @override
  String toString() {
    return 'GlobalAccountResponse{id: $id, name: $name, phoneNo: $phoneNo, email: $email, profilePic: $profilePic, baseCurrency: $baseCurrency}';
  }
}
