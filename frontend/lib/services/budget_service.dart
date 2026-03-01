import 'package:tracko/models/budget_allocation_request.dart';
import 'package:tracko/models/budget_category.dart';
import 'package:tracko/models/budget_response.dart';
import 'package:tracko/services/api_client.dart';

class BudgetService {
  final ApiClient _apiClient;

  BudgetService({ApiClient? apiClient}) : _apiClient = apiClient ?? ApiClient();

  Future<BudgetResponse> getBudgetDetails(int month, int year) async {
    final Map<String, dynamic> data = await _apiClient.get(
      '/api/budget',
      query: {
        'month': month.toString(),
        'year': year.toString(),
        'includeActual': 'true',
      },
    );
    return BudgetResponse.fromJson(data);
  }

  Future<double> getAvailableToAssign(int month, int year) async {
    final response = await _apiClient.get(
      '/api/budget/available',
      query: {
        'month': month.toString(),
        'year': year.toString(),
      },
    );
    return (response as num).toDouble();
  }

  Future<BudgetCategory> allocateFunds(BudgetAllocationRequest request) async {
    final Map<String, dynamic> data = await _apiClient.post(
      '/api/budget/allocate',
      data: request.toJson(),
    );
    return BudgetCategory.fromJson(data);
  }
}
