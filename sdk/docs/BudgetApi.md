# tracko_sdk.BudgetApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**allocate_funds**](BudgetApi.md#allocate_funds) | **POST** /api/budget/allocate | Allocate funds to a category for a month
[**get_available_to_assign**](BudgetApi.md#get_available_to_assign) | **GET** /api/budget/available | Get the amount available to assign for a month
[**get_budget**](BudgetApi.md#get_budget) | **GET** /api/budget | Get budget details for a month/year
[**get_current_budget**](BudgetApi.md#get_current_budget) | **GET** /api/budget/current | Get budget for the current month


# **allocate_funds**
> BudgetCategoryDTO allocate_funds(budget_allocation_request_dto)

Allocate funds to a category for a month

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO
from tracko_sdk.models.budget_category_dto import BudgetCategoryDTO
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
    api_instance = tracko_sdk.BudgetApi(api_client)
    budget_allocation_request_dto = tracko_sdk.BudgetAllocationRequestDTO() # BudgetAllocationRequestDTO | 

    try:
        # Allocate funds to a category for a month
        api_response = api_instance.allocate_funds(budget_allocation_request_dto)
        print("The response of BudgetApi->allocate_funds:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetApi->allocate_funds: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **budget_allocation_request_dto** | [**BudgetAllocationRequestDTO**](BudgetAllocationRequestDTO.md)|  | 

### Return type

[**BudgetCategoryDTO**](BudgetCategoryDTO.md)

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

# **get_available_to_assign**
> float get_available_to_assign(month=month, year=year)

Get the amount available to assign for a month

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
    api_instance = tracko_sdk.BudgetApi(api_client)
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)

    try:
        # Get the amount available to assign for a month
        api_response = api_instance.get_available_to_assign(month=month, year=year)
        print("The response of BudgetApi->get_available_to_assign:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetApi->get_available_to_assign: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **month** | **int**|  | [optional] 
 **year** | **int**|  | [optional] 

### Return type

**float**

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

# **get_budget**
> BudgetResponseDTO get_budget(month=month, year=year, category_id=category_id, include_actual=include_actual, sort_by=sort_by, sort_order=sort_order)

Get budget details for a month/year

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.budget_response_dto import BudgetResponseDTO
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
    api_instance = tracko_sdk.BudgetApi(api_client)
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)
    category_id = 56 # int |  (optional)
    include_actual = True # bool |  (optional) (default to True)
    sort_by = 'sort_by_example' # str |  (optional)
    sort_order = 'asc' # str |  (optional) (default to 'asc')

    try:
        # Get budget details for a month/year
        api_response = api_instance.get_budget(month=month, year=year, category_id=category_id, include_actual=include_actual, sort_by=sort_by, sort_order=sort_order)
        print("The response of BudgetApi->get_budget:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetApi->get_budget: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **month** | **int**|  | [optional] 
 **year** | **int**|  | [optional] 
 **category_id** | **int**|  | [optional] 
 **include_actual** | **bool**|  | [optional] [default to True]
 **sort_by** | **str**|  | [optional] 
 **sort_order** | **str**|  | [optional] [default to &#39;asc&#39;]

### Return type

[**BudgetResponseDTO**](BudgetResponseDTO.md)

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

# **get_current_budget**
> BudgetResponseDTO get_current_budget()

Get budget for the current month

### Example

* Bearer (JWT) Authentication (bearerAuth):

```python
import tracko_sdk
from tracko_sdk.models.budget_response_dto import BudgetResponseDTO
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
    api_instance = tracko_sdk.BudgetApi(api_client)

    try:
        # Get budget for the current month
        api_response = api_instance.get_current_budget()
        print("The response of BudgetApi->get_current_budget:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetApi->get_current_budget: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**BudgetResponseDTO**](BudgetResponseDTO.md)

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

