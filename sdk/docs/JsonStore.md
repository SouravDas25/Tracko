# JsonStore


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | 
**value** | **str** |  | [optional] 

## Example

```python
from tracko_sdk.models.json_store import JsonStore

# TODO update the JSON string below
json = "{}"
# create an instance of JsonStore from a JSON string
json_store_instance = JsonStore.from_json(json)
# print the JSON string representation of the object
print(JsonStore.to_json())

# convert the object into a dict
json_store_dict = json_store_instance.to_dict()
# create an instance of JsonStore from a dict
json_store_form_dict = json_store.from_dict(json_store_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


