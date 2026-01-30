import 'package:Tracko/Utils/DatabaseUtil.dart';
// import 'package:Tracko/services/BackupService.dart'; // TODO: Migrate to new Firebase Storage API
import 'package:google_mobile_ads/google_mobile_ads.dart';

import 'Utils/AdsUtil.dart';

class InitializeApp {
  static init() async {
    // TODO: Re-enable BackupService after migrating to new Firebase Storage API
    // await BackupService.isRestoreRequired();

    MobileAds.instance.initialize();

    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
//     await DatabaseUtil.dropTables(adapter);

    await DatabaseUtil.seedTables(adapter);

//    await DatabaseUtil.clearTransaction();
//    await DatabaseUtil.seedTransaction();
  }
}
