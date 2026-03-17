# GetByName200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**JsonStore**](JsonStore.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_by_name200_response import GetByName200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetByName200Response from a JSON string
get_by_name200_response_instance = GetByName200Response.from_json(json)
# print the JSON string representation of the object
print(GetByName200Response.to_json())

# convert the object into a dict
get_by_name200_response_dict = get_by_name200_response_instance.to_dict()
# create an instance of GetByName200Response from a dict
get_by_name200_response_form_dict = get_by_name200_response.from_dict(get_by_name200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


