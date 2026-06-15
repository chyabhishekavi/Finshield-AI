package com.finshield.backend.fraud.engine;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.risk.velocity.VelocityMetrics;
import com.finshield.backend.transaction.domain.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

@Component
public class RuleFactExtractor {

    public Map<String, Object> extract(
            Transaction transaction,
            Customer customer,
            Account account,
            CustomerDevice device,
            Beneficiary beneficiary,
            VelocityMetrics velocityMetrics
    ) {
        Map<String, Object> facts = new HashMap<>();

        facts.put("amount", transaction.getAmount());
        facts.put("transaction.amount", transaction.getAmount());
        facts.put("transaction.currency", transaction.getCurrency());
        facts.put("transaction.type", transaction.getTransactionType().name());
        facts.put("transaction.channel", transaction.getChannel().name());
        facts.put("transaction.status", transaction.getStatus().name());
        facts.put("transaction.initiatedAt", transaction.getInitiatedAt());
        facts.put("transaction.count.5m", velocityMetrics.transactionCount5Minutes());
        facts.put("transaction.amount.total.1h", velocityMetrics.totalAmount1Hour());
        facts.put("beneficiary.count.24h", velocityMetrics.distinctBeneficiaries24Hours());
        facts.put("device.suspiciousAttempts.24h", velocityMetrics.failedOrSuspiciousDeviceAttempts24Hours());

        facts.put("customer.riskLevel", customer.getCustomerRiskLevel().name());
        facts.put("customer.kycStatus", customer.getKycStatus().name());
        facts.put("customer.country", customer.getCountry());
        facts.put("customer.city", customer.getCity());

        facts.put("account.type", account.getAccountType().name());
        facts.put("account.status", account.getStatus().name());
        facts.put("account.balance", account.getBalance());
        facts.put("account.currency", account.getCurrency());
        facts.put("account.amountToBalanceRatio", amountToBalanceRatio(transaction, account));

        facts.put("device.new", device == null || !device.isTrusted()
                || Duration.between(device.getFirstSeenAt(), transaction.getInitiatedAt()).toHours() < 24);
        facts.put("device.present", device != null);
        facts.put("device.trusted", device != null && device.isTrusted());
        facts.put("device.type", device == null ? null : device.getDeviceType().name());
        facts.put("device.ipMatches", device != null
                && transaction.getIpAddress() != null
                && transaction.getIpAddress().equals(device.getIpAddress()));

        facts.put("beneficiary.firstTime", beneficiary == null
                || Duration.between(beneficiary.getAddedAt(), transaction.getInitiatedAt()).toHours() < 24);
        facts.put("beneficiary.status", beneficiary == null ? null : beneficiary.getStatus().name());
        facts.put("beneficiary.riskScore", beneficiary == null ? null : beneficiary.getRiskScore());
        facts.put("beneficiary.ageHours", beneficiary == null
                ? null
                : Duration.between(beneficiary.getAddedAt(), transaction.getInitiatedAt()).toHours());

        facts.put("geoLocation.matchesProfile", geoMatchesProfile(transaction, customer));
        return Collections.unmodifiableMap(new HashMap<>(facts));
    }

    private BigDecimal amountToBalanceRatio(Transaction transaction, Account account) {
        if (account.getBalance().signum() <= 0
                || !account.getCurrency().equals(transaction.getCurrency())) {
            return BigDecimal.ZERO;
        }
        return transaction.getAmount().divide(account.getBalance(), 4, RoundingMode.HALF_UP);
    }

    private Boolean geoMatchesProfile(Transaction transaction, Customer customer) {
        if (transaction.getGeoLocation() == null || transaction.getGeoLocation().isBlank()) {
            return null;
        }
        String location = transaction.getGeoLocation().toLowerCase(Locale.ROOT);
        return location.contains(customer.getCity().toLowerCase(Locale.ROOT))
                || location.contains(customer.getCountry().toLowerCase(Locale.ROOT));
    }
}
