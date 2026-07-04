# tracko_sdk.TransactionHistoryApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_all_history**](TransactionHistoryApi.md#get_all_history) | **GET** /api/transactions/history | List transaction change history (paged) across all transactions
[**get_history**](TransactionHistoryApi.md#get_history) | **GET** /api/transactions/{id}/history | Get a transaction&#39;s change history
[**get_trash**](TransactionHistoryApi.md#get_trash) | **GET** /api/transactions/trash | List deleted transactions (recycle bin)
[**revert**](TransactionHistoryApi.md#revert) | **POST** /api/transactions/history/{historyId}/revert | Revert a transaction to a history snapshot


# **get_all_history**
> GetAllHistory200Response get_all_history(operation=operation, page=page, size=size)

List transaction change history (paged) across all transactions

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_all_history200_response import GetAllHistory200Response
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
    api_instance = tracko_sdk.TransactionHistoryApi(api_client)
    operation = 'operation_example' # str |  (optional)
    page = 0 # int |  (optional) (default to 0)
    size = 30 # int |  (optional) (default to 30)

    try:
        # List transaction change history (paged) across all transactions
        api_response = api_instance.get_all_history(operation=operation, page=page, size=size)
        print("The response of TransactionHistoryApi->get_all_history:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionHistoryApi->get_all_history: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **operation** | **str**|  | [optional] 
 **page** | **int**|  | [optional] [default to 0]
 **size** | **int**|  | [optional] [default to 30]

### Return type

[**GetAllHistory200Response**](GetAllHistory200Response.md)

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

# **get_history**
> GetHistory200Response get_history(id)

Get a transaction's change history

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_history200_response import GetHistory200Response
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
    api_instance = tracko_sdk.TransactionHistoryApi(api_client)
    id = 56 # int | 

    try:
        # Get a transaction's change history
        api_response = api_instance.get_history(id)
        print("The response of TransactionHistoryApi->get_history:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionHistoryApi->get_history: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**GetHistory200Response**](GetHistory200Response.md)

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

# **get_trash**
> GetHistory200Response get_trash()

List deleted transactions (recycle bin)

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_history200_response import GetHistory200Response
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
    api_instance = tracko_sdk.TransactionHistoryApi(api_client)

    try:
        # List deleted transactions (recycle bin)
        api_response = api_instance.get_trash()
        print("The response of TransactionHistoryApi->get_trash:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionHistoryApi->get_trash: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetHistory200Response**](GetHistory200Response.md)

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

# **revert**
> Delete200Response revert(history_id)

Revert a transaction to a history snapshot

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.delete200_response import Delete200Response
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
    api_instance = tracko_sdk.TransactionHistoryApi(api_client)
    history_id = 56 # int | 

    try:
        # Revert a transaction to a history snapshot
        api_response = api_instance.revert(history_id)
        print("The response of TransactionHistoryApi->revert:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionHistoryApi->revert: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **history_id** | **int**|  | 

### Return type

[**Delete200Response**](Delete200Response.md)

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

