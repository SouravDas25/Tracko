# tracko_sdk.UserControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create**](UserControllerApi.md#create) | **POST** /api/user/create | 
[**me**](UserControllerApi.md#me) | **GET** /api/user/me | 
[**reset_data**](UserControllerApi.md#reset_data) | **DELETE** /api/user/data | 
[**reset_transactions**](UserControllerApi.md#reset_transactions) | **DELETE** /api/user/transactions | 
[**show**](UserControllerApi.md#show) | **GET** /api/user | 
[**show1**](UserControllerApi.md#show1) | **GET** /api/user/{id} | 
[**show_by_phone**](UserControllerApi.md#show_by_phone) | **GET** /api/user/byPhoneNo | 
[**update_profile**](UserControllerApi.md#update_profile) | **POST** /api/user/me | 


# **create**
> object create(user_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.user_save_request import UserSaveRequest
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
    api_instance = tracko_sdk.UserControllerApi(api_client)
    user_save_request = tracko_sdk.UserSaveRequest() # UserSaveRequest | 

    try:
        api_response = api_instance.create(user_save_request)
        print("The response of UserControllerApi->create:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->create: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user_save_request** | [**UserSaveRequest**](UserSaveRequest.md)|  | 

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

# **me**
> object me()



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
    api_instance = tracko_sdk.UserControllerApi(api_client)

    try:
        api_response = api_instance.me()
        print("The response of UserControllerApi->me:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->me: %s\n" % e)
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

# **reset_data**
> object reset_data()



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
    api_instance = tracko_sdk.UserControllerApi(api_client)

    try:
        api_response = api_instance.reset_data()
        print("The response of UserControllerApi->reset_data:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->reset_data: %s\n" % e)
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

# **reset_transactions**
> object reset_transactions()



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
    api_instance = tracko_sdk.UserControllerApi(api_client)

    try:
        api_response = api_instance.reset_transactions()
        print("The response of UserControllerApi->reset_transactions:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->reset_transactions: %s\n" % e)
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

# **show**
> object show()



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
    api_instance = tracko_sdk.UserControllerApi(api_client)

    try:
        api_response = api_instance.show()
        print("The response of UserControllerApi->show:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->show: %s\n" % e)
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

# **show1**
> object show1(id)



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
    api_instance = tracko_sdk.UserControllerApi(api_client)
    id = 'id_example' # str | 

    try:
        api_response = api_instance.show1(id)
        print("The response of UserControllerApi->show1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->show1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **str**|  | 

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

# **show_by_phone**
> object show_by_phone(phone_no)



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
    api_instance = tracko_sdk.UserControllerApi(api_client)
    phone_no = 'phone_no_example' # str | 

    try:
        api_response = api_instance.show_by_phone(phone_no)
        print("The response of UserControllerApi->show_by_phone:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->show_by_phone: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **phone_no** | **str**|  | 

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

# **update_profile**
> object update_profile(user_profile_update_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.user_profile_update_request import UserProfileUpdateRequest
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
    api_instance = tracko_sdk.UserControllerApi(api_client)
    user_profile_update_request = tracko_sdk.UserProfileUpdateRequest() # UserProfileUpdateRequest | 

    try:
        api_response = api_instance.update_profile(user_profile_update_request)
        print("The response of UserControllerApi->update_profile:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserControllerApi->update_profile: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user_profile_update_request** | [**UserProfileUpdateRequest**](UserProfileUpdateRequest.md)|  | 

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

