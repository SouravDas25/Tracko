# tracko_sdk.ExchangeRateControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_rates**](ExchangeRateControllerApi.md#get_rates) | **GET** /api/exchange-rates/{baseCurrency} | 


# **get_rates**
> object get_rates(base_currency)



### Example


```python
import tracko_sdk
from tracko_sdk.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost:8080
# See configuration.py for a list of all supported configuration parameters.
configuration = tracko_sdk.Configuration(
    host = "http://localhost:8080"
)


# Enter a context with an instance of the API client
with tracko_sdk.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = tracko_sdk.ExchangeRateControllerApi(api_client)
    base_currency = 'base_currency_example' # str | 

    try:
        api_response = api_instance.get_rates(base_currency)
        print("The response of ExchangeRateControllerApi->get_rates:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ExchangeRateControllerApi->get_rates: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **base_currency** | **str**|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

