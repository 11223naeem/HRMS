package com.hrms.repo;

import com.hrms.model.Employee;
import com.hrms.model.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<Employee> findByRoleInOrderByNameAsc(Collection<Role> roles);
    long countByRole(Role role);
}
