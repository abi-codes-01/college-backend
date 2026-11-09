package com.example.demo.service;

import com.example.demo.dto.JwtResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.model.AccountStatus;
import com.example.demo.model.Role;
import com.example.demo.model.Student;
import com.example.demo.model.User;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Year;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (user.getStatus() == AccountStatus.PENDING) {
            throw new IllegalStateException(
                "Your student account is PENDING administrator approval. Please wait for an Admin to approve your registration."
            );
        }

        if (user.getStatus() == AccountStatus.REJECTED) {
            throw new IllegalStateException(
                "Your registration request was declined by the administrator. Please contact the Registrar's office."
            );
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        return new JwtResponse(
                jwt,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
    }

    @Transactional
    public void registerStudent(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword()) || !StringUtils.hasText(request.getEmail())) {
            throw new IllegalArgumentException("Username, email, and password are required.");
        }

        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email address is already registered.");
        }

        String displayName = StringUtils.hasText(request.getName())
                ? request.getName().trim()
                : request.getUsername().trim();

        // Section 9 Security Rule: Public self-registration is strictly locked to ROLE_STUDENT and PENDING status
        User newUser = new User(
                request.getUsername().trim(),
                request.getEmail().trim(),
                passwordEncoder.encode(request.getPassword()),
                Role.ROLE_STUDENT,
                AccountStatus.PENDING,
                displayName
        );

        User savedUser = userRepository.save(newUser);

        Student student = new Student();
        student.setName(displayName);
        student.setEmail(savedUser.getEmail());
        student.setCourse(StringUtils.hasText(request.getCourse()) ? request.getCourse().trim() : "B.Tech");
        student.setDepartment(StringUtils.hasText(request.getDepartment()) ? request.getDepartment().trim() : "Computer Science & Engineering");
        student.setSemester(StringUtils.hasText(request.getSemester()) ? request.getSemester().trim() : "Semester 1");
        student.setPhone(request.getPhone());
        student.setEnrollmentNumber("APX-" + Year.now().getValue() + "-" + String.format("%04d", savedUser.getId()));
        student.setStatus(AccountStatus.PENDING);
        student.setUser(savedUser);

        studentRepository.save(student);
    }
}
