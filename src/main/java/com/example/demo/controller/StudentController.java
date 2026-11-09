package com.example.demo.controller;

import com.example.demo.model.AccountStatus;
import com.example.demo.model.Student;
import com.example.demo.model.User;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentController(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Student> getPendingStudents() {
        return studentRepository.findByStatus(AccountStatus.PENDING);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
    public Student createStudent(@RequestBody Student student) {
        if (student.getStatus() == null) {
            student.setStatus(AccountStatus.APPROVED);
        }
        return studentRepository.save(student);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Student> approveStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        student.setStatus(AccountStatus.APPROVED);

        if (student.getUser() != null) {
            User user = student.getUser();
            user.setStatus(AccountStatus.APPROVED);
            userRepository.save(user);
        } else if (student.getEmail() != null) {
            userRepository.findByEmail(student.getEmail()).ifPresent(u -> {
                u.setStatus(AccountStatus.APPROVED);
                userRepository.save(u);
            });
        }

        return ResponseEntity.ok(studentRepository.save(student));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Student> rejectStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with id: " + id));
        student.setStatus(AccountStatus.REJECTED);

        if (student.getUser() != null) {
            User user = student.getUser();
            user.setStatus(AccountStatus.REJECTED);
            userRepository.save(user);
        } else if (student.getEmail() != null) {
            userRepository.findByEmail(student.getEmail()).ifPresent(u -> {
                u.setStatus(AccountStatus.REJECTED);
                userRepository.save(u);
            });
        }

        return ResponseEntity.ok(studentRepository.save(student));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStudent(@PathVariable Long id) {
        studentRepository.deleteById(id);
    }
}
