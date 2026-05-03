# AnalyticsResponseDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**granularity** | **str** |  | [optional] 
**transaction_type** | **str** |  | [optional] 
**period_start** | **str** |  | [optional] 
**period_end** | **str** |  | [optional] 
**total** | **float** |  | [optional] 
**grouped_series** | [**List[NamedSeriesDTO]**](NamedSeriesDTO.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.analytics_response_dto import AnalyticsResponseDTO

# TODO update the JSON string below
json = "{}"
# create an instance of AnalyticsResponseDTO from a JSON string
analytics_response_dto_instance = AnalyticsResponseDTO.from_json(json)
# print the JSON string representation of the object
print(AnalyticsResponseDTO.to_json())

# convert the object into a dict
analytics_response_dto_dict = analytics_response_dto_instance.to_dict()
# create an instance of AnalyticsResponseDTO from a dict
analytics_response_dto_form_dict = analytics_response_dto.from_dict(analytics_response_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


