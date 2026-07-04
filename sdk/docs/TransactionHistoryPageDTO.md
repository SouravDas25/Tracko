# TransactionHistoryPageDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**page** | **int** |  | [optional] 
**size** | **int** |  | [optional] 
**total_elements** | **int** |  | [optional] 
**total_pages** | **int** |  | [optional] 
**has_next** | **bool** |  | [optional] 
**has_previous** | **bool** |  | [optional] 
**history** | [**List[TransactionHistoryDTO]**](TransactionHistoryDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_history_page_dto import TransactionHistoryPageDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionHistoryPageDTO from a JSON string
transaction_history_page_dto_instance = TransactionHistoryPageDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionHistoryPageDTO.to_json())

# convert the object into a dict
transaction_history_page_dto_dict = transaction_history_page_dto_instance.to_dict()
# create an instance of TransactionHistoryPageDTO from a dict
transaction_history_page_dto_form_dict = transaction_history_page_dto.from_dict(transaction_history_page_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


