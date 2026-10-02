package com.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password; // BCrypt hash

    @Enumerated(EnumType.STRING)
    private Role role = Role.EMPLOYEE;

    private String designation;
    private LocalDate joinDate = LocalDate.now();
    private LocalDate exitDate;

    /** JSON array of 3-5 face vectors, each 128 numbers. */
   @Column(length = 100000)
private String faceDescriptors;

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isManager() { return role == Role.MANAGER; }
    public boolean isEmployee() { return role == Role.EMPLOYEE; }
    public boolean isExited() {
    return !isAdmin()
            && exitDate != null
            && !LocalDate.now().isBefore(exitDate);
}
}
