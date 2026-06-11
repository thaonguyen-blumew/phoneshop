package com.ecommerce.mobile.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.mobile.entity.Employee;
import com.ecommerce.mobile.entity.Manager;
import com.ecommerce.mobile.entity.Role;
import com.ecommerce.mobile.entity.Voucher;
import com.ecommerce.mobile.enums.VoucherDiscountType;
import com.ecommerce.mobile.repository.EmployessRepository;
import com.ecommerce.mobile.repository.ManagerRepository;
import com.ecommerce.mobile.repository.RoleRepository;
import com.ecommerce.mobile.repository.VoucherRepository;

@Configuration
@ConditionalOnProperty(name = "phoneshop.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeederConfig {

    private Role ensureRole(RoleRepository roleRepository, String roleName) {
        return roleRepository.findByNameRole(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setNameRole(roleName);
                    return roleRepository.save(role);
                });
    }

    @Bean
    CommandLineRunner seedInternalAccounts(RoleRepository roleRepository,
                                           EmployessRepository employessRepository,
                                           ManagerRepository managerRepository,
                                           VoucherRepository voucherRepository,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            Role managerRole = ensureRole(roleRepository, "MANAGER");
            Role employeeRole = ensureRole(roleRepository, "EMPLOYEE");
            ensureRole(roleRepository, "CUSTOMER");

            if (managerRepository.findByEmail("manager1@gmail.com").isEmpty()) {
                Manager manager = new Manager();
                manager.setEmail("manager1@gmail.com");
                manager.setHashPassword(passwordEncoder.encode("123456"));
                manager.setFullName("Manager 1");
                manager.setPhone("0900000001");
                manager.setIsActive(true);
                manager.setRole(managerRole);
                manager.setSalary(new BigDecimal("20000000"));
                manager.setHireDate(LocalDateTime.now());
                managerRepository.save(manager);
            }

            if (employessRepository.findByEmail("employee1@gmail.com").isEmpty()) {
                Employee employee = new Employee();
                employee.setEmail("employee1@gmail.com");
                employee.setHashPassword(passwordEncoder.encode("123456"));
                employee.setFullName("Employee 1");
                employee.setPhone("0900000002");
                employee.setIsActive(true);
                employee.setRole(employeeRole);
                employee.setSalary(new BigDecimal("10000000"));
                employee.setHireDate(LocalDateTime.now());
                employessRepository.save(employee);
            }

            seedVoucherIfMissing(voucherRepository, "WELCOME10", VoucherDiscountType.PERCENT,
                    new BigDecimal("10"), new BigDecimal("100000"), new BigDecimal("1000000"),
                    100, "Giảm 10% tối đa 100.000đ");
            seedVoucherIfMissing(voucherRepository, "FLAT50K", VoucherDiscountType.FIXED,
                    new BigDecimal("50000"), null, new BigDecimal("300000"),
                    100, "Giảm trực tiếp 50.000đ");
            seedVoucherIfMissing(voucherRepository, "TECH15", VoucherDiscountType.PERCENT,
                    new BigDecimal("15"), new BigDecimal("150000"), new BigDecimal("1500000"),
                    100, "Giảm 15% tối đa 150.000đ");
        };
    }

    private void seedVoucherIfMissing(VoucherRepository voucherRepository,
                                      String code,
                                      VoucherDiscountType discountType,
                                      BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount,
                                      BigDecimal minOrderAmount,
                                      Integer remainingQuantity,
                                      String description) {
        if (voucherRepository.findByCodeIgnoreCase(code).isPresent()) {
            return;
        }

        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDescription(description);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMaxDiscountAmount(maxDiscountAmount);
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setRemainingQuantity(remainingQuantity);
        voucher.setIsActive(true);
        voucherRepository.save(voucher);
    }
}
