# tracko_sdk.UserCurrencyControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**delete**](UserCurrencyControllerApi.md#delete) | **DELETE** /api/user-currencies/{code} | 
[**get_all**](UserCurrencyControllerApi.md#get_all) | **GET** /api/user-currencies | 
[**save**](UserCurrencyControllerApi.md#save) | **POST** /api/user-currencies | 
[**save_auto**](UserCurrencyControllerApi.md#save_auto) | **POST** /api/user-currencies/auto | 


# **delete**
> object delete(code)



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
    api_instance = tracko_sdk.UserCurrencyControllerApi(api_client)
    code = 'code_example' # str | 

    try:
        api_response = api_instance.delete(code)
        print("The response of UserCurrencyControllerApi->delete:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrencyControllerApi->delete: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **code** | **str**|  | 

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

# **get_all**
> object get_all()



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
    api_instance = tracko_sdk.UserCurrencyControllerApi(api_client)

    try:
        api_response = api_instance.get_all()
        print("The response of UserCurrencyControllerApi->get_all:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrencyControllerApi->get_all: %s\n" % e)
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

# **save**
> object save(user_currency_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.user_currency_request import UserCurrencyRequest
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
    api_instance = tracko_sdk.UserCurrencyControllerApi(api_client)
    user_currency_request = tracko_sdk.UserCurrencyRequest() # UserCurrencyRequest | 

    try:
        api_response = api_instance.save(user_currency_request)
        print("The response of UserCurrencyControllerApi->save:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrencyControllerApi->save: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user_currency_request** | [**UserCurrencyRequest**](UserCurrencyRequest.md)|  | 

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

# **save_auto**
> object save_auto(currency_code)



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
    api_instance = tracko_sdk.UserCurrencyControllerApi(api_client)
    currency_code = 'currency_code_example' # str | 

    try:
        api_response = api_instance.save_auto(currency_code)
        print("The response of UserCurrencyControllerApi->save_auto:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrencyControllerApi->save_auto: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **currency_code** | **str**|  | 

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

