package com.hrms.repo;

import com.hrms.model.Attendance;
import com.hrms.model.Employee;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByEmployeeAndDay(Employee employee, LocalDate day);
    List<Attendance> findByEmployee(Employee employee);
    List<Attendance> findTop31ByEmployeeOrderByDayDesc(Employee employee);
}
