
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:http/http.dart';

class Entity {
  String category;
  String domain;
  String name;
  String entity_type;
  String logo;

  Future<Category> fetchOrNewCategory(var adapter) async {
    CategoryBean categoryBean = new CategoryBean(adapter);
    List<Category> categories = await categoryBean.getAll();
    Category category;
    for(Category cat in categories){
      if(cat.name.toLowerCase().compareTo(this.category.toLowerCase()) == 0) {
        category = cat;
      }
    }
    if(category == null) {
      category = new Category();
      category.name = this.category;
      category.userId = 1;
      category.id = await categoryBean.insert(category);
    }
    return category;
  }
  
  static Entity fromMap(dynamic jsonEntity){
    Entity entity = new Entity();
    entity.name = jsonEntity['name'];
    entity.category = jsonEntity['category'];
    entity.domain = jsonEntity['domain'];
    entity.entity_type = jsonEntity['entity_type'];
    entity.logo = jsonEntity['logo'];
    return entity;
  }

}

class PossibleTransaction {

  Category category;
  Account account;

  List<double> amounts;
  List<DateTime> dates;
  List<String>  comments;

  String type,name;
  bool valid;

  List<Entity> entities = new List();
  String smsText;

  String logo(){
    return this.entities.length > 0 ? this.entities[0].logo : "https://ui-avatars.com/api/?name=Item";
  }

  static resolveVariables(PossibleTransaction possibleTransaction) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    AccountBean accountBean = AccountBean(adapter);
    List<Account> accounts = await accountBean.getAll();
    possibleTransaction.account = accounts[0];
    if(possibleTransaction.entities.length > 0){
      Entity entity = possibleTransaction.entities[0];
      possibleTransaction.category = await entity.fetchOrNewCategory(adapter);
    }
    else possibleTransaction.category = await Category.defaultCategory(adapter);
//    print(possibleTransaction.category);
    await adapter.close();
  }

  static Future<PossibleTransaction> fromJson(dynamic jsonResponse) async {
    PossibleTransaction possibleTransaction = new PossibleTransaction();
    possibleTransaction.amounts = new List<double>.from(jsonResponse['amounts']);
    possibleTransaction.comments = new List<String>.from(jsonResponse['comments']);
    possibleTransaction.dates = new List<DateTime>();
    if(jsonResponse['dates'] != null){
      for(dynamic date in jsonResponse['dates']){
        possibleTransaction.dates.add(DateTime.parse(date.toString()));
      }
    }
    possibleTransaction.type = jsonResponse['type'];
    possibleTransaction.valid = jsonResponse['valid'];
    possibleTransaction.smsText = jsonResponse['request']['text'];

    if(jsonResponse['entity'] != null){
      for(dynamic entity in jsonResponse['entity']){
        possibleTransaction.entities.add(Entity.fromMap(entity));
      }
    }
    possibleTransaction.name = possibleTransaction.entities.length > 0 ? possibleTransaction.entities[0].name : "Item";

    await PossibleTransaction.resolveVariables(possibleTransaction);
    return possibleTransaction;
  }

  @override
  String toString() {
    return 'PossibleTransaction{amounts: $amounts, dates: $dates, comments: $comments, type: $type, valid: $valid}';
  }


}