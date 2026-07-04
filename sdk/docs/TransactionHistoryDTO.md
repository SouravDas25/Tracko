# TransactionHistoryDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**transaction_id** | **int** |  | [optional] 
**operation** | **str** |  | [optional] 
**changed_at** | **datetime** |  | [optional] 
**linked_transaction_id** | **int** |  | [optional] 
**name** | **str** |  | [optional] 
**amount** | **float** |  | [optional] 
**original_currency** | **str** |  | [optional] 
**var_date** | **datetime** |  | [optional] 
**transaction_type** | **int** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_history_dto import TransactionHistoryDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionHistoryDTO from a JSON string
transaction_history_dto_instance = TransactionHistoryDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionHistoryDTO.to_json())

# convert the object into a dict
transaction_history_dto_dict = transaction_history_dto_instance.to_dict()
# create an instance of TransactionHistoryDTO from a dict
transaction_history_dto_form_dict = transaction_history_dto.from_dict(transaction_history_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


