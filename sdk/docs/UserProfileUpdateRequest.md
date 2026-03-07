# UserProfileUpdateRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | [optional] 
**email** | **str** |  | [optional] 
**profile_pic** | **str** |  | [optional] 
**base_currency** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.user_profile_update_request import UserProfileUpdateRequest

# TODO update the JSON string below
json = "{}"
# create an instance of UserProfileUpdateRequest from a JSON string
user_profile_update_request_instance = UserProfileUpdateRequest.from_json(json)
# print the JSON string representation of the object
print(UserProfileUpdateRequest.to_json())

# convert the object into a dict
user_profile_update_request_dict = user_profile_update_request_instance.to_dict()
# create an instance of UserProfileUpdateRequest from a dict
user_profile_update_request_form_dict = user_profile_update_request.from_dict(user_profile_update_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


