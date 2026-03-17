# ListMine200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**List[Contact]**](Contact.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.list_mine200_response import ListMine200Response

# TODO update the JSON string below
json = "{}"
# create an instance of ListMine200Response from a JSON string
list_mine200_response_instance = ListMine200Response.from_json(json)
# print the JSON string representation of the object
print(ListMine200Response.to_json())

# convert the object into a dict
list_mine200_response_dict = list_mine200_response_instance.to_dict()
# create an instance of ListMine200Response from a dict
list_mine200_response_form_dict = list_mine200_response.from_dict(list_mine200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


