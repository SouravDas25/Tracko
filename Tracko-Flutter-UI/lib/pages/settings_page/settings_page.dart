import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/DeleteDialog.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/component/PaddedText.dart';
import 'package:tracko/component/month_picker_dialog.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/pages/account_page/AccountPage.dart';
import 'package:tracko/pages/category_page/category_page.dart';
import 'package:tracko/pages/contact_page/contact_page.dart';
import 'package:tracko/pages/settings_page/currency_settings_page.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;

class SettingsPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SettingsPage();
  }
}

class _SettingsPage extends State<SettingsPage> {
  User? user;
  DateTime month = SettingUtil.currentMonth;

  @override
  void initState() {
    super.initState();
    initData();
  }

  void initData() async {
    try {
      user = await SessionService.getCurrentUser();
    } catch (e) {
      print("Error loading user: $e");
    }
    if (this.mounted) setState(() {});
  }

  void _showResetDatabaseDialog() {
    DeleteDialog.show(
        context: context,
        title: "Reset Database",
        message: "Are sure you want to delete all your transaction ?",
        deleteCallback: () async {
          await SessionService.logout();
          await _logout();
        });
  }

  Future<void> _logout() async {
    await SessionService.logout();
    Navigator.popAndPushNamed(context, "/welcome");
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Text(
        title,
        style: TextStyle(
          color: Theme.of(context).primaryColor,
          fontWeight: FontWeight.bold,
          fontSize: 14,
        ),
      ),
    );
  }

  Widget _buildSettingsTile({
    required IconData icon,
    required String title,
    required VoidCallback onTap,
    Widget? trailing,
    Color? iconColor,
  }) {
    final color = iconColor ?? Theme.of(context).primaryColor;
    return ListTile(
      leading: Container(
        padding: EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(icon, color: color),
      ),
      title: Text(title, style: TextStyle(fontSize: 16)),
      trailing: trailing ?? Icon(Icons.chevron_right, color: Colors.grey),
      onTap: onTap,
    );
  }

  @override
  Widget build(BuildContext context) {
    // Determine text color based on brightness for secondary text
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final secondaryTextColor = isDark ? Colors.grey[400] : Colors.grey[600];

    return Scaffold(
      appBar: AppBar(
        title: Text("Settings"),
        centerTitle: true,
        elevation: 0,
      ),
      body: ListView(
        children: <Widget>[
          SizedBox(height: 20),
          // User Profile Section
          Container(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30.0,
                  backgroundColor:
                      Theme.of(context).primaryColor.withOpacity(0.1),
                  child: user == null ||
                          user!.profilePic == null ||
                          user!.profilePic.isEmpty
                      ? Image.asset("assets/images/user-avatar.png")
                      : ClipOval(child: Image.network(user!.profilePic)),
                ),
                SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        user?.name ?? "Guest",
                        style: TextStyle(
                            fontSize: 20.0, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        user?.email ?? "",
                        style:
                            TextStyle(color: secondaryTextColor, fontSize: 14),
                      ),
                      Text(
                        user?.phoneNo ?? "",
                        style:
                            TextStyle(color: secondaryTextColor, fontSize: 14),
                      ),
                    ],
                  ),
                ),
                Text(
                  ConstantUtil.version,
                  style: TextStyle(color: secondaryTextColor, fontSize: 12),
                ),
              ],
            ),
          ),

          Divider(height: 32),

          _buildSectionHeader("DATA SETTINGS"),

          _buildSettingsTile(
            icon: Icons.category,
            title: "Categories",
            iconColor: Colors.orangeAccent,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CategoryPage()),
            ),
          ),
          _buildSettingsTile(
            icon: Icons.account_balance,
            title: "Accounts",
            iconColor: Colors.lightBlueAccent,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => AccountPage()),
            ),
          ),
          _buildSettingsTile(
            icon: Icons.contacts,
            title: "Contacts",
            iconColor: Colors.tealAccent,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => ContactPage()),
            ),
          ),
          _buildSettingsTile(
            icon: Icons.monetization_on,
            title: "Currency Settings",
            iconColor: Colors.greenAccent,
            onTap: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => CurrencySettingsPage()),
            ),
          ),

          _buildSectionHeader("SYSTEM SETTINGS"),

          _buildSettingsTile(
            icon: Icons.calendar_today,
            title: "Month - ${DateFormatter.DateFormat("MMMM").format(month)}",
            iconColor: Colors.pinkAccent,
            trailing: Icon(Icons.edit, size: 20, color: Colors.grey),
            onTap: () async {
              var m = await showMonthPicker(
                  context: context,
                  firstDate: DateTime(DateTime.now().year - 1, 5),
                  lastDate: DateTime(DateTime.now().year + 1, 9),
                  initialDate: month);
              if (m != null) {
                month = m;
                SettingUtil.setSelectedMonth(month);
                setState(() {});
              }
            },
          ),

          Divider(),

          ListTile(
            leading: Container(
              padding: EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.redAccent.withOpacity(0.1),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Icon(Icons.exit_to_app, color: Colors.redAccent),
            ),
            title: Text("Sign-out",
                style: TextStyle(fontSize: 16, color: Colors.redAccent)),
            onTap: _logout,
          ),
          SizedBox(height: 20),
        ],
      ),
    );
  }
}
