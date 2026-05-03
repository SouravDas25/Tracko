# TransactionSearchResultDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**results** | [**List[TransactionSearchHitDTO]**](TransactionSearchHitDTO.md) |  | [optional] 
**total_results** | **int** |  | [optional] 
**page** | **int** |  | [optional] 
**size** | **int** |  | [optional] 
**total_pages** | **int** |  | [optional] 
**has_next** | **bool** |  | [optional] 
**has_previous** | **bool** |  | [optional] 
**search_time_ms** | **int** |  | [optional] 
**query** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_search_result_dto import TransactionSearchResultDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionSearchResultDTO from a JSON string
transaction_search_result_dto_instance = TransactionSearchResultDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionSearchResultDTO.to_json())

# convert the object into a dict
transaction_search_result_dto_dict = transaction_search_result_dto_instance.to_dict()
# create an instance of TransactionSearchResultDTO from a dict
transaction_search_result_dto_form_dict = transaction_search_result_dto.from_dict(transaction_search_result_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


