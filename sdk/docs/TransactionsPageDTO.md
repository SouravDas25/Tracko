# TransactionsPageDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**month** | **int** |  | [optional] 
**year** | **int** |  | [optional] 
**page** | **int** |  | [optional] 
**size** | **int** |  | [optional] 
**total_elements** | **int** |  | [optional] 
**total_pages** | **int** |  | [optional] 
**has_next** | **bool** |  | [optional] 
**has_previous** | **bool** |  | [optional] 
**transactions** | [**List[TransactionDetailDTO]**](TransactionDetailDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.transactions_page_dto import TransactionsPageDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionsPageDTO from a JSON string
transactions_page_dto_instance = TransactionsPageDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionsPageDTO.to_json())

# convert the object into a dict
transactions_page_dto_dict = transactions_page_dto_instance.to_dict()
# create an instance of TransactionsPageDTO from a dict
transactions_page_dto_form_dict = transactions_page_dto.from_dict(transactions_page_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


