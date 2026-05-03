# GetChartData200Response


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**result** | [**AnalyticsResponseDTO**](AnalyticsResponseDTO.md) |  | [optional] 
**message** | **str** | Human-readable status message | [optional] 

## Example

```python
from tracko_sdk.models.get_chart_data200_response import GetChartData200Response

# TODO update the JSON string below
json = "{}"
# create an instance of GetChartData200Response from a JSON string
get_chart_data200_response_instance = GetChartData200Response.from_json(json)
# print the JSON string representation of the object
print(GetChartData200Response.to_json())

# convert the object into a dict
get_chart_data200_response_dict = get_chart_data200_response_instance.to_dict()
# create an instance of GetChartData200Response from a dict
get_chart_data200_response_form_dict = get_chart_data200_response.from_dict(get_chart_data200_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


