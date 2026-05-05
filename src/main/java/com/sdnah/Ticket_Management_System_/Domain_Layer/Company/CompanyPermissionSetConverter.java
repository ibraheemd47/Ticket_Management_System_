package com.sdnah.Ticket_Management_System_.Domain_Layer.Company;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class CompanyPermissionSetConverter
        implements AttributeConverter<Set<CompanyPermission>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<CompanyPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "";
        }

        return permissions.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<CompanyPermission> convertToEntityAttribute(String dbData) {
        Set<CompanyPermission> result = new HashSet<>();

        if (dbData == null || dbData.isBlank()) {
            return result;
        }

        for (String name : dbData.split(DELIMITER)) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                result.add(CompanyPermission.valueOf(trimmed));
            }
        }

        return result;
    }
}