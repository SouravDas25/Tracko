# BudgetCategoryDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**category_id** | **int** |  | [optional] 
**category_name** | **str** |  | [optional] 
**allocated_amount** | **float** |  | [optional] 
**actual_spent** | **float** |  | [optional] 
**remaining_balance** | **float** |  | [optional] 

## Example

```python
from tracko_sdk.models.budget_category_dto import BudgetCategoryDTO

# TODO update the JSON string below
json = "{}"
# create an instance of BudgetCategoryDTO from a JSON string
budget_category_dto_instance = BudgetCategoryDTO.from_json(json)
# print the JSON string representation of the object
print(BudgetCategoryDTO.to_json())

# convert the object into a dict
budget_category_dto_dict = budget_category_dto_instance.to_dict()
# create an instance of BudgetCategoryDTO from a dict
budget_category_dto_form_dict = budget_category_dto.from_dict(budget_category_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


