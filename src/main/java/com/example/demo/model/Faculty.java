package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "faculty")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String designation;
    private String department;
    private String qualification;
    private String specialization;
    private String office;
    private String phone;

    @Column(length = 1000)
    private String bio;

    public Faculty() {}

    public Faculty(String name, String email, String designation, String department, String qualification, String specialization, String office, String phone, String bio) {
        this.name = name;
        this.email = email;
        this.designation = designation;
        this.department = department;
        this.qualification = qualification;
        this.specialization = specialization;
        this.office = office;
        this.phone = phone;
        this.bio = bio;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
