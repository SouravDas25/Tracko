# Delete1200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | **str** |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.delete1200_response import Delete1200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Delete1200Response from a JSON string
delete1200_response_instance = Delete1200Response.from_json(json)
# print the JSON string representation of the object
print(Delete1200Response.to_json())

# convert the object into a dict
delete1200_response_dict = delete1200_response_instance.to_dict()
# create an instance of Delete1200Response from a dict
delete1200_response_form_dict = delete1200_response.from_dict(delete1200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


