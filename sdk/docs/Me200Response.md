# Me200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**User**](User.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.me200_response import Me200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Me200Response from a JSON string
me200_response_instance = Me200Response.from_json(json)
# print the JSON string representation of the object
print(Me200Response.to_json())

# convert the object into a dict
me200_response_dict = me200_response_instance.to_dict()
# create an instance of Me200Response from a dict
me200_response_form_dict = me200_response.from_dict(me200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


