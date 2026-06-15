package com.finshield.backend.fraud.engine;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.fraud.api.RuleEvaluationResult;
import com.finshield.backend.risk.velocity.VelocityMetrics;
import com.finshield.backend.transaction.domain.Transaction;

public interface FraudRuleEvaluationEngine {

    RuleEvaluationResult evaluate(
            Transaction transaction,
            Customer customer,
            Account account,
            CustomerDevice device,
            Beneficiary beneficiary,
            VelocityMetrics velocityMetrics
    );
}
