# AccountSaveRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | 
**currency** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.account_save_request import AccountSaveRequest

# TODO update the JSON string below
json = "{}"
# create an instance of AccountSaveRequest from a JSON string
account_save_request_instance = AccountSaveRequest.from_json(json)
# print the JSON string representation of the object
print(AccountSaveRequest.to_json())

# convert the object into a dict
account_save_request_dict = account_save_request_instance.to_dict()
# create an instance of AccountSaveRequest from a dict
account_save_request_form_dict = account_save_request.from_dict(account_save_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


