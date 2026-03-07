# TransactionRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**account_id** | **int** |  | [optional] 
**var_date** | **datetime** |  | [optional] 
**name** | **str** |  | [optional] 
**comments** | **str** |  | [optional] 
**category_id** | **int** |  | [optional] 
**transaction_type** | **str** |  | [optional] 
**is_countable** | **int** |  | [optional] 
**original_currency** | **str** |  | [optional] 
**original_amount** | **float** |  | [optional] 
**exchange_rate** | **float** |  | [optional] 
**linked_transaction_id** | **int** |  | [optional] 
**to_account_id** | **int** |  | [optional] 
**from_account_id** | **int** |  | [optional] 
**transfer** | **bool** |  | [optional] 
**source_account_id** | **int** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_request import TransactionRequest

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionRequest from a JSON string
transaction_request_instance = TransactionRequest.from_json(json)
# print the JSON string representation of the object
print(TransactionRequest.to_json())

# convert the object into a dict
transaction_request_dict = transaction_request_instance.to_dict()
# create an instance of TransactionRequest from a dict
transaction_request_form_dict = transaction_request.from_dict(transaction_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


