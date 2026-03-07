# tracko_sdk.RecurringTransactionControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create3**](RecurringTransactionControllerApi.md#create3) | **POST** /api/recurring-transactions | 
[**delete3**](RecurringTransactionControllerApi.md#delete3) | **DELETE** /api/recurring-transactions/{id} | 
[**get_all3**](RecurringTransactionControllerApi.md#get_all3) | **GET** /api/recurring-transactions | 
[**get_by_id2**](RecurringTransactionControllerApi.md#get_by_id2) | **GET** /api/recurring-transactions/{id} | 
[**update1**](RecurringTransactionControllerApi.md#update1) | **PUT** /api/recurring-transactions/{id} | 


# **create3**
> object create3(recurring_transaction)



### Example


```python
import tracko_sdk
from tracko_sdk.models.recurring_transaction import RecurringTransaction
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
    api_instance = tracko_sdk.RecurringTransactionControllerApi(api_client)
    recurring_transaction = tracko_sdk.RecurringTransaction() # RecurringTransaction | 

    try:
        api_response = api_instance.create3(recurring_transaction)
        print("The response of RecurringTransactionControllerApi->create3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionControllerApi->create3: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **recurring_transaction** | [**RecurringTransaction**](RecurringTransaction.md)|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete3**
> object delete3(id)



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
    api_instance = tracko_sdk.RecurringTransactionControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.delete3(id)
        print("The response of RecurringTransactionControllerApi->delete3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionControllerApi->delete3: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

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

# **get_all3**
> object get_all3()



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
    api_instance = tracko_sdk.RecurringTransactionControllerApi(api_client)

    try:
        api_response = api_instance.get_all3()
        print("The response of RecurringTransactionControllerApi->get_all3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionControllerApi->get_all3: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

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

# **get_by_id2**
> object get_by_id2(id)



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
    api_instance = tracko_sdk.RecurringTransactionControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.get_by_id2(id)
        print("The response of RecurringTransactionControllerApi->get_by_id2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionControllerApi->get_by_id2: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

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

# **update1**
> object update1(id, recurring_transaction)



### Example


```python
import tracko_sdk
from tracko_sdk.models.recurring_transaction import RecurringTransaction
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
    api_instance = tracko_sdk.RecurringTransactionControllerApi(api_client)
    id = 56 # int | 
    recurring_transaction = tracko_sdk.RecurringTransaction() # RecurringTransaction | 

    try:
        api_response = api_instance.update1(id, recurring_transaction)
        print("The response of RecurringTransactionControllerApi->update1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionControllerApi->update1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **recurring_transaction** | [**RecurringTransaction**](RecurringTransaction.md)|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

