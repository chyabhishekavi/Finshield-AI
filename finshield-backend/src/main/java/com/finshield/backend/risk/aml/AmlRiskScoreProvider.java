package com.finshield.backend.risk.aml;

import com.finshield.backend.customer.domain.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AmlRiskScoreProvider {

    public BigDecimal score(Customer customer) {
        BigDecimal customerRisk = switch (customer.getCustomerRiskLevel()) {
            case LOW -> new BigDecimal("10.00");
            case MEDIUM -> new BigDecimal("40.00");
            case HIGH -> new BigDecimal("75.00");
            case CRITICAL -> new BigDecimal("100.00");
        };
        BigDecimal kycRisk = switch (customer.getKycStatus()) {
            case VERIFIED -> BigDecimal.ZERO;
            case PENDING -> new BigDecimal("60.00");
            case NOT_STARTED -> new BigDecimal("70.00");
            case EXPIRED -> new BigDecimal("85.00");
            case REJECTED -> new BigDecimal("100.00");
        };
        return customerRisk.max(kycRisk);
    }
}
