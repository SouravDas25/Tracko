





import 'package:jaguar_orm/jaguar_orm.dart';

part 'setting.jorm.dart';

class Setting {
  Setting();


  Setting.make(this.id, this.key, this.value);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 128)
  String key;

  @Column(isNullable: false , length: 512)
  String value;

  @override
  String toString() {
    return 'Setting{id: $id, key: $key, value: $value}';
  }


}

@GenBean()
class SettingBean extends Bean<Setting> with _SettingBean {
  SettingBean(Adapter adapter) : super(adapter);

  final String tableName = 'settings';
}