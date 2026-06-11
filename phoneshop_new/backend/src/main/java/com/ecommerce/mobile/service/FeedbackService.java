package com.ecommerce.mobile.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.entity.Employee;
import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.enums.FeedbackStatus;
import com.ecommerce.mobile.repository.CustomerRepository;
import com.ecommerce.mobile.repository.EmployessRepository;
import com.ecommerce.mobile.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final EmployessRepository employeesRepository;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           CustomerService customerService,
                           CustomerRepository customerRepository,
                           EmployessRepository employeesRepository) {
        this.feedbackRepository = feedbackRepository;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.employeesRepository = employeesRepository;
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFeedbacksForCustomer(String customerEmail) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return feedbackRepository.findByCustomerUserIDOrderByCreatedAtDesc(customer.getUserID());
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFeedbacksForEmployee(String employeeEmail) {
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        return feedbackRepository.findByEmployeeUserIDOrderByCreatedAtDesc(employee.getUserID());
    }

    @Transactional(readOnly = true)
    public List<Feedback> getPendingFeedbacks() {
        return feedbackRepository.findByStatusOrderByCreatedAtDesc(FeedbackStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFilteredFeedbacks(com.ecommerce.mobile.enums.FeedbackStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end, String search) {
        return feedbackRepository.filterFeedbacks(status, start, end, search);
    }

    @Transactional(readOnly = true)
    public List<Feedback> getFilteredEmployeeFeedbacks(String employeeEmail, com.ecommerce.mobile.enums.FeedbackStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end, String search) {
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        return feedbackRepository.filterEmployeeFeedbacks(employee.getUserID(), status, start, end, search);
    }

    @Transactional(readOnly = true)
    public Feedback getFeedbackForCustomer(String customerEmail, Long feedbackId) {
        Customer customer = customerService.requireCustomerByEmail(customerEmail);
        return feedbackRepository.findByFeedbackIdAndCustomerUserID(feedbackId, customer.getUserID())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
    }

    @Transactional(readOnly = true)
    public Feedback getFeedbackForEmployee(String employeeEmail, Long feedbackId) {
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        return feedbackRepository.findByFeedbackIdAndEmployeeUserID(feedbackId, employee.getUserID())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
    }

    @Transactional(readOnly = true)
    public Feedback viewFeedbackForEmployee(String employeeEmail, Long feedbackId) {
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
        if (feedback.getEmployee() != null && !feedback.getEmployee().getUserID().equals(employee.getUserID())) {
            throw new RuntimeException("Phản hồi này đang do nhân viên khác xử lý");
        }
        return feedback;
    }

    @Transactional
    public Feedback createFeedback(String customerEmail, String content) {
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("Nội dung phản hồi không được để trống");
        }
        Customer customer = customerService.requireCustomerByEmail(customerEmail);

        Feedback feedback = new Feedback();
        feedback.setCustomer(customer);
        feedback.setContent(content.trim());
        feedback.setStatus(FeedbackStatus.PENDING);
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback assignToMe(String employeeEmail, Long feedbackId) {
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
        if (feedback.getStatus() == FeedbackStatus.RESOLVED) {
            throw new RuntimeException("Phản hồi đã được xử lý");
        }
        feedback.setEmployee(employee);
        if (feedback.getStatus() == FeedbackStatus.PENDING) {
            feedback.setStatus(FeedbackStatus.IN_PROGRESS);
        }
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback resolveFeedback(String employeeEmail, Long feedbackId, String resolution) {
        if (!StringUtils.hasText(resolution)) {
            throw new RuntimeException("Nội dung xử lý không được để trống");
        }
        Employee employee = employeesRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phản hồi"));
        if (feedback.getEmployee() != null && !feedback.getEmployee().getUserID().equals(employee.getUserID())) {
            throw new RuntimeException("Phản hồi này đang do nhân viên khác xử lý");
        }

        feedback.setEmployee(employee);
        feedback.setResolution(resolution.trim());
        feedback.setStatus(FeedbackStatus.RESOLVED);
        feedback.setResolvedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }
}
