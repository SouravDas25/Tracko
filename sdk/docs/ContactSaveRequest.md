# ContactSaveRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | 
**phone_no** | **str** |  | [optional] 
**email** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.contact_save_request import ContactSaveRequest

# TODO update the JSON string below
json = "{}"
# create an instance of ContactSaveRequest from a JSON string
contact_save_request_instance = ContactSaveRequest.from_json(json)
# print the JSON string representation of the object
print(ContactSaveRequest.to_json())

# convert the object into a dict
contact_save_request_dict = contact_save_request_instance.to_dict()
# create an instance of ContactSaveRequest from a dict
contact_save_request_form_dict = contact_save_request.from_dict(contact_save_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


