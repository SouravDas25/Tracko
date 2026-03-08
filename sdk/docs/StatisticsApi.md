# tracko_sdk.StatisticsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_category_stats**](StatisticsApi.md#get_category_stats) | **GET** /api/stats/category-summary | Get stats for a specific category by range
[**get_stats**](StatisticsApi.md#get_stats) | **GET** /api/stats/summary | Get aggregated stats by range (weekly/monthly/yearly/custom)


# **get_category_stats**
> StatsResponseDTO get_category_stats(range, transaction_type, category_id, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)

Get stats for a specific category by range

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.stats_response_dto import StatsResponseDTO
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
    api_instance = tracko_sdk.StatisticsApi(api_client)
    range = 'range_example' # str | 
    transaction_type = 'transaction_type_example' # str | 
    category_id = 56 # int | 
    account_id = 56 # int |  (optional)
    var_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)

    try:
        # Get stats for a specific category by range
        api_response = api_instance.get_category_stats(range, transaction_type, category_id, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)
        print("The response of StatisticsApi->get_category_stats:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling StatisticsApi->get_category_stats: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **range** | **str**|  | 
 **transaction_type** | **str**|  | 
 **category_id** | **int**|  | 
 **account_id** | **int**|  | [optional] 
 **var_date** | **datetime**|  | [optional] 
 **start_date** | **datetime**|  | [optional] 
 **end_date** | **datetime**|  | [optional] 

### Return type

[**StatsResponseDTO**](StatsResponseDTO.md)

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

# **get_stats**
> StatsResponseDTO get_stats(range, transaction_type, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)

Get aggregated stats by range (weekly/monthly/yearly/custom)

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.stats_response_dto import StatsResponseDTO
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
    api_instance = tracko_sdk.StatisticsApi(api_client)
    range = 'range_example' # str | 
    transaction_type = 'transaction_type_example' # str | 
    account_id = 56 # int |  (optional)
    var_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)

    try:
        # Get aggregated stats by range (weekly/monthly/yearly/custom)
        api_response = api_instance.get_stats(range, transaction_type, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)
        print("The response of StatisticsApi->get_stats:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling StatisticsApi->get_stats: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **range** | **str**|  | 
 **transaction_type** | **str**|  | 
 **account_id** | **int**|  | [optional] 
 **var_date** | **datetime**|  | [optional] 
 **start_date** | **datetime**|  | [optional] 
 **end_date** | **datetime**|  | [optional] 

### Return type

[**StatsResponseDTO**](StatsResponseDTO.md)

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

