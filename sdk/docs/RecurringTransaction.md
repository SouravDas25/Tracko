# RecurringTransaction


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**user_id** | **str** |  | [optional] 
**name** | **str** |  | 
**amount** | **float** |  | [optional] 
**original_currency** | **str** |  | 
**original_amount** | **float** |  | 
**exchange_rate** | **float** |  | [optional] 
**account_id** | **int** |  | 
**category_id** | **int** |  | 
**to_account_id** | **int** |  | [optional] 
**transaction_type** | **str** |  | 
**frequency** | **str** |  | 
**start_date** | **datetime** |  | 
**next_run_date** | **datetime** |  | 
**end_date** | **datetime** |  | [optional] 
**is_active** | **bool** |  | [optional] 
**last_run_date** | **datetime** |  | [optional] 
**created_at** | **datetime** |  | [optional] 
**updated_at** | **datetime** |  | [optional] 

## Example

```python
from tracko_sdk.models.recurring_transaction import RecurringTransaction

# TODO update the JSON string below
json = "{}"
# create an instance of RecurringTransaction from a JSON string
recurring_transaction_instance = RecurringTransaction.from_json(json)
# print the JSON string representation of the object
print(RecurringTransaction.to_json())

# convert the object into a dict
recurring_transaction_dict = recurring_transaction_instance.to_dict()
# create an instance of RecurringTransaction from a dict
recurring_transaction_form_dict = recurring_transaction.from_dict(recurring_transaction_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


