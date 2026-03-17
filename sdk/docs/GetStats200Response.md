# GetStats200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**StatsResponseDTO**](StatsResponseDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_stats200_response import GetStats200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetStats200Response from a JSON string
get_stats200_response_instance = GetStats200Response.from_json(json)
# print the JSON string representation of the object
print(GetStats200Response.to_json())

# convert the object into a dict
get_stats200_response_dict = get_stats200_response_instance.to_dict()
# create an instance of GetStats200Response from a dict
get_stats200_response_form_dict = get_stats200_response.from_dict(get_stats200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


