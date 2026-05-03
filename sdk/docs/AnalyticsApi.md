# tracko_sdk.AnalyticsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_chart_data**](AnalyticsApi.md#get_chart_data) | **GET** /api/analytics/chart | Get chart data with optional grouping and granularity


# **get_chart_data**
> GetChartData200Response get_chart_data(transaction_type, start_date, end_date, granularity=granularity, group_by=group_by, account_id=account_id, category_id=category_id)

Get chart data with optional grouping and granularity

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_chart_data200_response import GetChartData200Response
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
    api_instance = tracko_sdk.AnalyticsApi(api_client)
    transaction_type = 'transaction_type_example' # str | 
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 
    granularity = 'granularity_example' # str |  (optional)
    group_by = 'group_by_example' # str |  (optional)
    account_id = 56 # int |  (optional)
    category_id = 56 # int |  (optional)

    try:
        # Get chart data with optional grouping and granularity
        api_response = api_instance.get_chart_data(transaction_type, start_date, end_date, granularity=granularity, group_by=group_by, account_id=account_id, category_id=category_id)
        print("The response of AnalyticsApi->get_chart_data:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AnalyticsApi->get_chart_data: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **transaction_type** | **str**|  | 
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 
 **granularity** | **str**|  | [optional] 
 **group_by** | **str**|  | [optional] 
 **account_id** | **int**|  | [optional] 
 **category_id** | **int**|  | [optional] 

### Return type

[**GetChartData200Response**](GetChartData200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

