# Delete200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | **str** |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.delete200_response import Delete200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Delete200Response from a JSON string
delete200_response_instance = Delete200Response.from_json(json)
# print the JSON string representation of the object
print(Delete200Response.to_json())

# convert the object into a dict
delete200_response_dict = delete200_response_instance.to_dict()
# create an instance of Delete200Response from a dict
delete200_response_form_dict = delete200_response.from_dict(delete200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


