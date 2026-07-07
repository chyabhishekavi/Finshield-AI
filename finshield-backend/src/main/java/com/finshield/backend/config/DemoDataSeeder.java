package com.finshield.backend.config;

import com.finshield.backend.account.domain.Account;
import com.finshield.backend.account.domain.AccountStatus;
import com.finshield.backend.account.domain.AccountType;
import com.finshield.backend.account.repository.AccountRepository;
import com.finshield.backend.aml.domain.AmlListType;
import com.finshield.backend.aml.domain.AmlRiskCategory;
import com.finshield.backend.aml.domain.AmlWatchlistEntry;
import com.finshield.backend.aml.repository.AmlWatchlistEntryRepository;
import com.finshield.backend.beneficiary.domain.Beneficiary;
import com.finshield.backend.beneficiary.domain.BeneficiaryStatus;
import com.finshield.backend.beneficiary.repository.BeneficiaryRepository;
import com.finshield.backend.customer.domain.AnnualIncomeRange;
import com.finshield.backend.customer.domain.Customer;
import com.finshield.backend.customer.domain.CustomerRiskLevel;
import com.finshield.backend.customer.domain.KycStatus;
import com.finshield.backend.customer.repository.CustomerRepository;
import com.finshield.backend.device.domain.CustomerDevice;
import com.finshield.backend.device.domain.DeviceType;
import com.finshield.backend.device.repository.CustomerDeviceRepository;
import com.finshield.backend.user.domain.Role;
import com.finshield.backend.user.domain.RoleName;
import com.finshield.backend.user.domain.User;
import com.finshield.backend.user.domain.UserRole;
import com.finshield.backend.user.domain.UserStatus;
import com.finshield.backend.user.repository.RoleRepository;
import com.finshield.backend.user.repository.UserRepository;
import com.finshield.backend.user.repository.UserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@Profile({"dev", "docker"})
@Order(10)
public class DemoDataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final BeneficiaryRepository beneficiaries;
    private final CustomerDeviceRepository devices;
    private final AmlWatchlistEntryRepository watchlist;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, RoleRepository roles, UserRoleRepository userRoles,
                          CustomerRepository customers, AccountRepository accounts,
                          BeneficiaryRepository beneficiaries, CustomerDeviceRepository devices,
                          AmlWatchlistEntryRepository watchlist, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
        this.customers = customers;
        this.accounts = accounts;
        this.beneficiaries = beneficiaries;
        this.devices = devices;
        this.watchlist = watchlist;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedUser("FinShield Administrator", "admin@finshield.ai", "Admin@123", RoleName.ADMIN);
        seedUser("Fraud Analyst", "analyst@finshield.ai", "Analyst@123", RoleName.FRAUD_ANALYST);
        seedUser("AML Investigator", "aml@finshield.ai", "Aml@123", RoleName.AML_INVESTIGATOR);
        seedUser("Compliance Officer", "compliance@finshield.ai", "Compliance@123", RoleName.COMPLIANCE_OFFICER);

        Customer customer = customers.findByCustomerNumber("CUST1001").orElseGet(() -> customers.save(
                new Customer("CUST1001", "Rahul Sharma", LocalDate.of(1991, 8, 14),
                        "rahul.sharma@example.com", "+919876543210", KycStatus.VERIFIED,
                        CustomerRiskLevel.MEDIUM, "Software Engineer", AnnualIncomeRange.FROM_25K_TO_50K,
                        "India", "Pune", "PUNE-01")));

        if (!accounts.existsByAccountNumber("ACC1001")) {
            accounts.save(new Account("ACC1001", AccountType.SAVINGS, new BigDecimal("1200000"),
                    "INR", AccountStatus.ACTIVE, Instant.now().minus(900, ChronoUnit.DAYS), customer));
        }
        seedBeneficiary(customer, "Unknown Beneficiary", "ACC9999", "Test Bank", "TEST0001234",
                new BigDecimal("70"), BeneficiaryStatus.PENDING_VERIFICATION, Instant.now());
        seedBeneficiary(customer, "Priya Sharma", "ACC2002", "Trusted Bank", "TRST0001234",
                new BigDecimal("5"), BeneficiaryStatus.ACTIVE, Instant.now().minus(365, ChronoUnit.DAYS));
        seedDevice(customer, "DEVICE-NEW-001", DeviceType.MOBILE, "103.20.44.15", "Unknown", false,
                Instant.now());
        seedDevice(customer, "DEVICE-TRUSTED-001", DeviceType.MOBILE, "103.20.44.10", "Pune", true,
                Instant.now().minus(365, ChronoUnit.DAYS));

        if (!watchlist.existsByListTypeAndIdentifier(AmlListType.INTERNAL_BLACKLIST, "ACC9999")) {
            watchlist.save(new AmlWatchlistEntry("Unknown Beneficiary", "ACC9999", "India",
                    AmlListType.INTERNAL_BLACKLIST, AmlRiskCategory.HIGH, true));
        }
    }

    private void seedUser(String name, String email, String password, RoleName roleName) {
        User user = users.findByEmailIgnoreCase(email).orElseGet(() -> {
            User created = new User(name, email, passwordEncoder.encode(password));
            created.changeStatus(UserStatus.ACTIVE);
            return users.save(created);
        });
        if (!userRoles.existsByUserIdAndRoleName(user.getId(), roleName)) {
            Role role = roles.findByName(roleName).orElseThrow();
            userRoles.save(new UserRole(user, role));
        }
    }

    private void seedBeneficiary(Customer customer, String name, String account, String bank, String ifsc,
                                 BigDecimal risk, BeneficiaryStatus status, Instant addedAt) {
        if (!beneficiaries.existsByCustomerIdAndBeneficiaryAccountNumber(customer.getId(), account)) {
            beneficiaries.save(new Beneficiary(customer, name, account, bank, ifsc, addedAt, risk, status));
        }
    }

    private void seedDevice(Customer customer, String id, DeviceType type, String ip, String location,
                            boolean trusted, Instant firstSeen) {
        if (!devices.existsByCustomerIdAndDeviceId(customer.getId(), id)) {
            devices.save(new CustomerDevice(customer, id, type, ip, location, trusted, firstSeen));
        }
    }
}
