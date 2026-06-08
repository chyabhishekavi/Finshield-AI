package com.finshield.backend.risk.ml;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.fraud.api.RuleEvaluationResult;
import com.finshield.backend.risk.velocity.VelocityMetrics;
import com.finshield.backend.transaction.domain.Transaction;
import com.finshield.backend.transaction.event.TransactionEvent;

import java.math.BigDecimal;

public record MlFraudScoringContext(
        TransactionEvent event,
        Transaction transaction,
        Customer customer,
        Account account,
        CustomerDevice device,
        Beneficiary beneficiary,
        VelocityMetrics velocityMetrics,
        RuleEvaluationResult ruleEvaluation,
        BigDecimal customerRiskScore,
        BigDecimal amlRiskScore
) {
}
