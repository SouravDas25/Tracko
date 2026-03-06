package com.trako.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RecurringTransactionTypeConverter implements AttributeConverter<RecurringTransactionType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RecurringTransactionType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public RecurringTransactionType convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return null;
        }
        return RecurringTransactionType.fromValue(dbData);
    }
}
