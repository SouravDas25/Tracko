# tracko_sdk.AuthenticationApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**login**](AuthenticationApi.md#login) | **POST** /api/login | Log in with username and password
[**sign_in**](AuthenticationApi.md#sign_in) | **POST** /api/oauth/token | Sign in with phone number and password


# **login**
> JwtResponse login(login_request)

Log in with username and password

### Example


```python
import tracko_sdk
from tracko_sdk.models.jwt_response import JwtResponse
from tracko_sdk.models.login_request import LoginRequest
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
    api_instance = tracko_sdk.AuthenticationApi(api_client)
    login_request = tracko_sdk.LoginRequest() # LoginRequest | 

    try:
        # Log in with username and password
        api_response = api_instance.login(login_request)
        print("The response of AuthenticationApi->login:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AuthenticationApi->login: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **login_request** | [**LoginRequest**](LoginRequest.md)|  | 

### Return type

[**JwtResponse**](JwtResponse.md)

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

# **sign_in**
> JwtResponse sign_in(authication_request)

Sign in with phone number and password

### Example


```python
import tracko_sdk
from tracko_sdk.models.authication_request import AuthicationRequest
from tracko_sdk.models.jwt_response import JwtResponse
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
    api_instance = tracko_sdk.AuthenticationApi(api_client)
    authication_request = tracko_sdk.AuthicationRequest() # AuthicationRequest | 

    try:
        # Sign in with phone number and password
        api_response = api_instance.sign_in(authication_request)
        print("The response of AuthenticationApi->sign_in:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling AuthenticationApi->sign_in: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **authication_request** | [**AuthicationRequest**](AuthicationRequest.md)|  | 

### Return type

[**JwtResponse**](JwtResponse.md)

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

