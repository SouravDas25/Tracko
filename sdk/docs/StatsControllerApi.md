# tracko_sdk.StatsControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_category_stats**](StatsControllerApi.md#get_category_stats) | **GET** /api/stats/category-summary | 
[**get_stats**](StatsControllerApi.md#get_stats) | **GET** /api/stats/summary | 


# **get_category_stats**
> object get_category_stats(range, transaction_type, category_id, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)



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
    api_instance = tracko_sdk.StatsControllerApi(api_client)
    range = 'range_example' # str | 
    transaction_type = 'transaction_type_example' # str | 
    category_id = 56 # int | 
    account_id = 56 # int |  (optional)
    var_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)

    try:
        api_response = api_instance.get_category_stats(range, transaction_type, category_id, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)
        print("The response of StatsControllerApi->get_category_stats:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling StatsControllerApi->get_category_stats: %s\n" % e)
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

# **get_stats**
> object get_stats(range, transaction_type, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)



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
    api_instance = tracko_sdk.StatsControllerApi(api_client)
    range = 'range_example' # str | 
    transaction_type = 'transaction_type_example' # str | 
    account_id = 56 # int |  (optional)
    var_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)

    try:
        api_response = api_instance.get_stats(range, transaction_type, account_id=account_id, var_date=var_date, start_date=start_date, end_date=end_date)
        print("The response of StatsControllerApi->get_stats:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling StatsControllerApi->get_stats: %s\n" % e)
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

