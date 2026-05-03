# NamedSeriesDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **str** |  | [optional] 
**series** | [**List[StatsPointDTO]**](StatsPointDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.named_series_dto import NamedSeriesDTO

# TODO update the JSON string below
json = "{}"
# create an instance of NamedSeriesDTO from a JSON string
named_series_dto_instance = NamedSeriesDTO.from_json(json)
# print the JSON string representation of the object
print(NamedSeriesDTO.to_json())

# convert the object into a dict
named_series_dto_dict = named_series_dto_instance.to_dict()
# create an instance of NamedSeriesDTO from a dict
named_series_dto_form_dict = named_series_dto.from_dict(named_series_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


