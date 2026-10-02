package com.hrms.config;

import com.hrms.model.Employee;
import com.hrms.model.Role;
import com.hrms.repo.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInit {
    private final EmployeeRepository repo;
    private final PasswordEncoder encoder;

    @Value("${admin.email}")
    private String email;
    @Value("${admin.password}")
    private String password;

    @PostConstruct
    public void init() {
        if (repo.existsByEmailIgnoreCase(email)) return;
        Employee a = new Employee();
        a.setName("Administrator");
        a.setEmail(email.toLowerCase());
        a.setPassword(encoder.encode(password));
        a.setRole(Role.ADMIN);
        a.setDesignation("HR Admin");
        a.setJoinDate(LocalDate.now());
        repo.save(a);
    }
}
