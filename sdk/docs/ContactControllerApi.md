# tracko_sdk.ContactControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create5**](ContactControllerApi.md#create5) | **POST** /api/contacts | 
[**delete5**](ContactControllerApi.md#delete5) | **DELETE** /api/contacts/{id} | 
[**get_one**](ContactControllerApi.md#get_one) | **GET** /api/contacts/{id} | 
[**list_mine**](ContactControllerApi.md#list_mine) | **GET** /api/contacts | 
[**update3**](ContactControllerApi.md#update3) | **PUT** /api/contacts/{id} | 


# **create5**
> object create5(contact_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.contact_save_request import ContactSaveRequest
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
    api_instance = tracko_sdk.ContactControllerApi(api_client)
    contact_save_request = tracko_sdk.ContactSaveRequest() # ContactSaveRequest | 

    try:
        api_response = api_instance.create5(contact_save_request)
        print("The response of ContactControllerApi->create5:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ContactControllerApi->create5: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contact_save_request** | [**ContactSaveRequest**](ContactSaveRequest.md)|  | 

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

# **delete5**
> object delete5(id)



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
    api_instance = tracko_sdk.ContactControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.delete5(id)
        print("The response of ContactControllerApi->delete5:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ContactControllerApi->delete5: %s\n" % e)
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

# **get_one**
> object get_one(id)



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
    api_instance = tracko_sdk.ContactControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.get_one(id)
        print("The response of ContactControllerApi->get_one:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ContactControllerApi->get_one: %s\n" % e)
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

# **list_mine**
> object list_mine()



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
    api_instance = tracko_sdk.ContactControllerApi(api_client)

    try:
        api_response = api_instance.list_mine()
        print("The response of ContactControllerApi->list_mine:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ContactControllerApi->list_mine: %s\n" % e)
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

# **update3**
> object update3(id, contact_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.contact_save_request import ContactSaveRequest
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
    api_instance = tracko_sdk.ContactControllerApi(api_client)
    id = 56 # int | 
    contact_save_request = tracko_sdk.ContactSaveRequest() # ContactSaveRequest | 

    try:
        api_response = api_instance.update3(id, contact_save_request)
        print("The response of ContactControllerApi->update3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ContactControllerApi->update3: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **contact_save_request** | [**ContactSaveRequest**](ContactSaveRequest.md)|  | 

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

