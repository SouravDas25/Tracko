# Create2200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**Split**](Split.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.create2200_response import Create2200Response

# TODO update the JSON string below
json = "{}"
# create an instance of Create2200Response from a JSON string
create2200_response_instance = Create2200Response.from_json(json)
# print the JSON string representation of the object
print(Create2200Response.to_json())

# convert the object into a dict
create2200_response_dict = create2200_response_instance.to_dict()
# create an instance of Create2200Response from a dict
create2200_response_form_dict = create2200_response.from_dict(create2200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


