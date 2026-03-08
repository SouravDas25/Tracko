# tracko_sdk.ExchangeRatesApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_rates**](ExchangeRatesApi.md#get_rates) | **GET** /api/exchange-rates/{baseCurrency} | Get exchange rates for a base currency (e.g. USD)


# **get_rates**
> ExchangeRateApiResponse get_rates(base_currency)

Get exchange rates for a base currency (e.g. USD)

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.exchange_rate_api_response import ExchangeRateApiResponse
from tracko_sdk.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8080
# See configuration.py for a list of all supported configuration parameters.
configuration = tracko_sdk.Configuration(
    host = "http://localhost:8080"
)

# The client must configure the authentication and authorization parameters
# in accordance with the API server security policy.
# Examples for each auth method are provided below, use the example that
# satisfies your auth use case.

# Configure Bearer authorization (JWT): bearerAuth
configuration = tracko_sdk.Configuration(
    access_token = os.environ["BEARER_TOKEN"]
)

# Enter a context with an instance of the API client
with tracko_sdk.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = tracko_sdk.ExchangeRatesApi(api_client)
    base_currency = 'base_currency_example' # str | 

    try:
        # Get exchange rates for a base currency (e.g. USD)
        api_response = api_instance.get_rates(base_currency)
        print("The response of ExchangeRatesApi->get_rates:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ExchangeRatesApi->get_rates: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **base_currency** | **str**|  | 

### Return type

[**ExchangeRateApiResponse**](ExchangeRateApiResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

