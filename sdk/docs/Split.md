# Split


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**transaction_id** | **int** |  | 
**user_id** | **str** |  | 
**amount** | **float** |  | 
**settled_at** | **datetime** |  | [optional] 
**is_settled** | **int** |  | [optional] 
**contact_id** | **int** |  | [optional] 

## Example

```python
from tracko_sdk.models.split import Split

# TODO update the JSON string below
json = "{}"
# create an instance of Split from a JSON string
split_instance = Split.from_json(json)
# print the JSON string representation of the object
print(Split.to_json())

# convert the object into a dict
split_dict = split_instance.to_dict()
# create an instance of Split from a dict
split_form_dict = split.from_dict(split_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


