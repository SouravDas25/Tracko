# GetById2200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**RecurringTransaction**](RecurringTransaction.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_by_id2200_response import GetById2200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetById2200Response from a JSON string
get_by_id2200_response_instance = GetById2200Response.from_json(json)
# print the JSON string representation of the object
print(GetById2200Response.to_json())

# convert the object into a dict
get_by_id2200_response_dict = get_by_id2200_response_instance.to_dict()
# create an instance of GetById2200Response from a dict
get_by_id2200_response_form_dict = get_by_id2200_response.from_dict(get_by_id2200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


