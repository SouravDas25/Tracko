import 'package:tracko/models/category.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/category_repository.dart';
import 'package:tracko/services/SessionService.dart';
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
    if (name.trim().length <= 0) {
      return;
    }
    Category category = this.category ?? Category();
    category.name = name;
    User user = SessionService.currentUser();
    category.userId = user.id;
    final repo = CategoryRepository();
    if (category.id == null) {
      final created = await repo.create(category.name);
      category.id = created.id;
    } else {
      await repo.update(category.id!, category.name);
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
