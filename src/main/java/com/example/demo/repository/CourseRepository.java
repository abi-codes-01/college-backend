package com.example.demo.repository;

import com.example.demo.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByDepartmentIgnoreCase(String department);
    List<Course> findByInstructorIgnoreCase(String instructor);
    boolean existsByCode(String code);

    @Query("SELECT COALESCE(c.department, 'Unassigned'), COUNT(c) FROM Course c GROUP BY c.department")
    List<Object[]> countCoursesByDepartment();
}
