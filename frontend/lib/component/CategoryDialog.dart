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
  final String? categoryType;
  String _selectedCategoryType = 'EXPENSE';

  CategoryDialog({required this.callback, this.category, this.categoryType}) {
    if (this.category != null) {
      _controller.text = this.category?.name ?? '';
      isEdit = true;
    } else
      this.category = new Category();

    _selectedCategoryType =
        (this.categoryType != null && this.categoryType!.trim().isNotEmpty)
            ? this.categoryType!.trim().toUpperCase()
            : ((this.category?.categoryType ?? 'EXPENSE').trim().toUpperCase());
  }

  upsertCategory() async {
    this.name = _controller.text;
    if (name.trim().length <= 0) {
      return;
    }
    Category category = this.category ?? Category();
    category.name = name;
    category.categoryType = _selectedCategoryType.trim().toUpperCase();
    User user = SessionService.currentUser();
    category.userId = user.id;
    final repo = CategoryRepository();
    if (category.id == null) {
      final created =
          await repo.create(category.name, categoryType: category.categoryType);
      category.id = created.id;
    } else {
      await repo.update(category.id!, category.name,
          categoryType: category.categoryType);
    }
    print(category);
    callback();
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text(isEdit ? "Update Category" : "Add Category"),
      content: StatefulBuilder(
        builder: (context, setState) {
          return Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: _controller,
                decoration: new InputDecoration(
                  hintText: 'Name',
                ),
              ),
              SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: _selectedCategoryType,
                decoration: InputDecoration(
                  labelText: 'Type',
                ),
                items: const [
                  DropdownMenuItem(
                    value: 'EXPENSE',
                    child: Text('Expense'),
                  ),
                  DropdownMenuItem(
                    value: 'INCOME',
                    child: Text('Income'),
                  ),
                ],
                onChanged: (val) {
                  if (val == null) return;
                  setState(() {
                    _selectedCategoryType = val;
                  });
                },
              ),
            ],
          );
        },
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
