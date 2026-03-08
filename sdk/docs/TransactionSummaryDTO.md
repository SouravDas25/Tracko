# TransactionSummaryDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**total_income** | **float** |  | [optional] 
**total_expense** | **float** |  | [optional] 
**net_total** | **float** |  | [optional] 
**rollover_net** | **float** |  | [optional] 
**net_total_with_rollover** | **float** |  | [optional] 
**transaction_count** | **int** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_summary_dto import TransactionSummaryDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionSummaryDTO from a JSON string
transaction_summary_dto_instance = TransactionSummaryDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionSummaryDTO.to_json())

# convert the object into a dict
transaction_summary_dto_dict = transaction_summary_dto_instance.to_dict()
# create an instance of TransactionSummaryDTO from a dict
transaction_summary_dto_form_dict = transaction_summary_dto.from_dict(transaction_summary_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


