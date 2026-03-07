# CategorySaveRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | 
**category_type** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.category_save_request import CategorySaveRequest

# TODO update the JSON string below
json = "{}"
# create an instance of CategorySaveRequest from a JSON string
category_save_request_instance = CategorySaveRequest.from_json(json)
# print the JSON string representation of the object
print(CategorySaveRequest.to_json())

# convert the object into a dict
category_save_request_dict = category_save_request_instance.to_dict()
# create an instance of CategorySaveRequest from a dict
category_save_request_form_dict = category_save_request.from_dict(category_save_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


