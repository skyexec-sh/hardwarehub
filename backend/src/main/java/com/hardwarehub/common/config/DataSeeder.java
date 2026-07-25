package com.hardwarehub.common.config;

import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.customer.domain.Customer;
import com.hardwarehub.customer.domain.CustomerStatus;
import com.hardwarehub.customer.repository.CustomerRepository;
import com.hardwarehub.pricing.domain.PriceLevel;
import com.hardwarehub.pricing.repository.PriceLevelRepository;
import com.hardwarehub.pricing.service.PricingService;
import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.domain.User;
import com.hardwarehub.user.repository.RoleRepository;
import com.hardwarehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CustomerRepository customerRepository;
    private final PriceLevelRepository priceLevelRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedOwnerIfMissing();
        seedDemoPriceLevelCustomers();
    }

    private void seedOwnerIfMissing() {
        if (userRepository.findByUsernameAndDeletedAtIsNull("owner").isPresent()) {
            return;
        }

        User owner = new User();
        owner.setUsername("owner");
        owner.setEmail("owner@hardwarehub.local");
        owner.setPasswordHash(passwordEncoder.encode("Owner@123"));
        owner.setFirstName("Store");
        owner.setLastName("Owner");
        owner.setActive(true);
        owner.setCreatedBy("system");
        owner.setUpdatedBy("system");
        owner.setRoles(Set.of(
                roleRepository.findByName(RoleName.OWNER)
                        .orElseThrow(() -> new IllegalStateException("OWNER role missing"))
        ));
        userRepository.save(owner);
        auditService.log("SEED", "USER", String.valueOf(owner.getId()), "Default owner user created");
        log.info("Seeded default owner user (username=owner)");
    }

    private void seedDemoPriceLevelCustomers() {
        PriceLevel retail = requireLevel(PricingService.RETAIL_CODE);
        PriceLevel contractor = requireLevel("CONTRACTOR");
        PriceLevel vip = requireLevel("VIP");

        seedCustomerIfMissing(
                "DEMO-RETAIL",
                "Santos Hardware Walk-in Co.",
                "Ana Santos",
                "09171234501",
                "ana.santos@demo.local",
                "12 Mabini St, Quezon City",
                "Quezon City",
                "Metro Manila",
                new BigDecimal("25000.00"),
                retail,
                "Demo Retail customer for POS pricing tests");

        seedCustomerIfMissing(
                "DEMO-CONTRACTOR",
                "Reyes Builders & Construction",
                "Miguel Reyes",
                "09181234502",
                "miguel.reyes@demo.local",
                "45 Industrial Ave, Valenzuela",
                "Valenzuela",
                "Metro Manila",
                new BigDecimal("150000.00"),
                contractor,
                "Demo Contractor customer for trade pricing tests");

        seedCustomerIfMissing(
                "DEMO-VIP",
                "Dela Cruz Premier Estates",
                "Catherine Dela Cruz",
                "09191234503",
                "catherine.dc@demo.local",
                "88 Ayala Avenue, Makati",
                "Makati",
                "Metro Manila",
                new BigDecimal("500000.00"),
                vip,
                "Demo VIP customer for preferred pricing tests");
    }

    private void seedCustomerIfMissing(
            String code,
            String businessName,
            String contactPerson,
            String phone,
            String email,
            String address,
            String city,
            String province,
            BigDecimal creditLimit,
            PriceLevel priceLevel,
            String notes) {
        if (customerRepository.existsByCustomerCodeIgnoreCaseAndDeletedAtIsNull(code)) {
            return;
        }

        Customer customer = new Customer();
        customer.setCustomerCode(code);
        customer.setBusinessName(businessName);
        customer.setContactPerson(contactPerson);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);
        customer.setCity(city);
        customer.setProvince(province);
        customer.setCreditLimit(creditLimit);
        customer.setOutstandingBalance(BigDecimal.ZERO);
        customer.setPriceLevel(priceLevel);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setNotes(notes);
        customer.setCreatedBy("system");
        customer.setUpdatedBy("system");

        Customer saved = customerRepository.save(customer);
        auditService.log(
                "SEED",
                "CUSTOMER",
                String.valueOf(saved.getId()),
                "Demo customer " + code + " (" + priceLevel.getCode() + ")");
        log.info("Seeded demo customer {} with price level {}", code, priceLevel.getCode());
    }

    private PriceLevel requireLevel(String code) {
        return priceLevelRepository
                .findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalStateException("Price level missing: " + code));
    }
}
