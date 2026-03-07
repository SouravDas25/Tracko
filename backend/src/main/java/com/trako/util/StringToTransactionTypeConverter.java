package com.trako.util;

import com.trako.enums.TransactionType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTransactionTypeConverter implements Converter<String, TransactionType> {

    @Override
    public TransactionType convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(source);
            return TransactionType.fromValue(value);
        } catch (NumberFormatException e) {
            // Fallback to name-based resolution if desired, or rethrow
            try {
                return TransactionType.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid transaction type: " + source);
            }
        }
    }
}
