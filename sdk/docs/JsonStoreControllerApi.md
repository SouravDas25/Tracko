# tracko_sdk.JsonStoreControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create4**](JsonStoreControllerApi.md#create4) | **POST** /api/json-store | 
[**delete4**](JsonStoreControllerApi.md#delete4) | **DELETE** /api/json-store/{name} | 
[**get_all4**](JsonStoreControllerApi.md#get_all4) | **GET** /api/json-store | 
[**get_by_name**](JsonStoreControllerApi.md#get_by_name) | **GET** /api/json-store/{name} | 
[**update2**](JsonStoreControllerApi.md#update2) | **PUT** /api/json-store/{name} | 


# **create4**
> object create4(json_store)



### Example


```python
import tracko_sdk
from tracko_sdk.models.json_store import JsonStore
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
    api_instance = tracko_sdk.JsonStoreControllerApi(api_client)
    json_store = tracko_sdk.JsonStore() # JsonStore | 

    try:
        api_response = api_instance.create4(json_store)
        print("The response of JsonStoreControllerApi->create4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JsonStoreControllerApi->create4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **json_store** | [**JsonStore**](JsonStore.md)|  | 

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

# **delete4**
> object delete4(name)



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
    api_instance = tracko_sdk.JsonStoreControllerApi(api_client)
    name = 'name_example' # str | 

    try:
        api_response = api_instance.delete4(name)
        print("The response of JsonStoreControllerApi->delete4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JsonStoreControllerApi->delete4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 

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

# **get_all4**
> object get_all4()



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
    api_instance = tracko_sdk.JsonStoreControllerApi(api_client)

    try:
        api_response = api_instance.get_all4()
        print("The response of JsonStoreControllerApi->get_all4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JsonStoreControllerApi->get_all4: %s\n" % e)
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

# **get_by_name**
> object get_by_name(name)



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
    api_instance = tracko_sdk.JsonStoreControllerApi(api_client)
    name = 'name_example' # str | 

    try:
        api_response = api_instance.get_by_name(name)
        print("The response of JsonStoreControllerApi->get_by_name:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JsonStoreControllerApi->get_by_name: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 

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

# **update2**
> object update2(name, json_store)



### Example


```python
import tracko_sdk
from tracko_sdk.models.json_store import JsonStore
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
    api_instance = tracko_sdk.JsonStoreControllerApi(api_client)
    name = 'name_example' # str | 
    json_store = tracko_sdk.JsonStore() # JsonStore | 

    try:
        api_response = api_instance.update2(name, json_store)
        print("The response of JsonStoreControllerApi->update2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JsonStoreControllerApi->update2: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 
 **json_store** | [**JsonStore**](JsonStore.md)|  | 

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

