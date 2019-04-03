package io.flutter.plugins;

import io.flutter.plugin.common.PluginRegistry;
import flutter.plugins.contactsservice.contactsservice.ContactsServicePlugin;
import io.flutter.plugins.pathprovider.PathProviderPlugin;
import com.baseflow.permissionhandler.PermissionHandlerPlugin;
import com.zt.shareextend.ShareExtendPlugin;
import io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin;
import com.babariviere.sms.SmsPlugin;
import com.tekartik.sqflite.SqflitePlugin;

/**
 * Generated file. Do not edit.
 */
public final class GeneratedPluginRegistrant {
  public static void registerWith(PluginRegistry registry) {
    if (alreadyRegisteredWith(registry)) {
      return;
    }
    ContactsServicePlugin.registerWith(registry.registrarFor("flutter.plugins.contactsservice.contactsservice.ContactsServicePlugin"));
    PathProviderPlugin.registerWith(registry.registrarFor("io.flutter.plugins.pathprovider.PathProviderPlugin"));
    PermissionHandlerPlugin.registerWith(registry.registrarFor("com.baseflow.permissionhandler.PermissionHandlerPlugin"));
    ShareExtendPlugin.registerWith(registry.registrarFor("com.zt.shareextend.ShareExtendPlugin"));
    SharedPreferencesPlugin.registerWith(registry.registrarFor("io.flutter.plugins.sharedpreferences.SharedPreferencesPlugin"));
    SmsPlugin.registerWith(registry.registrarFor("com.babariviere.sms.SmsPlugin"));
    SqflitePlugin.registerWith(registry.registrarFor("com.tekartik.sqflite.SqflitePlugin"));
  }

  private static boolean alreadyRegisteredWith(PluginRegistry registry) {
    final String key = GeneratedPluginRegistrant.class.getCanonicalName();
    if (registry.hasPlugin(key)) {
      return true;
    }
    registry.registrarFor(key);
    return false;
  }
}
