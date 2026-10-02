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
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Employee employee;

    private LocalDate fromDate;
    private LocalDate toDate;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    /** Day the leave was approved/rejected; salary deduction happens in this month. */
    private LocalDate decidedOn;
}
