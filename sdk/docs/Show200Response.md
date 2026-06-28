# Show200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**List[User]**](User.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.show200_response import Show200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Show200Response from a JSON string
show200_response_instance = Show200Response.from_json(json)
# print the JSON string representation of the object
print(Show200Response.to_json())

# convert the object into a dict
show200_response_dict = show200_response_instance.to_dict()
# create an instance of Show200Response from a dict
show200_response_form_dict = show200_response.from_dict(show200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


