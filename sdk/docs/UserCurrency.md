# UserCurrency


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **int** |  | [optional] 
**currency_code** | **str** |  | 
**exchange_rate** | **float** |  | 

## Example

```python
from tracko_sdk.models.user_currency import UserCurrency

# TODO update the JSON string below
json = "{}"
# create an instance of UserCurrency from a JSON string
user_currency_instance = UserCurrency.from_json(json)
# print the JSON string representation of the object
print(UserCurrency.to_json())

# convert the object into a dict
user_currency_dict = user_currency_instance.to_dict()
# create an instance of UserCurrency from a dict
user_currency_form_dict = user_currency.from_dict(user_currency_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


