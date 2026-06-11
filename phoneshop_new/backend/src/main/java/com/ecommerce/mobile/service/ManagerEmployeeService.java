package com.ecommerce.mobile.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.mobile.entity.Employee;
import com.ecommerce.mobile.entity.Role;
import com.ecommerce.mobile.repository.EmployessRepository;
import com.ecommerce.mobile.repository.RoleRepository;

@Service
public class ManagerEmployeeService {

    private final EmployessRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagerEmployeeService(EmployessRepository employeeRepository,
                                  RoleRepository roleRepository,
                                  PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAllStaff();
    }

    @Transactional(readOnly = true)
    public List<Employee> getFilteredEmployees(Boolean isActive, LocalDateTime start, LocalDateTime end, String search) {
        return employeeRepository.filterEmployees(isActive, start, end, search);
    }

    @Transactional(readOnly = true)
    public Employee getEmployee(Long id) {
        return employeeRepository.findStaffById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
    }

    @Transactional
    public Employee createEmployee(String email,
                                   String password,
                                   String fullName,
                                   String phone,
                                   BigDecimal salary,
                                   Boolean active) {
        validateCreateOrUpdate(email, password, fullName);
        if (employeeRepository.findByEmail(email.trim()).isPresent()) {
            throw new RuntimeException("Email nhân viên đã tồn tại");
        }

        Role employeeRole = roleRepository.findByNameRole("EMPLOYEE")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role EMPLOYEE"));

        Employee employee = new Employee();
        employee.setEmail(email.trim());
        employee.setHashPassword(passwordEncoder.encode(password));
        employee.setFullName(fullName.trim());
        employee.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        employee.setSalary(salary == null ? BigDecimal.ZERO : salary);
        employee.setHireDate(LocalDateTime.now());
        employee.setIsActive(active == null || active);
        employee.setRole(employeeRole);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long employeeId,
                                   String email,
                                   String fullName,
                                   String phone,
                                   BigDecimal salary,
                                   Boolean active,
                                   String password) {
        // Fetch directly from repository (not via self-invoked getEmployee which has readOnly=true)
        // to ensure Hibernate dirty checking is active in this read-write transaction.
        Employee employee = employeeRepository.findStaffById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        // Cập nhật email nếu có thay đổi
        if (StringUtils.hasText(email)) {
            String newEmail = email.trim();
            if (!newEmail.equalsIgnoreCase(employee.getEmail())) {
                // Kiểm tra email mới chưa bị dùng bởi tài khoản khác
                employeeRepository.findByEmail(newEmail).ifPresent(existing -> {
                    if (!existing.getUserID().equals(employeeId)) {
                        throw new RuntimeException("Email \"" + newEmail + "\" đã được sử dụng bởi tài khoản khác");
                    }
                });
                employee.setEmail(newEmail);
            }
        }
        if (StringUtils.hasText(fullName)) {
            employee.setFullName(fullName.trim());
        }
        employee.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        if (salary != null) {
            employee.setSalary(salary);
        }
        if (active != null) {
            employee.setIsActive(active);
        }
        if (StringUtils.hasText(password)) {
            employee.setHashPassword(passwordEncoder.encode(password));
        }
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee setActive(Long employeeId, boolean active) {
        Employee employee = employeeRepository.findStaffById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        employee.setIsActive(active);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee resetPassword(Long employeeId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new RuntimeException("Mật khẩu mới không được để trống");
        }
        Employee employee = employeeRepository.findStaffById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        employee.setHashPassword(passwordEncoder.encode(newPassword.trim()));
        return employeeRepository.save(employee);
    }

    private void validateCreateOrUpdate(String email, String password, String fullName) {
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("Email không được để trống");
        }
        if (!StringUtils.hasText(password)) {
            throw new RuntimeException("Mật khẩu không được để trống");
        }
        if (!StringUtils.hasText(fullName)) {
            throw new RuntimeException("Họ và tên không được để trống");
        }
    }
}
