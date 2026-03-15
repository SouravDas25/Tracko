# GetAll1200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**TransactionsPageDTO**](TransactionsPageDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_all1200_response import GetAll1200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetAll1200Response from a JSON string
get_all1200_response_instance = GetAll1200Response.from_json(json)
# print the JSON string representation of the object
print(GetAll1200Response.to_json())

# convert the object into a dict
get_all1200_response_dict = get_all1200_response_instance.to_dict()
# create an instance of GetAll1200Response from a dict
get_all1200_response_form_dict = get_all1200_response.from_dict(get_all1200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


