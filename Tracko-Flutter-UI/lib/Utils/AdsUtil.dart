import 'dart:io';

import 'package:Tracko/Utils/WidgetUtil.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';

class AdsUtil {
  static String getAppId() {
    if (Platform.isIOS) {
      return '';
    } else if (Platform.isAndroid) {
      return 'ca-app-pub-9374066862084503~4491267401';
    }
    return '';
  }

  static String getBannerAdUnitId() {
    if (Platform.isIOS) {
      return '';
    } else if (Platform.isAndroid) {
      return 'ca-app-pub-9374066862084503/1568277515';
    }
    return '';
  }

  static String getInterstitialAdUnitId() {
    if (Platform.isIOS) {
      return '';
    } else if (Platform.isAndroid) {
      return '';
    }
    return '';
  }

  static String getRewardBasedVideoAdUnitId() {
    if (Platform.isIOS) {
      return '';
    } else if (Platform.isAndroid) {
      return '';
    }
    return '';
  }

  static AdSize getBannerSize() {
    return AdSize.banner;
  }

  // AdEvent handling is now done via callbacks in google_mobile_ads
  // Use BannerAdListener, InterstitialAdLoadCallback, etc. instead
}
