import 'package:Tracko/Utils/AdsUtil.dart';
import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/component/HomePieChart.dart';
import 'package:Tracko/component/PaddedText.dart';
import 'package:Tracko/component/TransactionTile.dart';
import 'package:Tracko/component/interfaces.dart';
import 'package:Tracko/controllers/CategoryController.dart';
import 'package:Tracko/controllers/TransactionController.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/scratch/ChartUtil.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;
import "package:pull_to_refresh/pull_to_refresh.dart";

class HomePage extends StatefulWidget {
  HomePage({Key? key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends RefreshableState<HomePage>
    with SingleTickerProviderStateMixin {
  List<Transaction> transactions = [];
  bool refreshIndicator = true;
  RefreshController refreshController = RefreshController();
  double totalAmount = 0.0;
  double previousTotal = 0.0;
  List<ChartEntry>? seriesList;
  BannerAd? _bannerAd;

  @override
  void initState() {
    super.initState();
    _loadBannerAd();
  }

  void _loadBannerAd() {
    _bannerAd = BannerAd(
      adUnitId: AdsUtil.getBannerAdUnitId(),
      size: AdsUtil.getBannerSize(),
      request: AdRequest(),
      listener: BannerAdListener(
        onAdLoaded: (ad) {
          // Ad loaded successfully
        },
        onAdFailedToLoad: (ad, error) {
          ad.dispose();
        },
      ),
    )..load();
  }

  @override
  asyncLoad() async {
    await initData();
    this.loadCompleteView();
  }

  initData() async {
//    await Future.delayed(Duration(seconds: 3));
    transactions = await TransactionController.getRecentTransaction();
    totalAmount = await TransactionController.getCurrentMonthTotal();
    seriesList = await CategoryController.getPieChartData();
    previousTotal = await TransactionController.getPreviousMonthTotal();
    print("$totalAmount");
    totalAmount = totalAmount + previousTotal;
    if (this.mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _bannerAd?.dispose();
    super.dispose();
    refreshController.dispose();
  }

  @override
  void refresh() async {
//    refreshController.sendBack(true, RefreshStatus.refreshing);
    await initData();
    if (this.mounted)
      setState(() {
        refreshController.refreshCompleted();
      });
  }

  @override
  Widget completeWidget(BuildContext context) {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () {
        refresh();
      },
      child: ListView(
        children: <Widget>[
          Card(
            child: Column(
              children: <Widget>[
                SizedBox(
                  height: 20,
                ),
                Text(
                  "${DateFormatter.DateFormat("MMM").format(
                      SettingUtil.previousMonth)} : ${CommonUtil.toCurrency(
                      previousTotal)}",
                  style: TextStyle(fontSize: 15),
                ),
                PaddedText(
                  CommonUtil.toCurrency(totalAmount),
                  vertical: 15.0,
                  horizontal: 10.0,
                  style: TextStyle(fontSize: 33, fontWeight: FontWeight.w600),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
          Card(
            child: CategoryChart(seriesList ?? []),
          ),
          PaddedText(
            "RECENT TRANSACTION",
            horizontal: 10.0,
            vertical: 10.0,
          ),
          ListView.builder(
              primary: false,
              shrinkWrap: true,
              itemCount: transactions.length,
              itemBuilder: (BuildContext context, int index) {
                Transaction transaction = transactions[index];
                if (index != 0 && index % 3 == 0 && _bannerAd != null) {
                  return Column(
                    children: <Widget>[
                      Container(
                        margin: EdgeInsets.only(bottom: 5.0),
                        height: 50,
                        child: AdWidget(ad: _bannerAd!),
                      ),
                      TransactionTile(
                          this,
                          transaction,
                              (dynamic parent, Transaction transaction) =>
                              parent.refresh()),
                    ],
                  );
                }
                return TransactionTile(
                    this,
                    transaction,
                        (dynamic parent, Transaction transaction) =>
                        parent.refresh());
              }),
        ],
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Data Available."),
    );
  }
}
