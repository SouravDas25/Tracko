# StatsResponseDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**range** | **str** |  | [optional] 
**transaction_type** | **str** |  | [optional] 
**period_start** | **str** |  | [optional] 
**period_end** | **str** |  | [optional] 
**total** | **float** |  | [optional] 
**series** | [**List[StatsPointDTO]**](StatsPointDTO.md) |  | [optional] 
**categories** | [**List[CategoryStatDTO]**](CategoryStatDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.stats_response_dto import StatsResponseDTO

# TODO update the JSON string below
json = "{}"
# create an instance of StatsResponseDTO from a JSON string
stats_response_dto_instance = StatsResponseDTO.from_json(json)
# print the JSON string representation of the object
print(StatsResponseDTO.to_json())

# convert the object into a dict
stats_response_dto_dict = stats_response_dto_instance.to_dict()
# create an instance of StatsResponseDTO from a dict
stats_response_dto_form_dict = stats_response_dto.from_dict(stats_response_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


