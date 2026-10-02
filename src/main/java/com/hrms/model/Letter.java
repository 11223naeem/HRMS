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
public class Letter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private LetterType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDate issuedOn;
}
