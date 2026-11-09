package com.example.demo.controller;

import com.example.demo.dto.MessageResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('FACULTY')")
public class TeacherDashboardController {

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

    public TeacherDashboardController(
            UserRepository userRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            MarkRepository markRepository) {
        this.userRepository = userRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
    }

    /**
     * Extracts the authenticated Teacher/Faculty identity strictly from SecurityContextHolder.
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found."));
    }

    private Faculty resolveAuthenticatedFaculty(User user) {
        return facultyRepository.findByEmail(user.getEmail())
                .orElseGet(() -> {
                    String displayName = StringUtils.hasText(user.getFullName()) ? user.getFullName() : user.getUsername();
                    return facultyRepository.findAll().stream()
                            .filter(f -> f.getName() != null && f.getName().equalsIgnoreCase(displayName))
                            .findFirst()
                            .orElse(null);
                });
    }

    private List<Course> resolveAssignedCourses(User user, Faculty faculty) {
        String instructorName = faculty != null ? faculty.getName() : user.getFullName();
        List<Course> assigned = new ArrayList<>();
        if (StringUtils.hasText(instructorName)) {
            assigned = courseRepository.findByInstructorIgnoreCase(instructorName);
        }
        if (assigned.isEmpty() && faculty != null && StringUtils.hasText(faculty.getDepartment())) {
            assigned = courseRepository.findByDepartmentIgnoreCase(faculty.getDepartment());
        }
        if (assigned.isEmpty()) {
            assigned = courseRepository.findAll();
        }
        return assigned;
    }

    private List<Student> resolveEnrolledStudentsForCourse(Course course) {
        List<Student> approvedStudents = studentRepository.findAll().stream()
                .filter(s -> s.getStatus() == null || s.getStatus() == AccountStatus.APPROVED)
                .collect(Collectors.toList());

        if (course != null && StringUtils.hasText(course.getDepartment())) {
            List<Student> deptMatched = approvedStudents.stream()
                    .filter(s -> s.getDepartment() != null && s.getDepartment().equalsIgnoreCase(course.getDepartment()))
                    .collect(Collectors.toList());
            if (!deptMatched.isEmpty()) {
                return deptMatched;
            }
        }
        return approvedStudents;
    }

    @GetMapping("/my-profile")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        User user = getAuthenticatedUser();
        Faculty faculty = resolveAuthenticatedFaculty(user);
        List<Course> myCourses = resolveAssignedCourses(user, faculty);

        Set<Long> uniqueStudentIds = new HashSet<>();
        for (Course c : myCourses) {
            resolveEnrolledStudentsForCourse(c).forEach(s -> uniqueStudentIds.add(s.getId()));
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("username", user.getUsername());
        profile.put("name", faculty != null ? faculty.getName() : (user.getFullName() != null ? user.getFullName() : user.getUsername()));
        profile.put("email", faculty != null ? faculty.getEmail() : user.getEmail());
        profile.put("designation", faculty != null ? faculty.getDesignation() : "Faculty Member");
        profile.put("department", faculty != null ? faculty.getDepartment() : "Computer Science & Engineering");
        profile.put("qualification", faculty != null ? faculty.getQualification() : "Ph.D.");
        profile.put("specialization", faculty != null ? faculty.getSpecialization() : "Academic Research");
        profile.put("office", faculty != null ? faculty.getOffice() : "Faculty Block");
        profile.put("phone", faculty != null ? faculty.getPhone() : "N/A");
        profile.put("assignedCoursesCount", myCourses.size());
        profile.put("enrolledStudentsCount", uniqueStudentIds.size());

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/my-courses")
    public ResponseEntity<List<Course>> getMyAssignedCourses() {
        User user = getAuthenticatedUser();
        Faculty faculty = resolveAuthenticatedFaculty(user);
        return ResponseEntity.ok(resolveAssignedCourses(user, faculty));
    }

    @GetMapping("/courses/{courseId}/students")
    public ResponseEntity<List<Student>> getEnrolledStudents(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        return ResponseEntity.ok(resolveEnrolledStudentsForCourse(course));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/attendance")
    @Transactional
    public ResponseEntity<?> submitBulkAttendance(@RequestBody Map<String, Object> payload) {
        User user = getAuthenticatedUser();
        Faculty faculty = resolveAuthenticatedFaculty(user);

        if (payload.get("courseId") == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Course ID is required."));
        }
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        LocalDate attendanceDate = payload.get("date") != null && StringUtils.hasText(payload.get("date").toString())
                ? LocalDate.parse(payload.get("date").toString())
                : LocalDate.now();

        List<Map<String, Object>> records = (List<Map<String, Object>>) payload.get("records");
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("At least one student attendance record is required."));
        }

        // Validation: Prevent duplicate attendance submissions for the same student, subject, and date
        for (Map<String, Object> item : records) {
            Long studentId = Long.valueOf(item.get("studentId").toString());
            if (attendanceRepository.existsByStudentIdAndCourseIdAndDate(studentId, courseId, attendanceDate)) {
                Student dupStudent = studentRepository.findById(studentId).orElse(null);
                String studentName = dupStudent != null ? dupStudent.getName() : ("ID " + studentId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new MessageResponse(
                        "Duplicate submission blocked: Attendance has already been recorded for " +
                        studentName + " in " + course.getCode() + " on " + attendanceDate + "."
                    )
                );
            }
        }

        List<Attendance> toSave = new ArrayList<>();
        for (Map<String, Object> item : records) {
            Long studentId = Long.valueOf(item.get("studentId").toString());
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));
            boolean present = item.get("present") == null || Boolean.parseBoolean(item.get("present").toString());
            String remarks = item.get("remarks") != null ? item.get("remarks").toString() : (present ? "Present" : "Absent");

            Attendance att = new Attendance(
                    student,
                    course,
                    faculty,
                    attendanceDate,
                    present,
                    present ? 1 : 0,
                    1,
                    remarks
            );
            toSave.add(att);
        }

        attendanceRepository.saveAll(toSave);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            new MessageResponse("Attendance submitted successfully for " + toSave.size() + " student(s) in " + course.getCode() + " (" + attendanceDate + ").")
        );
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/marks")
    @Transactional
    public ResponseEntity<?> publishAssessmentMarks(@RequestBody Map<String, Object> payload) {
        User user = getAuthenticatedUser();
        Faculty faculty = resolveAuthenticatedFaculty(user);

        if (payload.get("courseId") == null) {
            return ResponseEntity.badRequest().body(new MessageResponse("Course ID is required."));
        }
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));

        String examType = payload.get("examType") != null && StringUtils.hasText(payload.get("examType").toString())
                ? payload.get("examType").toString().trim()
                : "Midterm";
        Double maxMarks = payload.get("maxMarks") != null
                ? Double.valueOf(payload.get("maxMarks").toString())
                : 100.0;
        String semester = payload.get("semester") != null && StringUtils.hasText(payload.get("semester").toString())
                ? payload.get("semester").toString()
                : (course.getSemester() != null ? course.getSemester() : "Semester 5");

        List<Map<String, Object>> records = (List<Map<String, Object>>) payload.get("records");
        if (records == null || records.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("No student marks provided to publish."));
        }

        int publishedCount = 0;
        for (Map<String, Object> item : records) {
            if (item.get("marksObtained") == null || item.get("marksObtained").toString().isBlank()) {
                continue;
            }
            Long studentId = Long.valueOf(item.get("studentId").toString());
            Double marksObtained = Double.valueOf(item.get("marksObtained").toString());

            if (marksObtained < 0 || marksObtained > maxMarks) {
                return ResponseEntity.badRequest().body(
                    new MessageResponse("Marks obtained (" + marksObtained + ") must be between 0 and " + maxMarks + ".")
                );
            }

            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found: " + studentId));

            double pct = maxMarks > 0 ? (marksObtained * 100.0) / maxMarks : 0.0;
            String grade = pct >= 90 ? "O" : pct >= 80 ? "A+" : pct >= 70 ? "A" : pct >= 60 ? "B+" : pct >= 50 ? "B" : "F";

            Mark mark = markRepository
                    .findByStudentIdAndCourseIdAndExamTypeIgnoreCase(studentId, courseId, examType)
                    .orElseGet(Mark::new);

            mark.setStudent(student);
            mark.setCourse(course);
            mark.setFaculty(faculty);
            mark.setExamType(examType);
            mark.setMarksObtained(marksObtained);
            mark.setMaxMarks(maxMarks);
            mark.setGrade(grade);
            mark.setSemester(semester);
            mark.setPublishedDate(LocalDate.now());

            markRepository.save(mark);
            publishedCount++;
        }

        if (publishedCount == 0) {
            return ResponseEntity.badRequest().body(new MessageResponse("Please enter marks for at least one student before publishing."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            new MessageResponse("Published " + examType + " marks for " + publishedCount + " student(s) in " + course.getCode() + ".")
        );
    }
}
