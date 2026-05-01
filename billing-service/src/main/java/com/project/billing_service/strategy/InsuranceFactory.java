package com.project.billing_service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InsuranceFactory {
    private static final Logger log = LoggerFactory.getLogger(InsuranceFactory.class);

    private final SgkStrategy sgkStrategy;
    private final AllianzStrategy allianzStrategy;
    private final NoInsuranceStrategy noInsuranceStrategy;

    public InsuranceFactory(SgkStrategy sgkStrategy, AllianzStrategy allianzStrategy, NoInsuranceStrategy noInsuranceStrategy) {
        this.sgkStrategy = sgkStrategy;
        this.allianzStrategy = allianzStrategy;
        this.noInsuranceStrategy = noInsuranceStrategy;
    }

    /**
     * Resolves the insurance calculation strategy for a given appointment.
     *
     * Resolution order:
     *  1. providerType == NONE   → NoInsurance (patient pays full amount)
     *  2. providerType == SGK    → SgkStrategy (80% insurance / 20% patient)
     *  3. providerName == allianz (case-insensitive) → AllianzStrategy
     *  4. providerType == PRIVATE → NoInsurance (self-pay, no insurance split)
     *  5. Any other / null        → NoInsurance (safe default, logged as warning)
     */
    public InsuranceStrategy getStrategy(String providerType, String providerName) {
        if (providerType != null && providerType.equalsIgnoreCase("NONE")) {
            return noInsuranceStrategy;
        }
        if (providerType != null && providerType.equalsIgnoreCase("SGK")) {
            return sgkStrategy;
        }
        if (providerName != null && providerName.equalsIgnoreCase("allianz")) {
            return allianzStrategy;
        }
        if (providerType != null && providerType.equalsIgnoreCase("PRIVATE")) {
            // PRIVATE = self-pay; patient covers the full amount, no insurance split
            return noInsuranceStrategy;
        }

        // Unknown provider type — default to no insurance and log for visibility
        log.warn("Unrecognized insurance providerType='{}' providerName='{}'. Defaulting to NoInsuranceStrategy.",
                providerType, providerName);
        return noInsuranceStrategy;
    }
}
