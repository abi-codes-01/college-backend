package com.example.demo.controller;

import com.example.demo.model.AccountStatus;
import com.example.demo.model.Course;
import com.example.demo.model.Department;
import com.example.demo.model.Faculty;
import com.example.demo.model.Notice;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.FacultyRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.service.CourseService;
import com.example.demo.service.DepartmentService;
import com.example.demo.service.FacultyService;
import com.example.demo.service.NoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final DepartmentService departmentService;
    private final FacultyService facultyService;
    private final CourseService courseService;
    private final NoticeService noticeService;
    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;

    public PublicController(
            DepartmentService departmentService,
            FacultyService facultyService,
            CourseService courseService,
            NoticeService noticeService,
            DepartmentRepository departmentRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository) {
        this.departmentService = departmentService;
        this.facultyService = facultyService;
        this.courseService = courseService;
        this.noticeService = noticeService;
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
    }

    @GetMapping("/departments")
    public List<Department> getPublicDepartments() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/faculty")
    public List<Faculty> getPublicFaculty(@RequestParam(required = false) String department) {
        return facultyService.getAllFaculty(department);
    }

    @GetMapping("/courses")
    public List<Course> getPublicCourses(@RequestParam(required = false) String department) {
        return courseService.getAllCourses(department);
    }

    @GetMapping("/notices")
    public List<Notice> getPublicNotices() {
        return noticeService.getPublicNotices();
    }

    @GetMapping("/stats")
    public Map<String, Long> getPublicStats() {
        Map<String, Long> stats = new HashMap<>();
        long approvedStudents = studentRepository.countByStatus(AccountStatus.APPROVED);
        stats.put("departments", departmentRepository.count());
        stats.put("faculty", facultyRepository.count());
        stats.put("courses", courseRepository.count());
        stats.put("students", approvedStudents > 0 ? approvedStudents : studentRepository.count());
        stats.put("notices", (long) noticeService.getPublicNotices().size());
        return stats;
    }
}
