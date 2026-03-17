# ShowByPhone200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**List[User]**](User.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.show_by_phone200_response import ShowByPhone200Response

# TODO update the JSON string below
json = "{}"
# create an instance of ShowByPhone200Response from a JSON string
show_by_phone200_response_instance = ShowByPhone200Response.from_json(json)
# print the JSON string representation of the object
print(ShowByPhone200Response.to_json())

# convert the object into a dict
show_by_phone200_response_dict = show_by_phone200_response_instance.to_dict()
# create an instance of ShowByPhone200Response from a dict
show_by_phone200_response_form_dict = show_by_phone200_response.from_dict(show_by_phone200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


