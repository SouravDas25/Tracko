# CategoryStatDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**category_id** | **int** |  | [optional] 
**category_name** | **str** |  | [optional] 
**amount** | **float** |  | [optional] 

## Example

```python
from tracko_sdk.models.category_stat_dto import CategoryStatDTO

# TODO update the JSON string below
json = "{}"
# create an instance of CategoryStatDTO from a JSON string
category_stat_dto_instance = CategoryStatDTO.from_json(json)
# print the JSON string representation of the object
print(CategoryStatDTO.to_json())

# convert the object into a dict
category_stat_dto_dict = category_stat_dto_instance.to_dict()
# create an instance of CategoryStatDTO from a dict
category_stat_dto_form_dict = category_stat_dto.from_dict(category_stat_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


