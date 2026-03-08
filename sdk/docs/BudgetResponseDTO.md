# BudgetResponseDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**month** | **int** |  | [optional] 
**year** | **int** |  | [optional] 
**total_budget** | **float** |  | [optional] 
**total_income** | **float** |  | [optional] 
**total_spent** | **float** |  | [optional] 
**rollover_amount** | **float** |  | [optional] 
**available_to_assign** | **float** |  | [optional] 
**is_closed** | **bool** |  | [optional] 
**categories** | [**List[BudgetCategoryDTO]**](BudgetCategoryDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.budget_response_dto import BudgetResponseDTO

# TODO update the JSON string below
json = "{}"
# create an instance of BudgetResponseDTO from a JSON string
budget_response_dto_instance = BudgetResponseDTO.from_json(json)
# print the JSON string representation of the object
print(BudgetResponseDTO.to_json())

# convert the object into a dict
budget_response_dto_dict = budget_response_dto_instance.to_dict()
# create an instance of BudgetResponseDTO from a dict
budget_response_dto_form_dict = budget_response_dto.from_dict(budget_response_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


