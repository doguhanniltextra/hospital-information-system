package com.project.patient_service.model;

import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.project.patient_service.security.CryptoConverter;

@Embeddable
public class InsuranceInfo {

    @Enumerated(EnumType.STRING)
    private InsuranceProviderType providerType;
    private String providerName;
    @Convert(converter = CryptoConverter.class)
    private String policyNumber;

    public InsuranceProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(InsuranceProviderType providerType) {
        this.providerType = providerType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }
}
