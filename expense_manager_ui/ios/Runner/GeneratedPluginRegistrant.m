//
//  Generated file. Do not edit.
//

#import "GeneratedPluginRegistrant.h"
#import <contacts_service/ContactsServicePlugin.h>
#import <path_provider/PathProviderPlugin.h>
#import <permission_handler/PermissionHandlerPlugin.h>
#import <share_extend/ShareExtendPlugin.h>
#import <shared_preferences/SharedPreferencesPlugin.h>
#import <sms/SmsPlugin.h>
#import <sqflite/SqflitePlugin.h>

@implementation GeneratedPluginRegistrant

+ (void)registerWithRegistry:(NSObject<FlutterPluginRegistry>*)registry {
  [ContactsServicePlugin registerWithRegistrar:[registry registrarForPlugin:@"ContactsServicePlugin"]];
  [FLTPathProviderPlugin registerWithRegistrar:[registry registrarForPlugin:@"FLTPathProviderPlugin"]];
  [PermissionHandlerPlugin registerWithRegistrar:[registry registrarForPlugin:@"PermissionHandlerPlugin"]];
  [FLTShareExtendPlugin registerWithRegistrar:[registry registrarForPlugin:@"FLTShareExtendPlugin"]];
  [FLTSharedPreferencesPlugin registerWithRegistrar:[registry registrarForPlugin:@"FLTSharedPreferencesPlugin"]];
  [SmsPlugin registerWithRegistrar:[registry registrarForPlugin:@"SmsPlugin"]];
  [SqflitePlugin registerWithRegistrar:[registry registrarForPlugin:@"SqflitePlugin"]];
}

@end
