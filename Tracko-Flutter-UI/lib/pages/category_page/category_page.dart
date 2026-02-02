import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/component/AsynLoadState.dart';
import 'package:tracko/component/CategoryDialog.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/screen.dart';
import 'package:tracko/controllers/CategoryController.dart';
import 'package:tracko/models/category.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class CategoryPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _category_page();
  }
}

class _category_page extends AsyncLoadState<CategoryPage> {
  List<Category> categories = [];

  @override
  asyncLoad() async {
    await initData();
    this.loadCompleteView();
    return null;
  }

  initData() async {
    categories = await CategoryController.getAllCategories();
  }

  void deleteDialog(int id) async {
    try {
      await CategoryController.deleteCategory(id);
      if (this.mounted)
        FlushDialog.flash(context, "Success", "Category Deleted.");
    } catch (e) {
      if (this.mounted) FlushDialog.flash(context, "Error", e.toString());
    }
    setState(() {
      initData();
    });
  }

  @override
  Widget completeWidget(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Categories"),
        centerTitle: true,
      ),
      body: ListView.builder(
        itemBuilder: (context, i) {
          return Slidable(
            // delegate: new SlidableScrollDelegate(), // Removed in flutter_slidable 3.x
            child: Card(
              margin: EdgeInsets.all(2),
              child: ListTile(
                contentPadding: EdgeInsets.all(8),
                leading: WidgetUtil.textAvatar(categories[i].name),
                title: Text(
                  categories[i].name,
                  style: WidgetUtil.defaultTextStyle(),
                ),
                onTap: () {
                  showDialog(
                    context: context,
                    builder: (_) => CategoryDialog(
                      category: categories[i],
                      callback: () {
                        setState(() {
                          initData();
                        });
                      },
                    ),
                  );
                },
                trailing: Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Icon(
                    Icons.edit,
                    size: 30,
                    color: Colors.blueAccent,
                  ),
                ),
              ),
            ),
            // TODO: Reimplement with SlidableAction for flutter_slidable 3.x
            endActionPane: ActionPane(
              motion: ScrollMotion(),
              children: [
                SlidableAction(
                  onPressed: (context) {
                    deleteDialog(categories[i].id ?? 0);
                  },
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                  icon: Icons.delete,
                  label: 'Delete',
                ),
              ],
            ),
          );
        },
        itemCount: categories.length,
      ),
      floatingActionButton: FloatingActionButton(
          child: Icon(Icons.add),
          onPressed: () {
            showDialog(
              context: context,
              builder: (_) => CategoryDialog(
                callback: () {
                  setState(() {
                    initData();
                  });
                },
              ),
            );
          }),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Screen(
      titleName: "Categories",
      body: Center(
        child: Text("No Categoies found."),
      ),
    );
  }
}
