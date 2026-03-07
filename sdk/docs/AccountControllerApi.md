# tracko_sdk.AccountControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create7**](AccountControllerApi.md#create7) | **POST** /api/accounts | 
[**delete7**](AccountControllerApi.md#delete7) | **DELETE** /api/accounts/{id} | 
[**get_account_monthly_summaries**](AccountControllerApi.md#get_account_monthly_summaries) | **GET** /api/accounts/{id}/summary/monthly | 
[**get_account_summary**](AccountControllerApi.md#get_account_summary) | **GET** /api/accounts/{id}/summary | 
[**get_account_transactions**](AccountControllerApi.md#get_account_transactions) | **GET** /api/accounts/{id}/transactions | 
[**get_account_yearly_summaries**](AccountControllerApi.md#get_account_yearly_summaries) | **GET** /api/accounts/{id}/summary/yearly | 
[**get_all6**](AccountControllerApi.md#get_all6) | **GET** /api/accounts | 
[**get_by_id4**](AccountControllerApi.md#get_by_id4) | **GET** /api/accounts/{id} | 
[**get_my_account_balances**](AccountControllerApi.md#get_my_account_balances) | **GET** /api/accounts/balances | 
[**update5**](AccountControllerApi.md#update5) | **PUT** /api/accounts/{id} | 


# **create7**
> object create7(account_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.account_save_request import AccountSaveRequest
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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    account_save_request = tracko_sdk.AccountSaveRequest() # AccountSaveRequest | 

    try:
        api_response = api_instance.create7(account_save_request)
        print("The response of AccountControllerApi->create7:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->create7: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **account_save_request** | [**AccountSaveRequest**](AccountSaveRequest.md)|  | 

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

# **delete7**
> object delete7(id)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.delete7(id)
        print("The response of AccountControllerApi->delete7:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->delete7: %s\n" % e)
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

# **get_account_monthly_summaries**
> object get_account_monthly_summaries(id, year=year)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 
    year = 56 # int |  (optional)

    try:
        api_response = api_instance.get_account_monthly_summaries(id, year=year)
        print("The response of AccountControllerApi->get_account_monthly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_account_monthly_summaries: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **year** | **int**|  | [optional] 

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

# **get_account_summary**
> object get_account_summary(id, start_date, end_date, include_rollover=include_rollover)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 
    include_rollover = True # bool |  (optional) (default to True)

    try:
        api_response = api_instance.get_account_summary(id, start_date, end_date, include_rollover=include_rollover)
        print("The response of AccountControllerApi->get_account_summary:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_account_summary: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 
 **include_rollover** | **bool**|  | [optional] [default to True]

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

# **get_account_transactions**
> object get_account_transactions(id, month=month, year=year, start_date=start_date, end_date=end_date, category_id=category_id, page=page, size=size, expand=expand)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    category_id = 56 # int |  (optional)
    page = 0 # int |  (optional) (default to 0)
    size = 500 # int |  (optional) (default to 500)
    expand = False # bool |  (optional) (default to False)

    try:
        api_response = api_instance.get_account_transactions(id, month=month, year=year, start_date=start_date, end_date=end_date, category_id=category_id, page=page, size=size, expand=expand)
        print("The response of AccountControllerApi->get_account_transactions:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_account_transactions: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **month** | **int**|  | [optional] 
 **year** | **int**|  | [optional] 
 **start_date** | **datetime**|  | [optional] 
 **end_date** | **datetime**|  | [optional] 
 **category_id** | **int**|  | [optional] 
 **page** | **int**|  | [optional] [default to 0]
 **size** | **int**|  | [optional] [default to 500]
 **expand** | **bool**|  | [optional] [default to False]

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

# **get_account_yearly_summaries**
> object get_account_yearly_summaries(id)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.get_account_yearly_summaries(id)
        print("The response of AccountControllerApi->get_account_yearly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_account_yearly_summaries: %s\n" % e)
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

# **get_all6**
> object get_all6()



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)

    try:
        api_response = api_instance.get_all6()
        print("The response of AccountControllerApi->get_all6:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_all6: %s\n" % e)
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

# **get_by_id4**
> object get_by_id4(id)



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 

    try:
        api_response = api_instance.get_by_id4(id)
        print("The response of AccountControllerApi->get_by_id4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_by_id4: %s\n" % e)
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

# **get_my_account_balances**
> object get_my_account_balances()



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
    api_instance = tracko_sdk.AccountControllerApi(api_client)

    try:
        api_response = api_instance.get_my_account_balances()
        print("The response of AccountControllerApi->get_my_account_balances:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->get_my_account_balances: %s\n" % e)
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

# **update5**
> object update5(id, account_save_request)



### Example


```python
import tracko_sdk
from tracko_sdk.models.account_save_request import AccountSaveRequest
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
    api_instance = tracko_sdk.AccountControllerApi(api_client)
    id = 56 # int | 
    account_save_request = tracko_sdk.AccountSaveRequest() # AccountSaveRequest | 

    try:
        api_response = api_instance.update5(id, account_save_request)
        print("The response of AccountControllerApi->update5:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountControllerApi->update5: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **account_save_request** | [**AccountSaveRequest**](AccountSaveRequest.md)|  | 

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

