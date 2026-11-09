package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminDashboardController(
            StudentRepository studentRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            NoticeRepository noticeRepository,
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            MarkRepository markRepository,
            PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================================
    // 1. REAL-TIME SYSTEM STATISTICS (JPA count() & GROUP BY aggregations)
    // =========================================================================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        long totalStudents = studentRepository.count();
        long pendingApprovals = studentRepository.countByStatus(AccountStatus.PENDING);
        long approvedStudents = studentRepository.countByStatus(AccountStatus.APPROVED);
        long rejectedStudents = studentRepository.countByStatus(AccountStatus.REJECTED);
        long totalFaculty = facultyRepository.count();
        long totalCourses = courseRepository.count();
        long totalDepartments = departmentRepository.count();
        long totalNotices = noticeRepository.count();

        Map<String, Long> studentCountsByDept = new LinkedHashMap<>();
        for (Object[] row : studentRepository.countStudentsByDepartment()) {
            String deptName = row[0] != null ? row[0].toString() : "Unassigned";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            studentCountsByDept.put(deptName, count);
        }

        Map<String, Long> courseCountsByDept = new LinkedHashMap<>();
        for (Object[] row : courseRepository.countCoursesByDepartment()) {
            String deptName = row[0] != null ? row[0].toString() : "Unassigned";
            Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            courseCountsByDept.put(deptName, count);
        }

        List<Department> allDepts = departmentRepository.findAll();
        List<Map<String, Object>> departmentDistribution = new ArrayList<>();
        Set<String> seenDepts = new HashSet<>();

        for (Department d : allDepts) {
            seenDepts.add(d.getName());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", d.getCode());
            item.put("department", d.getName());
            item.put("students", studentCountsByDept.getOrDefault(d.getName(), 0L));
            item.put("courses", courseCountsByDept.getOrDefault(d.getName(), 0L));
            departmentDistribution.add(item);
        }

        for (Map.Entry<String, Long> entry : studentCountsByDept.entrySet()) {
            if (!seenDepts.contains(entry.getKey())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", entry.getKey().length() > 6 ? entry.getKey().substring(0, 6).toUpperCase() : entry.getKey().toUpperCase());
                item.put("department", entry.getKey());
                item.put("students", entry.getValue());
                item.put("courses", courseCountsByDept.getOrDefault(entry.getKey(), 0L));
                departmentDistribution.add(item);
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("pendingApprovals", pendingApprovals);
        stats.put("approvedStudents", approvedStudents);
        stats.put("rejectedStudents", rejectedStudents);
        stats.put("totalFaculty", totalFaculty);
        stats.put("totalCourses", totalCourses);
        stats.put("totalDepartments", totalDepartments);
        stats.put("totalNotices", totalNotices);
        stats.put("departmentDistribution", departmentDistribution);

        return ResponseEntity.ok(stats);
    }

    // =========================================================================
    // 2. STUDENT MANAGEMENT & ACADEMIC RECORD INSPECTION
    // =========================================================================
    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status) {
        List<Student> students = studentRepository.findAll();

        if (StringUtils.hasText(department) && !"All".equalsIgnoreCase(department)) {
            students = students.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(department))
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(status) && !"All".equalsIgnoreCase(status)) {
            students = students.stream()
                    .filter(s -> {
                        String st = s.getStatus() != null ? s.getStatus().name() : "APPROVED";
                        return st.equalsIgnoreCase(status);
                    })
                    .collect(Collectors.toList());
        }

        if (StringUtils.hasText(search)) {
            String q = search.trim().toLowerCase();
            students = students.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(q))
                            || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q))
                            || (s.getEnrollmentNumber() != null && s.getEnrollmentNumber().toLowerCase().contains(q))
                            || (s.getCourse() != null && s.getCourse().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(students);
    }

    @GetMapping("/students/pending")
    public ResponseEntity<List<Student>> getPendingStudents() {
        return ResponseEntity.ok(studentRepository.findByStatus(AccountStatus.PENDING));
    }

    @PutMapping("/students/{id}/approve")
    @Transactional
    public ResponseEntity<Student> approveStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setStatus(AccountStatus.APPROVED);

        if (student.getUser() != null) {
            User u = student.getUser();
            u.setStatus(AccountStatus.APPROVED);
            userRepository.save(u);
        } else if (student.getEmail() != null) {
            userRepository.findByEmail(student.getEmail()).ifPresent(u -> {
                u.setStatus(AccountStatus.APPROVED);
                userRepository.save(u);
            });
        }
        return ResponseEntity.ok(studentRepository.save(student));
    }

    @PutMapping("/students/{id}/reject")
    @Transactional
    public ResponseEntity<Student> rejectStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        student.setStatus(AccountStatus.REJECTED);

        if (student.getUser() != null) {
            User u = student.getUser();
            u.setStatus(AccountStatus.REJECTED);
            userRepository.save(u);
        } else if (student.getEmail() != null) {
            userRepository.findByEmail(student.getEmail()).ifPresent(u -> {
                u.setStatus(AccountStatus.REJECTED);
                userRepository.save(u);
            });
        }
        return ResponseEntity.ok(studentRepository.save(student));
    }

    @DeleteMapping("/students/{id}")
    @Transactional
    public ResponseEntity<?> deleteStudent(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        attendanceRepository.deleteAll(attendanceRepository.findByStudentId(id));
        markRepository.deleteAll(markRepository.findByStudentId(id));
        studentRepository.delete(student);
        return ResponseEntity.ok(new MessageResponse("Student record deleted successfully."));
    }

    @GetMapping("/students/{id}/academic-record")
    public ResponseEntity<Map<String, Object>> getStudentAcademicRecord(@PathVariable Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<Attendance> rawAttendance = attendanceRepository.findByStudentId(id);
        Map<Long, Map<String, Object>> byCourse = new LinkedHashMap<>();
        int totalAttended = 0;
        int totalClasses = 0;

        for (Attendance rec : rawAttendance) {
            Course c = rec.getCourse();
            if (c == null) continue;
            int att = rec.getAttendedClasses() != null ? rec.getAttendedClasses() : (Boolean.TRUE.equals(rec.getPresent()) ? 1 : 0);
            int tot = rec.getTotalClasses() != null && rec.getTotalClasses() > 0 ? rec.getTotalClasses() : 1;
            totalAttended += att;
            totalClasses += tot;

            Map<String, Object> item = byCourse.computeIfAbsent(c.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courseCode", c.getCode());
                m.put("courseName", c.getName());
                m.put("attendedClasses", 0);
                m.put("totalClasses", 0);
                return m;
            });
            item.put("attendedClasses", ((Integer) item.get("attendedClasses")) + att);
            item.put("totalClasses", ((Integer) item.get("totalClasses")) + tot);
        }

        List<Map<String, Object>> attendanceList = new ArrayList<>();
        for (Map<String, Object> entry : byCourse.values()) {
            int a = (Integer) entry.get("attendedClasses");
            int t = (Integer) entry.get("totalClasses");
            entry.put("percentage", t > 0 ? Math.round((a * 1000.0) / t) / 10.0 : 0.0);
            attendanceList.add(entry);
        }

        double overallPct = totalClasses > 0 ? Math.round((totalAttended * 1000.0) / totalClasses) / 10.0 : 0.0;

        List<Map<String, Object>> marksList = markRepository.findByStudentId(id).stream().map(m -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", m.getId());
            dto.put("courseCode", m.getCourse() != null ? m.getCourse().getCode() : "N/A");
            dto.put("courseName", m.getCourse() != null ? m.getCourse().getName() : "N/A");
            dto.put("examType", m.getExamType());
            dto.put("marksObtained", m.getMarksObtained());
            dto.put("maxMarks", m.getMaxMarks());
            dto.put("grade", m.getGrade());
            dto.put("semester", m.getSemester());
            dto.put("publishedDate", m.getPublishedDate());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("student", student);
        response.put("overallAttendancePercentage", overallPct);
        response.put("totalAttended", totalAttended);
        response.put("totalClasses", totalClasses);
        response.put("attendanceBySubject", attendanceList);
        response.put("marks", marksList);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 3. FACULTY MANAGEMENT & COURSE ASSIGNMENT
    // =========================================================================
    @GetMapping("/faculty")
    public ResponseEntity<List<Faculty>> getAllFaculty() {
        return ResponseEntity.ok(facultyRepository.findAll());
    }

    @PostMapping("/faculty")
    @Transactional
    public ResponseEntity<?> registerFaculty(@RequestBody Map<String, Object> payload) {
        String name = payload.getOrDefault("name", "").toString().trim();
        String email = payload.getOrDefault("email", "").toString().trim();
        if (!StringUtils.hasText(name) || !StringUtils.hasText(email)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Faculty name and email are required."));
        }
        if (facultyRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(new MessageResponse("A faculty member with this email already exists."));
        }

        Faculty faculty = new Faculty(
                name,
                email,
                payload.getOrDefault("designation", "Assistant Professor").toString(),
                payload.getOrDefault("department", "Computer Science & Engineering").toString(),
                payload.getOrDefault("qualification", "Ph.D.").toString(),
                payload.getOrDefault("specialization", "").toString(),
                payload.getOrDefault("office", "").toString(),
                payload.getOrDefault("phone", "").toString(),
                payload.getOrDefault("bio", "").toString()
        );
        Faculty savedFaculty = facultyRepository.save(faculty);

        // Also provision a ROLE_FACULTY User login if username is provided or derived from email
        String rawUsername = payload.get("username") != null && StringUtils.hasText(payload.get("username").toString())
                ? payload.get("username").toString().trim()
                : email.split("@")[0].replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
        String rawPassword = payload.get("password") != null && StringUtils.hasText(payload.get("password").toString())
                ? payload.get("password").toString()
                : "faculty123";

        if (StringUtils.hasText(rawUsername) && !userRepository.existsByUsername(rawUsername) && !userRepository.existsByEmail(email)) {
            userRepository.save(new User(
                    rawUsername,
                    email,
                    passwordEncoder.encode(rawPassword),
                    Role.ROLE_FACULTY,
                    AccountStatus.APPROVED,
                    name
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(savedFaculty);
    }

    @PutMapping("/faculty/{id}/assign-course")
    @Transactional
    public ResponseEntity<?> assignCourseToFaculty(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Faculty member not found."));

        if (payload.get("courseId") == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("courseId is required."));
        }
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        course.setInstructor(faculty.getName());
        courseRepository.save(course);

        return ResponseEntity.ok(
            new MessageResponse("Assigned " + faculty.getName() + " to course " + course.getCode() + " (" + course.getName() + ").")
        );
    }

    @DeleteMapping("/faculty/{id}")
    public ResponseEntity<?> deleteFaculty(@PathVariable Long id) {
        facultyRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Faculty member removed."));
    }

    // =========================================================================
    // 4. ACADEMIC MANAGEMENT (DEPARTMENT & COURSE CRUD)
    // =========================================================================
    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        if (!StringUtils.hasText(department.getCode()) || !StringUtils.hasText(department.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Department code and name are required."));
        }
        if (departmentRepository.existsByCode(department.getCode().trim())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Department code already exists."));
        }
        department.setCode(department.getCode().trim().toUpperCase());
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentRepository.save(department));
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department updated) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found."));
        if (StringUtils.hasText(updated.getCode())) dept.setCode(updated.getCode().trim().toUpperCase());
        if (StringUtils.hasText(updated.getName())) dept.setName(updated.getName().trim());
        dept.setDescription(updated.getDescription());
        dept.setHeadOfDepartment(updated.getHeadOfDepartment());
        dept.setBuilding(updated.getBuilding());
        dept.setEstablishedYear(updated.getEstablishedYear());
        dept.setTotalSeats(updated.getTotalSeats());
        return ResponseEntity.ok(departmentRepository.save(dept));
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        departmentRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Department deleted."));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseRepository.findAll());
    }

    @PostMapping("/courses")
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        if (!StringUtils.hasText(course.getCode()) || !StringUtils.hasText(course.getName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Course code and name are required."));
        }
        if (courseRepository.existsByCode(course.getCode().trim())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Course code already exists."));
        }
        course.setCode(course.getCode().trim().toUpperCase());
        return ResponseEntity.status(HttpStatus.CREATED).body(courseRepository.save(course));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course updated) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if (StringUtils.hasText(updated.getCode())) course.setCode(updated.getCode().trim().toUpperCase());
        if (StringUtils.hasText(updated.getName())) course.setName(updated.getName().trim());
        course.setCredits(updated.getCredits());
        course.setDepartment(updated.getDepartment());
        course.setInstructor(updated.getInstructor());
        course.setSemester(updated.getSemester());
        course.setDescription(updated.getDescription());
        return ResponseEntity.ok(courseRepository.save(course));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Course deleted."));
    }

    // =========================================================================
    // 5. CAMPUS NOTICE MANAGEMENT
    // =========================================================================
    @GetMapping("/notices")
    public ResponseEntity<List<Notice>> getAllNotices() {
        return ResponseEntity.ok(noticeRepository.findAllByOrderByIdDesc());
    }

    @PostMapping("/notices")
    public ResponseEntity<Notice> createNotice(@RequestBody Notice notice) {
        if (notice.getDate() == null) {
            notice.setDate(java.time.LocalDate.now());
        }
        if (notice.getIsPublic() == null) {
            notice.setIsPublic(true);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(noticeRepository.save(notice));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<?> deleteNotice(@PathVariable Long id) {
        noticeRepository.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Notice deleted."));
    }
}
