package com.example.demo.repository;

import com.example.demo.model.AccountStatus;
import com.example.demo.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByStatus(AccountStatus status);
    List<Student> findByDepartmentIgnoreCase(String department);
    long countByStatus(AccountStatus status);

    @Query("SELECT COALESCE(s.department, 'Unassigned'), COUNT(s) FROM Student s GROUP BY s.department")
    List<Object[]> countStudentsByDepartment();
}
