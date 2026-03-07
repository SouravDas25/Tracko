package com.trako.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionEntryTypeConverter implements AttributeConverter<TransactionDbType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransactionDbType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public TransactionDbType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TransactionDbType.fromValue(dbData);
    }
}
