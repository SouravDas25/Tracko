package com.trako.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TransactionTypeConverter implements AttributeConverter<TransactionType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TransactionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public TransactionType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return TransactionType.fromValue(dbData);
    }
}
