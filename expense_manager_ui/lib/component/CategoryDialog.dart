import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';

// ignore: must_be_immutable
class CategoryDialog extends StatelessWidget {
  String name;
  Function callback;


  CategoryDialog({this.callback});

  addCategory() async {
    if(name.trim().length <= 0){
      return;
    }
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = CategoryBean(adapter);
    Category category = new Category();
    category.name = name;
    User user = await UserBean.getCurrentUser(adapter: adapter);
    categoryBean.associateUser(category, user);
    await categoryBean.insert(category);
    print(category);
    await adapter.close();
    if(callback != null){
      callback();
    }
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text("Add Category"),
      content: TextField(
        decoration: new InputDecoration(
          hintText: 'Name',
        ),
        onChanged: (text) {
          name = text;
        },
      ),
      actions: <Widget>[
        RaisedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          textColor: Colors.white,
          child: Text("Cancel"),
        ),
        RaisedButton(
          onPressed: () {
            this.addCategory();
            Navigator.pop(context);
          },
          textColor: Colors.white,
          child: Text("Add"),
        )
      ],
    );
  }
}
