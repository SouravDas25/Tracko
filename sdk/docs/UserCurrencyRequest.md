# UserCurrencyRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**currency_code** | **str** |  | 
**exchange_rate** | **float** |  | 

## Example

```python
from tracko_sdk.models.user_currency_request import UserCurrencyRequest

# TODO update the JSON string below
json = "{}"
# create an instance of UserCurrencyRequest from a JSON string
user_currency_request_instance = UserCurrencyRequest.from_json(json)
# print the JSON string representation of the object
print(UserCurrencyRequest.to_json())

# convert the object into a dict
user_currency_request_dict = user_currency_request_instance.to_dict()
# create an instance of UserCurrencyRequest from a dict
user_currency_request_form_dict = user_currency_request.from_dict(user_currency_request_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


