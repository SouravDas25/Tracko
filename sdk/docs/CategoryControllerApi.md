# tracko_sdk.CategoryControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create6**](CategoryControllerApi.md#create6) | **POST** /api/categories | 
[**delete6**](CategoryControllerApi.md#delete6) | **DELETE** /api/categories/{id} | 
[**get_all5**](CategoryControllerApi.md#get_all5) | **GET** /api/categories | 
[**get_by_id3**](CategoryControllerApi.md#get_by_id3) | **GET** /api/categories/{id} | 
[**update4**](CategoryControllerApi.md#update4) | **PUT** /api/categories/{id} | 


# **create6**
> object create6(category_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.category_save_request import CategorySaveRequest
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
    api_instance = tracko_sdk.CategoryControllerApi(api_client)
    category_save_request = tracko_sdk.CategorySaveRequest() # CategorySaveRequest | 

    try:
        api_response = api_instance.create6(category_save_request)
        print("The response of CategoryControllerApi->create6:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CategoryControllerApi->create6: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **category_save_request** | [**CategorySaveRequest**](CategorySaveRequest.md)|  | 

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

# **delete6**
> object delete6(id)



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
    api_instance = tracko_sdk.CategoryControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.delete6(id)
        print("The response of CategoryControllerApi->delete6:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CategoryControllerApi->delete6: %s\n" % e)
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

# **get_all5**
> object get_all5()



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
    api_instance = tracko_sdk.CategoryControllerApi(api_client)

    try:
        api_response = api_instance.get_all5()
        print("The response of CategoryControllerApi->get_all5:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CategoryControllerApi->get_all5: %s\n" % e)
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

# **get_by_id3**
> object get_by_id3(id)



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
    api_instance = tracko_sdk.CategoryControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.get_by_id3(id)
        print("The response of CategoryControllerApi->get_by_id3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CategoryControllerApi->get_by_id3: %s\n" % e)
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

# **update4**
> object update4(id, category_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.category_save_request import CategorySaveRequest
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
    api_instance = tracko_sdk.CategoryControllerApi(api_client)
    id = 56 # int | 
    category_save_request = tracko_sdk.CategorySaveRequest() # CategorySaveRequest | 

    try:
        api_response = api_instance.update4(id, category_save_request)
        print("The response of CategoryControllerApi->update4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CategoryControllerApi->update4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **category_save_request** | [**CategorySaveRequest**](CategorySaveRequest.md)|  | 

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

