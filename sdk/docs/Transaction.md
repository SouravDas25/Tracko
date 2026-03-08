# Transaction


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**name** | **str** |  | 
**comments** | **str** |  | [optional] 
**var_date** | **datetime** |  | 
**amount** | **float** |  | [optional] 
**original_currency** | **str** |  | 
**original_amount** | **float** |  | 
**exchange_rate** | **float** |  | 
**account_id** | **int** |  | 
**category_id** | **int** |  | 
**is_countable** | **int** |  | [optional] 
**linked_transaction_id** | **int** |  | [optional] 
**transaction_type** | **int** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction import Transaction

# TODO update the JSON string below
json = "{}"
# create an instance of Transaction from a JSON string
transaction_instance = Transaction.from_json(json)
# print the JSON string representation of the object
print(Transaction.to_json())

# convert the object into a dict
transaction_dict = transaction_instance.to_dict()
# create an instance of Transaction from a dict
transaction_form_dict = transaction.from_dict(transaction_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


