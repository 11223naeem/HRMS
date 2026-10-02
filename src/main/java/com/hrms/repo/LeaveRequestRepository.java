package com.hrms.repo;

import com.hrms.model.Employee;
import com.hrms.model.LeaveRequest;
import com.hrms.model.LeaveStatus;
import com.hrms.model.Role;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeOrderByIdDesc(Employee employee);
    List<LeaveRequest> findByEmployeeAndStatus(Employee employee, LeaveStatus status);
    List<LeaveRequest> findByStatusAndEmployeeRoleOrderByIdAsc(LeaveStatus status, Role role);
    long countByStatusAndEmployeeRole(LeaveStatus status, Role role);

    @Query("""
    SELECT l FROM LeaveRequest l
    WHERE l.employee = :employee
      AND l.status IN :statuses
      AND l.fromDate <= :toDate
      AND l.toDate >= :fromDate
""")
List<LeaveRequest> findOverlappingLeaves(
        @Param("employee") Employee employee,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("statuses") List<LeaveStatus> statuses
);
}
