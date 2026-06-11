package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.enums.FeedbackStatus;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByCustomerUserIDOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByEmployeeUserIDOrderByCreatedAtDesc(Long employeeId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<Feedback> findByFeedbackIdAndCustomerUserID(Long feedbackId, Long customerId);

    @EntityGraph(attributePaths = {"customer", "employee"})
    Optional<Feedback> findByFeedbackIdAndEmployeeUserID(Long feedbackId, Long employeeId);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM Feedback f WHERE " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:start IS NULL OR f.createdAt >= :start) AND " +
           "(:end IS NULL OR f.createdAt <= :end) AND " +
           "(:search IS NULL OR :search = '' OR lower(f.content) LIKE lower(concat('%', :search, '%')) OR lower(f.resolution) LIKE lower(concat('%', :search, '%')) OR lower(f.customer.fullName) LIKE lower(concat('%', :search, '%'))) " +
           "ORDER BY f.createdAt DESC")
    List<Feedback> filterFeedbacks(@org.springframework.data.repository.query.Param("status") com.ecommerce.mobile.enums.FeedbackStatus status, 
                                   @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
                                   @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
                                   @org.springframework.data.repository.query.Param("search") String search);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM Feedback f WHERE " +
           "f.employee.userID = :employeeId AND " +
           "(:status IS NULL OR f.status = :status) AND " +
           "(:start IS NULL OR f.createdAt >= :start) AND " +
           "(:end IS NULL OR f.createdAt <= :end) AND " +
           "(:search IS NULL OR :search = '' OR lower(f.content) LIKE lower(concat('%', :search, '%')) OR lower(f.resolution) LIKE lower(concat('%', :search, '%')) OR lower(f.customer.fullName) LIKE lower(concat('%', :search, '%'))) " +
           "ORDER BY f.createdAt DESC")
    List<Feedback> filterEmployeeFeedbacks(@org.springframework.data.repository.query.Param("employeeId") Long employeeId,
                                           @org.springframework.data.repository.query.Param("status") com.ecommerce.mobile.enums.FeedbackStatus status, 
                                           @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start, 
                                           @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
                                           @org.springframework.data.repository.query.Param("search") String search);
}
