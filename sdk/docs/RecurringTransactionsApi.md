# tracko_sdk.RecurringTransactionsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create3**](RecurringTransactionsApi.md#create3) | **POST** /api/recurring-transactions | Create a recurring transaction
[**delete3**](RecurringTransactionsApi.md#delete3) | **DELETE** /api/recurring-transactions/{id} | Delete a recurring transaction
[**get_all3**](RecurringTransactionsApi.md#get_all3) | **GET** /api/recurring-transactions | List all recurring transactions
[**get_by_id2**](RecurringTransactionsApi.md#get_by_id2) | **GET** /api/recurring-transactions/{id} | Get a recurring transaction by ID
[**update1**](RecurringTransactionsApi.md#update1) | **PUT** /api/recurring-transactions/{id} | Update a recurring transaction


# **create3**
> GetById2200Response create3(recurring_transaction)

Create a recurring transaction

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_id2200_response import GetById2200Response
from tracko_sdk.models.recurring_transaction import RecurringTransaction
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
    api_instance = tracko_sdk.RecurringTransactionsApi(api_client)
    recurring_transaction = tracko_sdk.RecurringTransaction() # RecurringTransaction | 

    try:
        # Create a recurring transaction
        api_response = api_instance.create3(recurring_transaction)
        print("The response of RecurringTransactionsApi->create3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionsApi->create3: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **recurring_transaction** | [**RecurringTransaction**](RecurringTransaction.md)|  | 

### Return type

[**GetById2200Response**](GetById2200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **delete3**
> Delete1200Response delete3(id)

Delete a recurring transaction

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.delete1200_response import Delete1200Response
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
    api_instance = tracko_sdk.RecurringTransactionsApi(api_client)
    id = 56 # int | 

    try:
        # Delete a recurring transaction
        api_response = api_instance.delete3(id)
        print("The response of RecurringTransactionsApi->delete3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionsApi->delete3: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**Delete1200Response**](Delete1200Response.md)

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

# **get_all3**
> GetAll3200Response get_all3()

List all recurring transactions

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_all3200_response import GetAll3200Response
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
    api_instance = tracko_sdk.RecurringTransactionsApi(api_client)

    try:
        # List all recurring transactions
        api_response = api_instance.get_all3()
        print("The response of RecurringTransactionsApi->get_all3:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionsApi->get_all3: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetAll3200Response**](GetAll3200Response.md)

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

# **get_by_id2**
> GetById2200Response get_by_id2(id)

Get a recurring transaction by ID

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_id2200_response import GetById2200Response
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
    api_instance = tracko_sdk.RecurringTransactionsApi(api_client)
    id = 56 # int | 

    try:
        # Get a recurring transaction by ID
        api_response = api_instance.get_by_id2(id)
        print("The response of RecurringTransactionsApi->get_by_id2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionsApi->get_by_id2: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**GetById2200Response**](GetById2200Response.md)

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

# **update1**
> GetById2200Response update1(id, recurring_transaction)

Update a recurring transaction

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_id2200_response import GetById2200Response
from tracko_sdk.models.recurring_transaction import RecurringTransaction
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
    api_instance = tracko_sdk.RecurringTransactionsApi(api_client)
    id = 56 # int | 
    recurring_transaction = tracko_sdk.RecurringTransaction() # RecurringTransaction | 

    try:
        # Update a recurring transaction
        api_response = api_instance.update1(id, recurring_transaction)
        print("The response of RecurringTransactionsApi->update1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling RecurringTransactionsApi->update1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 
 **recurring_transaction** | [**RecurringTransaction**](RecurringTransaction.md)|  | 

### Return type

[**GetById2200Response**](GetById2200Response.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

