# tracko_sdk.SplitsApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create2**](SplitsApi.md#create2) | **POST** /api/splits | Create a split for a transaction
[**delete2**](SplitsApi.md#delete2) | **DELETE** /api/splits/{id} | Delete a split
[**get_all2**](SplitsApi.md#get_all2) | **GET** /api/splits | List all splits for the current user
[**get_by_contact_id**](SplitsApi.md#get_by_contact_id) | **GET** /api/splits/contact/{contactId} | List all splits for a contact
[**get_by_id1**](SplitsApi.md#get_by_id1) | **GET** /api/splits/{id} | Get a split by ID
[**get_by_transaction_id**](SplitsApi.md#get_by_transaction_id) | **GET** /api/splits/transaction/{transactionId} | List splits for a transaction
[**get_my_unsettled**](SplitsApi.md#get_my_unsettled) | **GET** /api/splits/unsettled | List all unsettled splits for the current user
[**get_unsettled_by_contact_id**](SplitsApi.md#get_unsettled_by_contact_id) | **GET** /api/splits/contact/{contactId}/unsettled | List unsettled splits for a contact
[**settle**](SplitsApi.md#settle) | **PATCH** /api/splits/settle/{splitId} | Mark a split as settled
[**unsettle**](SplitsApi.md#unsettle) | **PATCH** /api/splits/unsettle/{splitId} | Reverse a settlement on a split


# **create2**
> Split create2(split)

Create a split for a transaction

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)
    split = tracko_sdk.Split() # Split | 

    try:
        # Create a split for a transaction
        api_response = api_instance.create2(split)
        print("The response of SplitsApi->create2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->create2: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **split** | [**Split**](Split.md)|  | 

### Return type

[**Split**](Split.md)

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

# **delete2**
> str delete2(id)

Delete a split

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
    api_instance = tracko_sdk.SplitsApi(api_client)
    id = 56 # int | 

    try:
        # Delete a split
        api_response = api_instance.delete2(id)
        print("The response of SplitsApi->delete2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->delete2: %s\n" % e)
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

# **get_all2**
> List[Split] get_all2()

List all splits for the current user

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)

    try:
        # List all splits for the current user
        api_response = api_instance.get_all2()
        print("The response of SplitsApi->get_all2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_all2: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**List[Split]**](Split.md)

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

# **get_by_contact_id**
> List[Split] get_by_contact_id(contact_id)

List all splits for a contact

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)
    contact_id = 56 # int | 

    try:
        # List all splits for a contact
        api_response = api_instance.get_by_contact_id(contact_id)
        print("The response of SplitsApi->get_by_contact_id:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_by_contact_id: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contact_id** | **int**|  | 

### Return type

[**List[Split]**](Split.md)

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

# **get_by_id1**
> Split get_by_id1(id)

Get a split by ID

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)
    id = 56 # int | 

    try:
        # Get a split by ID
        api_response = api_instance.get_by_id1(id)
        print("The response of SplitsApi->get_by_id1:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_by_id1: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **int**|  | 

### Return type

[**Split**](Split.md)

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

# **get_by_transaction_id**
> List[Split] get_by_transaction_id(transaction_id)

List splits for a transaction

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)
    transaction_id = 56 # int | 

    try:
        # List splits for a transaction
        api_response = api_instance.get_by_transaction_id(transaction_id)
        print("The response of SplitsApi->get_by_transaction_id:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_by_transaction_id: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **transaction_id** | **int**|  | 

### Return type

[**List[Split]**](Split.md)

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

# **get_my_unsettled**
> List[Split] get_my_unsettled()

List all unsettled splits for the current user

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)

    try:
        # List all unsettled splits for the current user
        api_response = api_instance.get_my_unsettled()
        print("The response of SplitsApi->get_my_unsettled:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_my_unsettled: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**List[Split]**](Split.md)

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

# **get_unsettled_by_contact_id**
> List[Split] get_unsettled_by_contact_id(contact_id)

List unsettled splits for a contact

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.split import Split
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
    api_instance = tracko_sdk.SplitsApi(api_client)
    contact_id = 56 # int | 

    try:
        # List unsettled splits for a contact
        api_response = api_instance.get_unsettled_by_contact_id(contact_id)
        print("The response of SplitsApi->get_unsettled_by_contact_id:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->get_unsettled_by_contact_id: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **contact_id** | **int**|  | 

### Return type

[**List[Split]**](Split.md)

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

# **settle**
> str settle(split_id)

Mark a split as settled

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
    api_instance = tracko_sdk.SplitsApi(api_client)
    split_id = 56 # int | 

    try:
        # Mark a split as settled
        api_response = api_instance.settle(split_id)
        print("The response of SplitsApi->settle:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->settle: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **split_id** | **int**|  | 

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

# **unsettle**
> str unsettle(split_id)

Reverse a settlement on a split

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
    api_instance = tracko_sdk.SplitsApi(api_client)
    split_id = 56 # int | 

    try:
        # Reverse a settlement on a split
        api_response = api_instance.unsettle(split_id)
        print("The response of SplitsApi->unsettle:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SplitsApi->unsettle: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **split_id** | **int**|  | 

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

