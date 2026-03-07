# tracko_sdk.BudgetControllerApi

All URIs are relative to *http://localhost:8080*

Method | HTTP request | Description
------------- | ------------- | -------------
[**allocate_funds**](BudgetControllerApi.md#allocate_funds) | **POST** /api/budget/allocate | 
[**get_available_to_assign**](BudgetControllerApi.md#get_available_to_assign) | **GET** /api/budget/available | 
[**get_budget**](BudgetControllerApi.md#get_budget) | **GET** /api/budget | 
[**get_current_budget**](BudgetControllerApi.md#get_current_budget) | **GET** /api/budget/current | 


# **allocate_funds**
> object allocate_funds(budget_allocation_request_dto)



### Example


```python
import tracko_sdk
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO
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
    api_instance = tracko_sdk.BudgetControllerApi(api_client)
    budget_allocation_request_dto = tracko_sdk.BudgetAllocationRequestDTO() # BudgetAllocationRequestDTO | 

    try:
        api_response = api_instance.allocate_funds(budget_allocation_request_dto)
        print("The response of BudgetControllerApi->allocate_funds:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetControllerApi->allocate_funds: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **budget_allocation_request_dto** | [**BudgetAllocationRequestDTO**](BudgetAllocationRequestDTO.md)|  | 

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

# **get_available_to_assign**
> object get_available_to_assign(month=month, year=year)



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
    api_instance = tracko_sdk.BudgetControllerApi(api_client)
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)

    try:
        api_response = api_instance.get_available_to_assign(month=month, year=year)
        print("The response of BudgetControllerApi->get_available_to_assign:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetControllerApi->get_available_to_assign: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **month** | **int**|  | [optional] 
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

# **get_budget**
> object get_budget(month=month, year=year, category_id=category_id, include_actual=include_actual, sort_by=sort_by, sort_order=sort_order)



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
    api_instance = tracko_sdk.BudgetControllerApi(api_client)
    month = 56 # int |  (optional)
    year = 56 # int |  (optional)
    category_id = 56 # int |  (optional)
    include_actual = True # bool |  (optional) (default to True)
    sort_by = 'sort_by_example' # str |  (optional)
    sort_order = 'asc' # str |  (optional) (default to 'asc')

    try:
        api_response = api_instance.get_budget(month=month, year=year, category_id=category_id, include_actual=include_actual, sort_by=sort_by, sort_order=sort_order)
        print("The response of BudgetControllerApi->get_budget:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetControllerApi->get_budget: %s\n" % e)
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

# **get_current_budget**
> object get_current_budget()



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
    api_instance = tracko_sdk.BudgetControllerApi(api_client)

    try:
        api_response = api_instance.get_current_budget()
        print("The response of BudgetControllerApi->get_current_budget:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BudgetControllerApi->get_current_budget: %s\n" % e)
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

