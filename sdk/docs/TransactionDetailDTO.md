# TransactionDetailDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**transaction_type** | **int** |  | [optional] 
**name** | **str** |  | [optional] 
**comments** | **str** |  | [optional] 
**var_date** | **datetime** |  | [optional] 
**amount** | **float** |  | [optional] 
**original_currency** | **str** |  | [optional] 
**original_amount** | **float** |  | [optional] 
**exchange_rate** | **float** |  | [optional] 
**account_id** | **int** |  | [optional] 
**category_id** | **int** |  | [optional] 
**is_countable** | **int** |  | [optional] 
**category** | [**Category**](Category.md) |  | [optional] 
**account** | [**Account**](Account.md) |  | [optional] 
**splits** | [**List[SplitDetailDTO]**](SplitDetailDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_detail_dto import TransactionDetailDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionDetailDTO from a JSON string
transaction_detail_dto_instance = TransactionDetailDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionDetailDTO.to_json())

# convert the object into a dict
transaction_detail_dto_dict = transaction_detail_dto_instance.to_dict()
# create an instance of TransactionDetailDTO from a dict
transaction_detail_dto_form_dict = transaction_detail_dto.from_dict(transaction_detail_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


