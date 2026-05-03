# TransactionSearchHitDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**transaction** | [**TransactionDetailDTO**](TransactionDetailDTO.md) |  | [optional] 
**relevance_score** | **float** |  | [optional] 
**highlights** | **Dict[str, str]** |  | [optional] 
**matched_fields** | **List[str]** |  | [optional] 

## Example

```python
from tracko_sdk.models.transaction_search_hit_dto import TransactionSearchHitDTO

# TODO update the JSON string below
json = "{}"
# create an instance of TransactionSearchHitDTO from a JSON string
transaction_search_hit_dto_instance = TransactionSearchHitDTO.from_json(json)
# print the JSON string representation of the object
print(TransactionSearchHitDTO.to_json())

# convert the object into a dict
transaction_search_hit_dto_dict = transaction_search_hit_dto_instance.to_dict()
# create an instance of TransactionSearchHitDTO from a dict
transaction_search_hit_dto_form_dict = transaction_search_hit_dto.from_dict(transaction_search_hit_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


