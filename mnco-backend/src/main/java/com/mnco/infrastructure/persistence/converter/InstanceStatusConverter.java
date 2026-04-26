package com.mnco.infrastructure.persistence.converter;

import com.mnco.domain.entities.InstanceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for InstanceStatus enum to handle PostgreSQL enum type.
 * Converts between the Java enum and the PostgreSQL 'instance_status' enum type.
 */
@Converter(autoApply = true)
public class InstanceStatusConverter implements AttributeConverter<InstanceStatus, String> {

    @Override
    public String convertToDatabaseColumn(InstanceStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public InstanceStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return InstanceStatus.valueOf(dbData);
    }
}