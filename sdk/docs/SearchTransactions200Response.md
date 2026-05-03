# SearchTransactions200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**TransactionSearchResultDTO**](TransactionSearchResultDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.search_transactions200_response import SearchTransactions200Response

# TODO update the JSON string below
json = "{}"
# create an instance of SearchTransactions200Response from a JSON string
search_transactions200_response_instance = SearchTransactions200Response.from_json(json)
# print the JSON string representation of the object
print(SearchTransactions200Response.to_json())

# convert the object into a dict
search_transactions200_response_dict = search_transactions200_response_instance.to_dict()
# create an instance of SearchTransactions200Response from a dict
search_transactions200_response_form_dict = search_transactions200_response.from_dict(search_transactions200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


