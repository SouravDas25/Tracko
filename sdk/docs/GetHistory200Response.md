# GetHistory200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**List[TransactionHistoryDTO]**](TransactionHistoryDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_history200_response import GetHistory200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetHistory200Response from a JSON string
get_history200_response_instance = GetHistory200Response.from_json(json)
# print the JSON string representation of the object
print(GetHistory200Response.to_json())

# convert the object into a dict
get_history200_response_dict = get_history200_response_instance.to_dict()
# create an instance of GetHistory200Response from a dict
get_history200_response_form_dict = get_history200_response.from_dict(get_history200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


