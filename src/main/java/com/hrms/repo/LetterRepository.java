package com.hrms.repo;

import com.hrms.model.Employee;
import com.hrms.model.Letter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LetterRepository extends JpaRepository<Letter, Long> {
    List<Letter> findByEmployeeOrderByIdDesc(Employee employee);
    List<Letter> findTop5ByEmployeeOrderByIdDesc(Employee employee);
    List<Letter> findAllByOrderByIdDesc();
}
