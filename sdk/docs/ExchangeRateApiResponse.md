# ExchangeRateApiResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**base_code** | **str** |  | [optional] 
**rates** | **Dict[str, float]** |  | [optional] 

## Example

```python
from tracko_sdk.models.exchange_rate_api_response import ExchangeRateApiResponse

# TODO update the JSON string below
json = "{}"
# create an instance of ExchangeRateApiResponse from a JSON string
exchange_rate_api_response_instance = ExchangeRateApiResponse.from_json(json)
# print the JSON string representation of the object
print(ExchangeRateApiResponse.to_json())

# convert the object into a dict
exchange_rate_api_response_dict = exchange_rate_api_response_instance.to_dict()
# create an instance of ExchangeRateApiResponse from a dict
exchange_rate_api_response_form_dict = exchange_rate_api_response.from_dict(exchange_rate_api_response_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


