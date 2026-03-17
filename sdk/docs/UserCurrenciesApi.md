# tracko_sdk.UserCurrenciesApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**delete**](UserCurrenciesApi.md#delete) | **DELETE** /api/user-currencies/{code} | Remove a currency from the current user
[**get_all**](UserCurrenciesApi.md#get_all) | **GET** /api/user-currencies | List currencies configured for the current user
[**save**](UserCurrenciesApi.md#save) | **POST** /api/user-currencies | Add or update a currency with a manual exchange rate
[**save_auto**](UserCurrenciesApi.md#save_auto) | **POST** /api/user-currencies/auto | Add a currency with an automatically fetched exchange rate


# **delete**
> Delete1200Response delete(code)

Remove a currency from the current user

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
    api_instance = tracko_sdk.UserCurrenciesApi(api_client)
    code = 'code_example' # str | 

    try:
        # Remove a currency from the current user
        api_response = api_instance.delete(code)
        print("The response of UserCurrenciesApi->delete:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrenciesApi->delete: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **code** | **str**|  | 

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

# **get_all**
> GetAll200Response get_all()

List currencies configured for the current user

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_all200_response import GetAll200Response
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
    api_instance = tracko_sdk.UserCurrenciesApi(api_client)

    try:
        # List currencies configured for the current user
        api_response = api_instance.get_all()
        print("The response of UserCurrenciesApi->get_all:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrenciesApi->get_all: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetAll200Response**](GetAll200Response.md)

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

# **save**
> Delete1200Response save(user_currency_request)

Add or update a currency with a manual exchange rate

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.delete1200_response import Delete1200Response
from tracko_sdk.models.user_currency_request import UserCurrencyRequest
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
    api_instance = tracko_sdk.UserCurrenciesApi(api_client)
    user_currency_request = tracko_sdk.UserCurrencyRequest() # UserCurrencyRequest | 

    try:
        # Add or update a currency with a manual exchange rate
        api_response = api_instance.save(user_currency_request)
        print("The response of UserCurrenciesApi->save:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrenciesApi->save: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **user_currency_request** | [**UserCurrencyRequest**](UserCurrencyRequest.md)|  | 

### Return type

[**Delete1200Response**](Delete1200Response.md)

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

# **save_auto**
> Delete1200Response save_auto(currency_code)

Add a currency with an automatically fetched exchange rate

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
    api_instance = tracko_sdk.UserCurrenciesApi(api_client)
    currency_code = 'currency_code_example' # str | 

    try:
        # Add a currency with an automatically fetched exchange rate
        api_response = api_instance.save_auto(currency_code)
        print("The response of UserCurrenciesApi->save_auto:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling UserCurrenciesApi->save_auto: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **currency_code** | **str**|  | 

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

