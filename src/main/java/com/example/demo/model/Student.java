package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String course;
    private String semester;

    private String enrollmentNumber;
    private String department;
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private AccountStatus status = AccountStatus.APPROVED;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Student(String name, String email, String course, String semester, String department, String phone, AccountStatus status) {
        this.name = name;
        this.email = email;
        this.course = course;
        this.semester = semester;
        this.department = department;
        this.phone = phone;
        this.status = status;
    }
}
