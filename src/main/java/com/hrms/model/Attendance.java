package com.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "att_day"}))
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Employee employee;

    @Column(name = "att_day")
    private LocalDate day;

    private LocalDateTime loginTime;   // check-in
    private LocalDateTime logoutTime;  // check-out
    private Double faceScoreIn;        // match distance at check-in (audit)
    private Double faceScoreOut;       // match distance at check-out (audit)
}
