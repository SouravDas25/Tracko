import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:Tracko/Utils/ConstantUtil.dart';
import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/component/DeleteDialog.dart';
import 'package:Tracko/component/LoadingDialog.dart';
import 'package:Tracko/component/PaddedText.dart';
import 'package:Tracko/component/month_picker_dialog.dart';
import 'package:Tracko/models/user.dart';
import 'package:Tracko/pages/account_page/AccountPage.dart';
import 'package:Tracko/pages/category_page/category_page.dart';
import 'package:Tracko/services/BackupService.dart';
import 'package:Tracko/services/SessionService.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;

class SettingsPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SettingsPage();
  }
}

class _SettingsPage extends State<SettingsPage> {
  late User user;
  DateTime month = SettingUtil.currentMonth;
  bool isAutoBackedEnabled = false;
  DateTime lastBackedUp = DateTime.now();

  _SettingsPage() {
    initData();
  }

  void initData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    user = SessionService.currentUser();
    isAutoBackedEnabled = await SettingUtil.isAutoBackUpEnabled();
    lastBackedUp = await SettingUtil.getLastBackedUpTime() ?? DateTime.now();
//    await adapter.close();
//    Future<void>.delayed(Duration(seconds: 1));
    if (this.mounted) setState(() {});
  }

  void _showLoaderForBackup() async {
    LoadingDialog.show(context);
    await BackupService.backupDatabase(user.phoneNo);
    LoadingDialog.hide(context);
  }

  void _showDeleteBackupDialog() {
    DeleteDialog.show(
        context: context,
        title: "Delete Backup",
        message: "Are sure you want to delete your backup forever ?",
        deleteCallback: () {
          BackupService.deleteBackUp(user.phoneNo);
        });
  }

  void _showResetDatabaseDialog() {
    DeleteDialog.show(
        context: context,
        title: "Reset Database",
        message: "Are sure you want to delete all your transaction ?",
        deleteCallback: () async {
          await DatabaseUtil.reset();
          await _logout();
        });
  }

  Future<void> _logout() async {
    await SessionService.logout();
    Navigator.popAndPushNamed(context, "/welcome");
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: ListView(children: <Widget>[
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: CircleAvatar(
              radius: 20.0,
              child: user == null ||
                  user.profilePic == null ||
                  user.profilePic.length <= 0
                  ? Image.asset("assets/images/user-avatar.png")
                  : Image.network(user.profilePic),
            ),
            title: Padding(
              padding: EdgeInsets.symmetric(vertical: 5.0),
              child: Text(user == null ? "" : user.name,
                  style: TextStyle(fontSize: 20.0)),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              textDirection: TextDirection.ltr,
              children: <Widget>[
                Text(user == null ? "" : user.phoneNo),
                Text(user == null ? "" : user.email),
              ],
            ),
            trailing: Text(ConstantUtil.version),
          ),
        ),
        PaddedText(
          "BACKUP SETTINGS",
          horizontal: 10.0,
          vertical: 10.0,
        ),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.backup, size: 30.0),
            title: Text("Auto Backup", style: TextStyle(fontSize: 20.0)),
            subtitle: Text(
                "Last backed up: ${lastBackedUp == null ? "Never" : CommonUtil
                    .humanDate(lastBackedUp)}"),
            trailing: Switch(
                value: isAutoBackedEnabled,
                onChanged: (val) {
                  SettingUtil.updateAutoBackup(val);
                }),
          ),
        ),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.file_upload, size: 30.0),
            title: Text("Backup Now", style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: _showLoaderForBackup,
          ),
        ),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.delete_forever, size: 30.0),
            title: Text("Delete Backup", style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: _showDeleteBackupDialog,
          ),
        ),
        PaddedText(
          "DATA SETTINGS",
          horizontal: 10.0,
          vertical: 10.0,
        ),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.category, size: 30.0),
            title: Text("Categories", style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: () {
//                Navigator.push(context, MaterialPageRoute())
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => CategoryPage(),
                ),
              );
            },
          ),
        ),
        Divider(height: 0),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.account_balance, size: 30.0),
            title: Text("Accounts", style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => AccountPage(),
                ),
              );
            },
          ),
        ),
        PaddedText(
          "SYSTEM SETTINGS",
          horizontal: 10.0,
          vertical: 10.0,
        ),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.calendar_today, size: 30.0),
            title: Text(
                "Month - ${DateFormatter.DateFormat("MMMM").format(month)}",
                style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: () async {
              var m = await showMonthPicker(
                  context: context,
                  firstDate: DateTime(DateTime
                      .now()
                      .year - 1, 5),
                  lastDate: DateTime(DateTime
                      .now()
                      .year + 1, 9),
                  initialDate: month);
              if (m != null) {
                month = m;
                SettingUtil.setSelectedMonth(month);
              }
              setState(() {});
            },
          ),
        ),
        Divider(height: 0),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.dns, size: 30.0),
            title: Text("Reset All", style: TextStyle(fontSize: 20.0)),
            trailing: Icon(Icons.arrow_forward_ios),
            onTap: _showResetDatabaseDialog,
          ),
        ),
        Divider(height: 0),
        Card(
          margin: EdgeInsets.all(0),
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.exit_to_app, size: 30.0),
            title: Text("Sign-out", style: TextStyle(fontSize: 20.0)),
            onTap: _logout,
            trailing: Icon(Icons.arrow_forward_ios),
          ),
        ),
      ]),
    );
  }
}
