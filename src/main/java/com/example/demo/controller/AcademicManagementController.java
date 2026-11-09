package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/academic")
@PreAuthorize("hasRole('FACULTY') or hasRole('ADMIN')")
public class AcademicManagementController {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

    public AcademicManagementController(
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            FacultyRepository facultyRepository,
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            MarkRepository markRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
    }

    private Faculty resolveCurrentFaculty() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName())
                .flatMap(u -> facultyRepository.findByEmail(u.getEmail()))
                .orElse(null);
    }

    @PostMapping("/attendance")
    public ResponseEntity<Attendance> recordAttendance(@RequestBody Map<String, Object> payload) {
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        Integer attendedClasses = Integer.valueOf(payload.getOrDefault("attendedClasses", 1).toString());
        Integer totalClasses = Integer.valueOf(payload.getOrDefault("totalClasses", 1).toString());
        String remarks = payload.getOrDefault("remarks", "Updated by faculty").toString();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Faculty faculty = resolveCurrentFaculty();

        // Update existing attendance record for this student + course if present, otherwise create
        List<Attendance> existing = attendanceRepository.findByStudentId(studentId);
        Attendance attendance = existing.stream()
                .filter(a -> a.getCourse() != null && a.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseGet(Attendance::new);

        attendance.setStudent(student);
        attendance.setCourse(course);
        if (faculty != null) {
            attendance.setFaculty(faculty);
        }
        attendance.setDate(LocalDate.now());
        attendance.setAttendedClasses(attendedClasses);
        attendance.setTotalClasses(totalClasses);
        attendance.setPresent(attendedClasses > 0);
        attendance.setRemarks(remarks);

        return ResponseEntity.ok(attendanceRepository.save(attendance));
    }

    @PostMapping("/marks")
    public ResponseEntity<Mark> publishMark(@RequestBody Map<String, Object> payload) {
        Long studentId = Long.valueOf(payload.get("studentId").toString());
        Long courseId = Long.valueOf(payload.get("courseId").toString());
        String examType = payload.getOrDefault("examType", "End-Semester").toString();
        Double marksObtained = Double.valueOf(payload.get("marksObtained").toString());
        Double maxMarks = Double.valueOf(payload.getOrDefault("maxMarks", 100.0).toString());
        String semester = payload.getOrDefault("semester", "Semester 5").toString();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        double pct = maxMarks > 0 ? (marksObtained * 100.0) / maxMarks : 0.0;
        String computedGrade = payload.get("grade") != null && !payload.get("grade").toString().isBlank()
                ? payload.get("grade").toString()
                : (pct >= 90 ? "O" : pct >= 80 ? "A+" : pct >= 70 ? "A" : pct >= 60 ? "B+" : pct >= 50 ? "B" : "F");

        Faculty faculty = resolveCurrentFaculty();

        Mark mark = new Mark(
                student,
                course,
                faculty,
                examType,
                marksObtained,
                maxMarks,
                computedGrade,
                semester,
                LocalDate.now()
        );

        return ResponseEntity.ok(markRepository.save(mark));
    }

    @GetMapping("/course/{courseId}/records")
    public ResponseEntity<Map<String, Object>> getCourseRecords(@PathVariable Long courseId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attendance", attendanceRepository.findByCourseId(courseId));
        result.put("marks", markRepository.findByCourseId(courseId));
        return ResponseEntity.ok(result);
    }
}
