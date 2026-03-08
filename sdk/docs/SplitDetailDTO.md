# SplitDetailDTO


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**split** | [**Split**](Split.md) |  | [optional] 
**contact** | [**Contact**](Contact.md) |  | [optional] 

## Example

```python
from tracko_sdk.models.split_detail_dto import SplitDetailDTO

# TODO update the JSON string below
json = "{}"
# create an instance of SplitDetailDTO from a JSON string
split_detail_dto_instance = SplitDetailDTO.from_json(json)
# print the JSON string representation of the object
print(SplitDetailDTO.to_json())

# convert the object into a dict
split_detail_dto_dict = split_detail_dto_instance.to_dict()
# create an instance of SplitDetailDTO from a dict
split_detail_dto_form_dict = split_detail_dto.from_dict(split_detail_dto_dict)
```
[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


