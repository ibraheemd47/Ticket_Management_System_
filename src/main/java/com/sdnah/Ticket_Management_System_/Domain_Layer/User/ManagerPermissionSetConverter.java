package com.sdnah.Ticket_Management_System_.Domain_Layer.User;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ManagerPermissionSetConverter
        implements AttributeConverter<Set<ManagerPermission>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<ManagerPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }
        return permissions.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<ManagerPermission> convertToEntityAttribute(String dbData) {
        Set<ManagerPermission> result = new HashSet<>();
        if (dbData == null || dbData.isBlank()) {
            return result;
        }
        for (String name : dbData.split(DELIMITER)) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                result.add(ManagerPermission.valueOf(trimmed));
            }
        }
        return result;
    }
}
