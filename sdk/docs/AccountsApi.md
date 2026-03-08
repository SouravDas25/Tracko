# tracko_sdk.AccountsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create7**](AccountsApi.md#create7) | **POST** /api/accounts | Create a new account
[**delete7**](AccountsApi.md#delete7) | **DELETE** /api/accounts/{id} | Delete an account
[**get_account_monthly_summaries**](AccountsApi.md#get_account_monthly_summaries) | **GET** /api/accounts/{id}/summary/monthly | Get monthly summaries for an account
[**get_account_summary**](AccountsApi.md#get_account_summary) | **GET** /api/accounts/{id}/summary | Get income/expense summary for an account in a date range
[**get_account_transactions**](AccountsApi.md#get_account_transactions) | **GET** /api/accounts/{id}/transactions | List transactions for an account with optional filters
[**get_account_yearly_summaries**](AccountsApi.md#get_account_yearly_summaries) | **GET** /api/accounts/{id}/summary/yearly | Get yearly summaries for an account
[**get_all6**](AccountsApi.md#get_all6) | **GET** /api/accounts | List all accounts for the current user
[**get_by_id4**](AccountsApi.md#get_by_id4) | **GET** /api/accounts/{id} | Get an account by ID
[**get_my_account_balances**](AccountsApi.md#get_my_account_balances) | **GET** /api/accounts/balances | Get balances for all accounts (derived from transactions)
[**update5**](AccountsApi.md#update5) | **PUT** /api/accounts/{id} | Update an account


# **create7**
> Account create7(account_save_request)

Create a new account

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.account import Account
from tracko_sdk.models.account_save_request import AccountSaveRequest
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    account_save_request = tracko_sdk.AccountSaveRequest() # AccountSaveRequest | 

    try:
        # Create a new account
        api_response = api_instance.create7(account_save_request)
        print("The response of AccountsApi->create7:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->create7: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **account_save_request** | [**AccountSaveRequest**](AccountSaveRequest.md)|  | 

### Return type

[**Account**](Account.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete7**
> str delete7(id)

Delete an account

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 

    try:
        # Delete an account
        api_response = api_instance.delete7(id)
        print("The response of AccountsApi->delete7:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->delete7: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

**str**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_account_monthly_summaries**
> List[TransactionPeriodSummaryDTO] get_account_monthly_summaries(id, year=year)

Get monthly summaries for an account

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.transaction_period_summary_dto import TransactionPeriodSummaryDTO
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 
    year = 56 # int |  (optional)

    try:
        # Get monthly summaries for an account
        api_response = api_instance.get_account_monthly_summaries(id, year=year)
        print("The response of AccountsApi->get_account_monthly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_account_monthly_summaries: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **year** | **int**|  | [optional] 

### Return type

[**List[TransactionPeriodSummaryDTO]**](TransactionPeriodSummaryDTO.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_account_summary**
> TransactionSummaryDTO get_account_summary(id, start_date, end_date, include_rollover=include_rollover)

Get income/expense summary for an account in a date range

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.transaction_summary_dto import TransactionSummaryDTO
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 
    start_date = '2013-10-20T19:20:30+01:00' # datetime | 
    end_date = '2013-10-20T19:20:30+01:00' # datetime | 
    include_rollover = True # bool |  (optional) (default to True)

    try:
        # Get income/expense summary for an account in a date range
        api_response = api_instance.get_account_summary(id, start_date, end_date, include_rollover=include_rollover)
        print("The response of AccountsApi->get_account_summary:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_account_summary: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **start_date** | **datetime**|  | 
 **end_date** | **datetime**|  | 
 **include_rollover** | **bool**|  | [optional] [default to True]

### Return type

[**TransactionSummaryDTO**](TransactionSummaryDTO.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_account_transactions**
> TransactionsPageDTO get_account_transactions(id, month=month, year=year, start_date=start_date, end_date=end_date, category_id=category_id, page=page, size=size, expand=expand)

List transactions for an account with optional filters

### Example

* Bearer (JWT) Authentication (bearerAuth):

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
    api_instance = tracko_sdk.AccountsApi(api_client)
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
        # List transactions for an account with optional filters
        api_response = api_instance.get_account_transactions(id, month=month, year=year, start_date=start_date, end_date=end_date, category_id=category_id, page=page, size=size, expand=expand)
        print("The response of AccountsApi->get_account_transactions:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_account_transactions: %s\n" % e)
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

[**TransactionsPageDTO**](TransactionsPageDTO.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_account_yearly_summaries**
> List[TransactionPeriodSummaryDTO] get_account_yearly_summaries(id)

Get yearly summaries for an account

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.transaction_period_summary_dto import TransactionPeriodSummaryDTO
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 

    try:
        # Get yearly summaries for an account
        api_response = api_instance.get_account_yearly_summaries(id)
        print("The response of AccountsApi->get_account_yearly_summaries:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_account_yearly_summaries: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**List[TransactionPeriodSummaryDTO]**](TransactionPeriodSummaryDTO.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_all6**
> List[Account] get_all6()

List all accounts for the current user

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.account import Account
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
    api_instance = tracko_sdk.AccountsApi(api_client)

    try:
        # List all accounts for the current user
        api_response = api_instance.get_all6()
        print("The response of AccountsApi->get_all6:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_all6: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**List[Account]**](Account.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_by_id4**
> Account get_by_id4(id)

Get an account by ID

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.account import Account
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 

    try:
        # Get an account by ID
        api_response = api_instance.get_by_id4(id)
        print("The response of AccountsApi->get_by_id4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_by_id4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**Account**](Account.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

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

Get balances for all accounts (derived from transactions)

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
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
    api_instance = tracko_sdk.AccountsApi(api_client)

    try:
        # Get balances for all accounts (derived from transactions)
        api_response = api_instance.get_my_account_balances()
        print("The response of AccountsApi->get_my_account_balances:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->get_my_account_balances: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**object**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **update5**
> Account update5(id, account_save_request)

Update an account

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.account import Account
from tracko_sdk.models.account_save_request import AccountSaveRequest
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
    api_instance = tracko_sdk.AccountsApi(api_client)
    id = 56 # int | 
    account_save_request = tracko_sdk.AccountSaveRequest() # AccountSaveRequest | 

    try:
        # Update an account
        api_response = api_instance.update5(id, account_save_request)
        print("The response of AccountsApi->update5:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AccountsApi->update5: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **account_save_request** | [**AccountSaveRequest**](AccountSaveRequest.md)|  | 

### Return type

[**Account**](Account.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

