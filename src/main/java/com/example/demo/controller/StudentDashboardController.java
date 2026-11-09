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

import java.util.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentDashboardController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;

    public StudentDashboardController(
            UserRepository userRepository,
            StudentRepository studentRepository,
            AttendanceRepository attendanceRepository,
            MarkRepository markRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
    }

    /**
     * Resolves the currently authenticated Student strictly from SecurityContextHolder.
     * Never accepts a student ID from request parameters or URL paths.
     */
    private Student getAuthenticatedStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated.");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated user not found."));

        return studentRepository.findByUserId(user.getId())
                .or(() -> studentRepository.findByEmail(user.getEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found for authenticated user."));
    }

    @GetMapping("/my-profile")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        Student student = getAuthenticatedStudent();
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", student.getId());
        profile.put("name", student.getName());
        profile.put("email", student.getEmail());
        profile.put("enrollmentNumber", student.getEnrollmentNumber() != null ? student.getEnrollmentNumber() : "APX-2026-" + String.format("%04d", student.getId()));
        profile.put("department", student.getDepartment());
        profile.put("course", student.getCourse());
        profile.put("semester", student.getSemester());
        profile.put("phone", student.getPhone());
        profile.put("status", student.getStatus() != null ? student.getStatus().name() : "APPROVED");
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/my-attendance")
    public ResponseEntity<Map<String, Object>> getMyAttendance() {
        Student student = getAuthenticatedStudent();
        List<Attendance> rawAttendance = attendanceRepository.findByStudentId(student.getId());

        Map<Long, Map<String, Object>> byCourse = new LinkedHashMap<>();
        int grandTotalAttended = 0;
        int grandTotalClasses = 0;

        for (Attendance record : rawAttendance) {
            Course course = record.getCourse();
            if (course == null) continue;

            int attended = record.getAttendedClasses() != null
                    ? record.getAttendedClasses()
                    : (Boolean.TRUE.equals(record.getPresent()) ? 1 : 0);
            int total = record.getTotalClasses() != null && record.getTotalClasses() > 0
                    ? record.getTotalClasses()
                    : 1;

            grandTotalAttended += attended;
            grandTotalClasses += total;

            Map<String, Object> entry = byCourse.computeIfAbsent(course.getId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courseId", course.getId());
                m.put("courseCode", course.getCode());
                m.put("courseName", course.getName());
                m.put("credits", course.getCredits());
                m.put("facultyName", record.getFaculty() != null ? record.getFaculty().getName() : course.getInstructor());
                m.put("attendedClasses", 0);
                m.put("totalClasses", 0);
                return m;
            });

            entry.put("attendedClasses", ((Integer) entry.get("attendedClasses")) + attended);
            entry.put("totalClasses", ((Integer) entry.get("totalClasses")) + total);
        }

        List<Map<String, Object>> subjectAttendance = new ArrayList<>();
        for (Map<String, Object> item : byCourse.values()) {
            int attended = (Integer) item.get("attendedClasses");
            int total = (Integer) item.get("totalClasses");
            double pct = total > 0 ? Math.round((attended * 1000.0) / total) / 10.0 : 0.0;
            item.put("percentage", pct);
            item.put("shortage", pct < 75.0);
            subjectAttendance.add(item);
        }

        double overallPercentage = grandTotalClasses > 0
                ? Math.round((grandTotalAttended * 1000.0) / grandTotalClasses) / 10.0
                : 0.0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("overallPercentage", overallPercentage);
        response.put("totalAttended", grandTotalAttended);
        response.put("totalClasses", grandTotalClasses);
        response.put("subjects", subjectAttendance);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-marks")
    public ResponseEntity<List<Map<String, Object>>> getMyMarks() {
        Student student = getAuthenticatedStudent();
        List<Mark> marks = markRepository.findByStudentId(student.getId());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Mark mark : marks) {
            Course course = mark.getCourse();
            double obtained = mark.getMarksObtained() != null ? mark.getMarksObtained() : 0.0;
            double max = mark.getMaxMarks() != null && mark.getMaxMarks() > 0 ? mark.getMaxMarks() : 100.0;
            double percentage = Math.round((obtained * 1000.0) / max) / 10.0;

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", mark.getId());
            dto.put("courseCode", course != null ? course.getCode() : "N/A");
            dto.put("courseName", course != null ? course.getName() : "Unknown Course");
            dto.put("credits", course != null ? course.getCredits() : 0);
            dto.put("facultyName", mark.getFaculty() != null ? mark.getFaculty().getName() : (course != null ? course.getInstructor() : "N/A"));
            dto.put("examType", mark.getExamType());
            dto.put("marksObtained", obtained);
            dto.put("maxMarks", max);
            dto.put("percentage", percentage);
            dto.put("grade", mark.getGrade());
            dto.put("semester", mark.getSemester());
            dto.put("publishedDate", mark.getPublishedDate());
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }
}
