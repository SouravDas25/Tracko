# AuthicationRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**phone_no** | **str** |  | 
**password** | **str** |  | 

## Example

```python
from tracko_sdk.models.authication_request import AuthicationRequest

# TODO update the JSON string below
json = "{}"
# create an instance of AuthicationRequest from a JSON string
authication_request_instance = AuthicationRequest.from_json(json)
# print the JSON string representation of the object
print(AuthicationRequest.to_json())

# convert the object into a dict
authication_request_dict = authication_request_instance.to_dict()
# create an instance of AuthicationRequest from a dict
authication_request_form_dict = authication_request.from_dict(authication_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


