import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/models/category.dart';
import 'package:Tracko/models/user.dart';
import 'package:Tracko/services/SessionService.dart';
import 'package:flutter/material.dart';

// ignore: must_be_immutable
class CategoryDialog extends StatelessWidget {
  TextEditingController _controller = TextEditingController();
  String name = '';
  Function callback;
  bool isEdit = false;
  Category? category;

  CategoryDialog({required this.callback, this.category}) {
    if (this.category != null) {
      _controller.text = this.category?.name ?? '';
      isEdit = true;
    } else
      this.category = new Category();
  }

  upsertCategory() async {
    this.name = _controller.text;
    if (name
        .trim()
        .length <= 0) {
      return;
    }
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    // TODO: Reimplement with raw SQL after jaguar_orm removal
    // CategoryBean categoryBean = CategoryBean(adapter);
    Category category = this.category ?? Category();
    category.name = name;
    User user = SessionService.currentUser();
    // categoryBean.associateUser(category, user);
    category.userId = user.id;
    // await categoryBean.upsert(category);
    // Stub: Insert or update category using raw SQL
    if (category.id == null) {
      await adapter.rawInsert('INSERT INTO categories (name, userId) VALUES (?, ?)', [category.name, category.userId]);
    } else {
      await adapter.rawUpdate('UPDATE categories SET name = ? WHERE id = ?', [category.name, category.id]);
    }
    print(category);
//    await adapter.close();
    callback();
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text(isEdit ? "Update Category" : "Add Category"),
      content: TextField(
        controller: _controller,
        decoration: new InputDecoration(
          hintText: 'Name',
        ),
//        onChanged: (text) {
//          name = text;
//        },
      ),
      actions: <Widget>[
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          // textColor: Colors.white, // Use style instead
          child: Text("Cancel"),
        ),
        ElevatedButton(
          onPressed: () {
            this.upsertCategory();
            Navigator.pop(context);
          },
          // textColor: Colors.white, // Use style instead
          child: Text(isEdit ? "Update" : "Add"),
        )
      ],
    );
  }
}
