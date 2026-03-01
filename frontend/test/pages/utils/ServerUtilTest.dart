import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/models/user.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('ServerUtil', () {
    User user = User.make(1, "TestUser", "1234567890", "TestUser@trako.com");

    test('createGlobalAccount', () async {
      String globalId = await ServerUtil.createGlobalAccount(user);
      user.globalId = globalId;
      print(globalId);
      assert(globalId != null);
    });

    test('getGlobalAccount', () async {
      GlobalAccountResponse globalAccountResponse =
          await ServerUtil.getGlobalAccount("1234567890");
      print(globalAccountResponse);
      assert(globalAccountResponse != null);
      assert(globalAccountResponse.id != null);
      assert(globalAccountResponse.name != null);
      assert(globalAccountResponse.name == "TestUser");
      assert(globalAccountResponse.email != null);
      assert(globalAccountResponse.email == "TestUser@trako.com");
      assert(globalAccountResponse.phoneNo != null);
      assert(globalAccountResponse.phoneNo == "1234567890");
    });

    test('updateGlobalUser', () async {
      user.email = "TestUser@trako.co.in";
      bool isSuccessful = await ServerUtil.updateGlobalUser(user);
      print(isSuccessful);
      assert(isSuccessful == true);

      GlobalAccountResponse globalAccountResponse =
          await ServerUtil.getGlobalAccount("1234567890");
      print(globalAccountResponse);
      assert(globalAccountResponse != null);
      assert(globalAccountResponse.id != null);
      assert(globalAccountResponse.name != null);
      assert(globalAccountResponse.name == "TestUser");
      assert(globalAccountResponse.email != null);
      assert(globalAccountResponse.email == "TestUser@trako.co.in");
      assert(globalAccountResponse.phoneNo != null);
      assert(globalAccountResponse.phoneNo == "1234567890");
    });
  });
}
