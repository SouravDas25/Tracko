# User


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **str** |  | [optional] 
**name** | **str** |  | [optional] 
**phone_no** | **str** |  | 
**email** | **str** |  | [optional] 
**profile_pic** | **str** |  | [optional] 
**password** | **str** |  | [optional] 
**global_id** | **str** |  | [optional] 
**base_currency** | **str** |  | [optional] 
**is_shadow** | **int** |  | [optional] 
**is_admin** | **int** |  | [optional] 
**secondary_currencies** | [**List[UserCurrency]**](UserCurrency.md) |  | [optional] 
**shadow** | **bool** |  | [optional] 
**admin** | **bool** |  | [optional] 

## Example

```python
from tracko_sdk.models.user import User

# TODO update the JSON string below
json = "{}"
# create an instance of User from a JSON string
user_instance = User.from_json(json)
# print the JSON string representation of the object
print(User.to_json())

# convert the object into a dict
user_dict = user_instance.to_dict()
# create an instance of User from a dict
user_form_dict = user.from_dict(user_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


