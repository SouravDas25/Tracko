# StatsPointDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**label** | **str** |  | [optional] 
**value** | **float** |  | [optional] 

## Example

```python
from tracko_sdk.models.stats_point_dto import StatsPointDTO

# TODO update the JSON string below
json = "{}"
# create an instance of StatsPointDTO from a JSON string
stats_point_dto_instance = StatsPointDTO.from_json(json)
# print the JSON string representation of the object
print(StatsPointDTO.to_json())

# convert the object into a dict
stats_point_dto_dict = stats_point_dto_instance.to_dict()
# create an instance of StatsPointDTO from a dict
stats_point_dto_form_dict = stats_point_dto.from_dict(stats_point_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


