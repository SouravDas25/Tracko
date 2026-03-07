package com.trako.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionEntryTypeConverter implements AttributeConverter<TransactionEntryType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransactionEntryType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public TransactionEntryType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TransactionEntryType.fromValue(dbData);
    }
}
