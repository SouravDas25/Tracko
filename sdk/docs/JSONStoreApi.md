# tracko_sdk.JSONStoreApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**create4**](JSONStoreApi.md#create4) | **POST** /api/json-store | Create a JSON store entry
[**delete4**](JSONStoreApi.md#delete4) | **DELETE** /api/json-store/{name} | Delete a JSON store entry by name
[**get_all4**](JSONStoreApi.md#get_all4) | **GET** /api/json-store | List all JSON store entries
[**get_by_name**](JSONStoreApi.md#get_by_name) | **GET** /api/json-store/{name} | Get a JSON store entry by name
[**update2**](JSONStoreApi.md#update2) | **PUT** /api/json-store/{name} | Update a JSON store entry by name


# **create4**
> GetByName200Response create4(json_store)

Create a JSON store entry

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_name200_response import GetByName200Response
from tracko_sdk.models.json_store import JsonStore
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
    api_instance = tracko_sdk.JSONStoreApi(api_client)
    json_store = tracko_sdk.JsonStore() # JsonStore | 

    try:
        # Create a JSON store entry
        api_response = api_instance.create4(json_store)
        print("The response of JSONStoreApi->create4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JSONStoreApi->create4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **json_store** | [**JsonStore**](JsonStore.md)|  | 

### Return type

[**GetByName200Response**](GetByName200Response.md)

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

# **delete4**
> Delete1200Response delete4(name)

Delete a JSON store entry by name

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
    api_instance = tracko_sdk.JSONStoreApi(api_client)
    name = 'name_example' # str | 

    try:
        # Delete a JSON store entry by name
        api_response = api_instance.delete4(name)
        print("The response of JSONStoreApi->delete4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JSONStoreApi->delete4: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 

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

# **get_all4**
> GetAll4200Response get_all4()

List all JSON store entries

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_all4200_response import GetAll4200Response
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
    api_instance = tracko_sdk.JSONStoreApi(api_client)

    try:
        # List all JSON store entries
        api_response = api_instance.get_all4()
        print("The response of JSONStoreApi->get_all4:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JSONStoreApi->get_all4: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**GetAll4200Response**](GetAll4200Response.md)

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

# **get_by_name**
> GetByName200Response get_by_name(name)

Get a JSON store entry by name

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_name200_response import GetByName200Response
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
    api_instance = tracko_sdk.JSONStoreApi(api_client)
    name = 'name_example' # str | 

    try:
        # Get a JSON store entry by name
        api_response = api_instance.get_by_name(name)
        print("The response of JSONStoreApi->get_by_name:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JSONStoreApi->get_by_name: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 

### Return type

[**GetByName200Response**](GetByName200Response.md)

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

# **update2**
> GetByName200Response update2(name, json_store)

Update a JSON store entry by name

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.get_by_name200_response import GetByName200Response
from tracko_sdk.models.json_store import JsonStore
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
    api_instance = tracko_sdk.JSONStoreApi(api_client)
    name = 'name_example' # str | 
    json_store = tracko_sdk.JsonStore() # JsonStore | 

    try:
        # Update a JSON store entry by name
        api_response = api_instance.update2(name, json_store)
        print("The response of JSONStoreApi->update2:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling JSONStoreApi->update2: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **name** | **str**|  | 
 **json_store** | [**JsonStore**](JsonStore.md)|  | 

### Return type

[**GetByName200Response**](GetByName200Response.md)

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

