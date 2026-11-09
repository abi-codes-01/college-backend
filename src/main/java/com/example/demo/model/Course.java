package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private Integer credits;
    private String department;
    private String instructor;
    private String semester;

    @Column(length = 1000)
    private String description;

    public Course() {}

    public Course(String code, String name, Integer credits, String department, String instructor) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.department = department;
        this.instructor = instructor;
    }

    public Course(String code, String name, Integer credits, String department, String instructor, String semester, String description) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.department = department;
        this.instructor = instructor;
        this.semester = semester;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
