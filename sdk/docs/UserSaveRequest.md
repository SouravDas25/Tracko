# UserSaveRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | [optional] 
**phone_no** | **str** |  | 
**email** | **str** |  | [optional] 
**profile_pic** | **str** |  | [optional] 
**is_shadow** | **int** |  | [optional] 
**base_currency** | **str** |  | [optional] 
**shadow** | **bool** |  | [optional] 
**password** | **str** |  | 

## Example

```python
from tracko_sdk.models.user_save_request import UserSaveRequest

# TODO update the JSON string below
json = "{}"
# create an instance of UserSaveRequest from a JSON string
user_save_request_instance = UserSaveRequest.from_json(json)
# print the JSON string representation of the object
print(UserSaveRequest.to_json())

# convert the object into a dict
user_save_request_dict = user_save_request_instance.to_dict()
# create an instance of UserSaveRequest from a dict
user_save_request_form_dict = user_save_request.from_dict(user_save_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


