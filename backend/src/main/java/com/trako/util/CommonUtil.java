package com.trako.util;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommonUtil {

    // ObjectMapper for parsing JSON arrays of accountIds
    private static final com.fasterxml.jackson.databind.ObjectMapper ACCOUNT_ID_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * Parses a string that may contain account IDs as a JSON array (e.g. "[1, 2, 3]")
     * or as comma-separated values (e.g. "1,2,3" or " 1 , 2, 3 ").
     * Returns an empty list on null/blank input or malformed JSON and silently ignores
     * elements that cannot be parsed as Long.
     */
    public static List<Long> parseAccountIds(String accountIdsStr) {
        java.util.List<Long> out = new java.util.ArrayList<>();
        if (accountIdsStr == null || accountIdsStr.trim().isEmpty()) {
            return out;
        }
        
        String trimmed = accountIdsStr.trim();
        
        // First, try to parse as a JSON array
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = ACCOUNT_ID_MAPPER.readTree(trimmed);
                if (node != null && node.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode elem : node) {
                        try {
                            if (elem.isNumber()) {
                                out.add(elem.longValue());
                            } else if (elem.isTextual()) {
                                String s = elem.asText();
                                if (s != null && !s.trim().isEmpty()) {
                                    out.add(Long.parseLong(s.trim()));
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                return out;
            } catch (Exception ignored) {
            }
        }
        
        // If JSON parse failed, try to parse as comma-separated values
        String[] parts = trimmed.split(",");
        for (String part : parts) {
            try {
                String s = part.trim();
                if (!s.isEmpty()) {
                    out.add(Long.parseLong(s));
                }
            } catch (Exception ignored) {
            }
        }
        
        return out;
    }

    private static final ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setPropertyCondition(Conditions.isNotNull())
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static <D, T> D mapModel(final T subject, Class<D> tClass) {
        return modelMapper.map(subject, tClass);
    }

    public static <D, T> List<D> mapModels(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream()
                .map(entity -> mapModel(entity, outCLass))
                .collect(Collectors.toList());
    }

    public static <S, D> D mapModel(final S source, D destination) {
        modelMapper.map(source, destination);
        return destination;
    }

    public static String extractPhoneNumber(String phoneNumber) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phoneNumber.length(); i++) {
            if (Character.isDigit(phoneNumber.charAt(i))) {
                sb.append(phoneNumber.charAt(i));
            }
        }
        if (sb.length() < 10) {
            return null;
        }
        return sb.substring(sb.length() - 10, sb.length());
    }
}
