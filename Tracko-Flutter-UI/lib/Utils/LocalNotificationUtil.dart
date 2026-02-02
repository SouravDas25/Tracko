import 'package:flutter_local_notifications/flutter_local_notifications.dart';

class LocalNotificationUtil {
  late FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin;
  var androidPlatformChannelSpecifics = new AndroidNotificationDetails(
      'your channel id', 'your channel name',
      channelDescription: 'your channel description',
      importance: Importance.max,
      priority: Priority.high);
  var iOSPlatformChannelSpecifics = new DarwinNotificationDetails();

  Future<void> show(int id, String title, String body,
      {String? payload, NotificationDetails? notificationDetails}) async {
    var platformChannelSpecifics = new NotificationDetails(
        android: androidPlatformChannelSpecifics,
        iOS: iOSPlatformChannelSpecifics);
    if (notificationDetails == null)
      notificationDetails = platformChannelSpecifics;
    await flutterLocalNotificationsPlugin
        .show(id, title, body, notificationDetails, payload: payload);
  }

  Future<void> schedule(int id, String title, String body, Duration duration,
      {String? payload, NotificationDetails? notificationDetails}) async {
    var platformChannelSpecifics = new NotificationDetails(
        android: androidPlatformChannelSpecifics,
        iOS: iOSPlatformChannelSpecifics);

    if (notificationDetails == null)
      notificationDetails = platformChannelSpecifics;
    // Note: schedule() is deprecated, use zonedSchedule() instead
    // await flutterLocalNotificationsPlugin.schedule(
    //     id, title, body, DateTime.now().add(duration), notificationDetails,
    //     payload: payload);
  }

  static Future onSelectNotification(String payload) async {
    print("returned from local notification");
  }

  FlutterLocalNotificationsPlugin initialize({onSelect}) {
    var initializationSettingsAndroid =
        new AndroidInitializationSettings('app_icon');
    var initializationSettingsIOS = new DarwinInitializationSettings();
    var initializationSettings = new InitializationSettings(
        android: initializationSettingsAndroid, iOS: initializationSettingsIOS);
    flutterLocalNotificationsPlugin = new FlutterLocalNotificationsPlugin();
    flutterLocalNotificationsPlugin.initialize(initializationSettings,
        onDidReceiveNotificationResponse: (NotificationResponse response) {
      if (onSelect != null) onSelect(response.payload);
    });
    return flutterLocalNotificationsPlugin;
  }
}
