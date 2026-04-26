package com.mnco.infrastructure.persistence.converter;

import com.mnco.domain.entities.LabTemplateStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter for LabTemplateStatus enum to handle PostgreSQL enum type.
 * Converts between the Java enum and the PostgreSQL 'lab_template_status' enum type.
 */
@Converter(autoApply = true)
public class LabTemplateStatusConverter implements AttributeConverter<LabTemplateStatus, String> {

    @Override
    public String convertToDatabaseColumn(LabTemplateStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public LabTemplateStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return LabTemplateStatus.valueOf(dbData);
    }
}