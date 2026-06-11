package com.ecommerce.mobile.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Employee;
import java.util.Optional;

@Repository
public interface EmployessRepository extends JpaRepository<Employee, Long>{
    Optional<Employee> findByEmail (String email);

    @Query("select e from Employee e where type(e) = Employee order by coalesce(e.hireDate, e.createdAt) desc")
    List<Employee> findAllStaff();

    @Query("select e from Employee e where e.userID = :id and type(e) = Employee")
    Optional<Employee> findStaffById(@Param("id") Long id);

    @Query("SELECT e FROM Employee e WHERE " +
           "type(e) = Employee AND " +
           "(:isActive IS NULL OR e.isActive = :isActive) AND " +
           "(:start IS NULL OR coalesce(e.hireDate, e.createdAt) >= :start) AND " +
           "(:end IS NULL OR coalesce(e.hireDate, e.createdAt) <= :end) AND " +
           "(:search IS NULL OR :search = '' OR lower(e.fullName) LIKE lower(concat('%', :search, '%')) OR lower(e.email) LIKE lower(concat('%', :search, '%'))) " +
           "ORDER BY coalesce(e.hireDate, e.createdAt) DESC")
    List<Employee> filterEmployees(@Param("isActive") Boolean isActive, 
                                   @Param("start") java.time.LocalDateTime start, 
                                   @Param("end") java.time.LocalDateTime end,
                                   @Param("search") String search);
}


