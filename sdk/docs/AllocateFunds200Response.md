# AllocateFunds200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**BudgetCategoryDTO**](BudgetCategoryDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.allocate_funds200_response import AllocateFunds200Response

# TODO update the JSON string below
json = "{}"
# create an instance of AllocateFunds200Response from a JSON string
allocate_funds200_response_instance = AllocateFunds200Response.from_json(json)
# print the JSON string representation of the object
print(AllocateFunds200Response.to_json())

# convert the object into a dict
allocate_funds200_response_dict = allocate_funds200_response_instance.to_dict()
# create an instance of AllocateFunds200Response from a dict
allocate_funds200_response_form_dict = allocate_funds200_response.from_dict(allocate_funds200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


