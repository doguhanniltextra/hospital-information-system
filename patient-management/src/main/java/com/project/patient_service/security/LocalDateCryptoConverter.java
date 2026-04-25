package com.project.patient_service.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Converter
public class LocalDateCryptoConverter implements AttributeConverter<LocalDate, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(@Lazy EncryptionService encryptionService) {
        LocalDateCryptoConverter.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (encryptionService == null || attribute == null) return null;
        return encryptionService.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (encryptionService == null || dbData == null) return null;
        String decrypted = encryptionService.decrypt(dbData);
        return decrypted != null ? LocalDate.parse(decrypted) : null;
    }
}
