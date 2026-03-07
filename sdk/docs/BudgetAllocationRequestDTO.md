# BudgetAllocationRequestDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**month** | **int** |  | 
**year** | **int** |  | 
**category_id** | **int** |  | 
**amount** | **float** |  | 

## Example

```python
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO

# TODO update the JSON string below
json = "{}"
# create an instance of BudgetAllocationRequestDTO from a JSON string
budget_allocation_request_dto_instance = BudgetAllocationRequestDTO.from_json(json)
# print the JSON string representation of the object
print(BudgetAllocationRequestDTO.to_json())

# convert the object into a dict
budget_allocation_request_dto_dict = budget_allocation_request_dto_instance.to_dict()
# create an instance of BudgetAllocationRequestDTO from a dict
budget_allocation_request_dto_form_dict = budget_allocation_request_dto.from_dict(budget_allocation_request_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


