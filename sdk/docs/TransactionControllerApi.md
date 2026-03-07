# tracko_sdk.TransactionControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create1**](TransactionControllerApi.md#create1) | **POST** /api/transactions | Create a transaction or transfer
[**delete1**](TransactionControllerApi.md#delete1) | **DELETE** /api/transactions/{id} | Delete a transaction or transfer
[**get_all1**](TransactionControllerApi.md#get_all1) | **GET** /api/transactions | List transactions with optional filters
[**get_by_id**](TransactionControllerApi.md#get_by_id) | **GET** /api/transactions/{id} | Get a transaction by ID
[**get_monthly_summaries**](TransactionControllerApi.md#get_monthly_summaries) | **GET** /api/transactions/summary/monthly | Monthly summaries for a year
[**get_my_summary**](TransactionControllerApi.md#get_my_summary) | **GET** /api/transactions/summary | Get income/expense summary in a date range
[**get_my_total_expense**](TransactionControllerApi.md#get_my_total_expense) | **GET** /api/transactions/total-expense | Get total expense in a date range
[**get_my_total_income**](TransactionControllerApi.md#get_my_total_income) | **GET** /api/transactions/total-income | Get total income in a date range
[**get_yearly_summaries**](TransactionControllerApi.md#get_yearly_summaries) | **GET** /api/transactions/summary/yearly | Yearly summaries
[**update**](TransactionControllerApi.md#update) | **PUT** /api/transactions/{id} | Update a transaction or transfer


# **create1**
> object create1(transaction_request)

Create a transaction or transfer

### Example


```python
import tracko_sdk
from tracko_sdk.models.transaction_request import TransactionRequest
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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    transaction_request = tracko_sdk.TransactionRequest() # TransactionRequest | 

    try:
        # Create a transaction or transfer
        api_response = api_instance.create1(transaction_request)
        print("The response of TransactionControllerApi->create1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->create1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **transaction_request** | [**TransactionRequest**](TransactionRequest.md)|  | 

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

# **delete1**
> object delete1(id)

Delete a transaction or transfer

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    id = 56 # int | 

    try:
        # Delete a transaction or transfer
        api_response = api_instance.delete1(id)
        print("The response of TransactionControllerApi->delete1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->delete1: %s\n" % e)
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

# **get_all1**
> TransactionsPageDTO get_all1(month=month, year=year, start_date=start_date, end_date=end_date, account_ids=account_ids, category_id=category_id, page=page, size=size, expand=expand)

List transactions with optional filters

### Example


```python
import tracko_sdk
from tracko_sdk.models.transactions_page_dto import TransactionsPageDTO
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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)
    start_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    end_date = '2013-10-20T19:20:30+01:00' # datetime |  (optional)
    account_ids = 'account_ids_example' # str |  (optional)
    category_id = 56 # int |  (optional)
    page = 0 # int |  (optional) (default to 0)
    size = 500 # int |  (optional) (default to 500)
    expand = False # bool |  (optional) (default to False)

    try:
        # List transactions with optional filters
        api_response = api_instance.get_all1(month=month, year=year, start_date=start_date, end_date=end_date, account_ids=account_ids, category_id=category_id, page=page, size=size, expand=expand)
        print("The response of TransactionControllerApi->get_all1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_all1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **month** | **int**|  | [optional] 
 **year** | **int**|  | [optional] 
 **start_date** | **datetime**|  | [optional] 
 **end_date** | **datetime**|  | [optional] 
 **account_ids** | **str**|  | [optional] 
 **category_id** | **int**|  | [optional] 
 **page** | **int**|  | [optional] [default to 0]
 **size** | **int**|  | [optional] [default to 500]
 **expand** | **bool**|  | [optional] [default to False]

### Return type

[**TransactionsPageDTO**](TransactionsPageDTO.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_by_id**
> object get_by_id(id)

Get a transaction by ID

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    id = 56 # int | 

    try:
        # Get a transaction by ID
        api_response = api_instance.get_by_id(id)
        print("The response of TransactionControllerApi->get_by_id:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_by_id: %s\n" % e)
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

# **get_monthly_summaries**
> object get_monthly_summaries(year=year, account_ids=account_ids)

Monthly summaries for a year

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    year = 56 # int |  (optional)
    account_ids = 'account_ids_example' # str |  (optional)

    try:
        # Monthly summaries for a year
        api_response = api_instance.get_monthly_summaries(year=year, account_ids=account_ids)
        print("The response of TransactionControllerApi->get_monthly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_monthly_summaries: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **year** | **int**|  | [optional] 
 **account_ids** | **str**|  | [optional] 

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

# **get_my_summary**
> object get_my_summary(start_date, end_date, account_ids=account_ids, include_rollover=include_rollover)

Get income/expense summary in a date range

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 
    account_ids = 'account_ids_example' # str |  (optional)
    include_rollover = True # bool |  (optional) (default to True)

    try:
        # Get income/expense summary in a date range
        api_response = api_instance.get_my_summary(start_date, end_date, account_ids=account_ids, include_rollover=include_rollover)
        print("The response of TransactionControllerApi->get_my_summary:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_my_summary: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 
 **account_ids** | **str**|  | [optional] 
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

# **get_my_total_expense**
> object get_my_total_expense(start_date, end_date)

Get total expense in a date range

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 

    try:
        # Get total expense in a date range
        api_response = api_instance.get_my_total_expense(start_date, end_date)
        print("The response of TransactionControllerApi->get_my_total_expense:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_my_total_expense: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 

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

# **get_my_total_income**
> object get_my_total_income(start_date, end_date)

Get total income in a date range

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 

    try:
        # Get total income in a date range
        api_response = api_instance.get_my_total_income(start_date, end_date)
        print("The response of TransactionControllerApi->get_my_total_income:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_my_total_income: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 

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

# **get_yearly_summaries**
> object get_yearly_summaries(account_ids=account_ids)

Yearly summaries

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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    account_ids = 'account_ids_example' # str |  (optional)

    try:
        # Yearly summaries
        api_response = api_instance.get_yearly_summaries(account_ids=account_ids)
        print("The response of TransactionControllerApi->get_yearly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->get_yearly_summaries: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **account_ids** | **str**|  | [optional] 

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

# **update**
> object update(id, transaction_request)

Update a transaction or transfer

### Example


```python
import tracko_sdk
from tracko_sdk.models.transaction_request import TransactionRequest
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
    api_instance = tracko_sdk.TransactionControllerApi(api_client)
    id = 56 # int | 
    transaction_request = tracko_sdk.TransactionRequest() # TransactionRequest | 

    try:
        # Update a transaction or transfer
        api_response = api_instance.update(id, transaction_request)
        print("The response of TransactionControllerApi->update:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling TransactionControllerApi->update: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **transaction_request** | [**TransactionRequest**](TransactionRequest.md)|  | 

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

