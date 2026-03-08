# JwtResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**token** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.jwt_response import JwtResponse

# TODO update the JSON string below
json = "{}"
# create an instance of JwtResponse from a JSON string
jwt_response_instance = JwtResponse.from_json(json)
# print the JSON string representation of the object
print(JwtResponse.to_json())

# convert the object into a dict
jwt_response_dict = jwt_response_instance.to_dict()
# create an instance of JwtResponse from a dict
jwt_response_form_dict = jwt_response.from_dict(jwt_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


