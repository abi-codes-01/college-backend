package com.example.demo;

import com.example.demo.model.AccountStatus;
import com.example.demo.model.Role;
import com.example.demo.model.Student;
import com.example.demo.model.User;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class WorkflowIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private String generateBearerTokenForUser(String username, Role role) {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                username,
                "password",
                Collections.singletonList(new SimpleGrantedAuthority(role.name()))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return "Bearer " + jwtUtils.generateJwtToken(auth);
    }

    @Test
    @DisplayName("Workflow 9 (Data Isolation): Authenticated STUDENT receives 200 OK on /api/student/my-marks and 403 Forbidden on /api/admin/stats")
    void verifyStudentDataIsolationAndRoleProtection() throws Exception {
        String studentBearerToken = generateBearerTokenForUser("student", Role.ROLE_STUDENT);

        // 1. Authenticated student fetching their own isolated marks -> 200 OK
        mockMvc.perform(get("/api/student/my-marks")
                        .header("Authorization", studentBearerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // 2. Authenticated student attempting to access Admin statistics -> 403 Forbidden
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", studentBearerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Workflows 2 & 3 (Registration & Admin Approval): Self-registration creates PENDING student and Admin approval transitions to APPROVED")
    void verifyStudentSelfRegistrationAndAdminApprovalWorkflow() throws Exception {
        String uniqueUsername = "test_scholar_" + System.currentTimeMillis();
        String uniqueEmail = uniqueUsername + "@apexuniversity.edu";

        String signupJson = String.format("""
                {
                  "name": "Test Scholar",
                  "username": "%s",
                  "email": "%s",
                  "password": "Password123!",
                  "department": "Computer Science & Engineering",
                  "course": "B.Tech CSE",
                  "semester": "Semester 1",
                  "phone": "+1 555-0100",
                  "role": "ROLE_ADMIN"
                }
                """, uniqueUsername, uniqueEmail);

        // Step 1: Submit student self-registration payload
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        // Step 2: Verify User and Student are created with PENDING status & locked to ROLE_STUDENT
        User createdUser = userRepository.findByUsername(uniqueUsername).orElseThrow();
        assertThat(createdUser.getStatus()).isEqualTo(AccountStatus.PENDING);
        assertThat(createdUser.getRole()).isEqualTo(Role.ROLE_STUDENT);

        Student createdStudent = studentRepository.findByEmail(uniqueEmail).orElseThrow();
        assertThat(createdStudent.getStatus()).isEqualTo(AccountStatus.PENDING);

        // Step 3: Verify login is blocked (403 Forbidden) while status is PENDING
        String loginJson = String.format("""
                {
                  "username": "%s",
                  "password": "Password123!"
                }
                """, uniqueUsername);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isForbidden());

        // Step 4: Admin approves the student registration via PUT /api/admin/students/{id}/approve
        String adminBearerToken = generateBearerTokenForUser("admin", Role.ROLE_ADMIN);

        mockMvc.perform(put("/api/admin/students/{id}/approve", createdStudent.getId())
                        .header("Authorization", adminBearerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Step 5: Assert both Student and User accounts have transitioned to APPROVED
        User approvedUser = userRepository.findByUsername(uniqueUsername).orElseThrow();
        Student approvedStudent = studentRepository.findById(createdStudent.getId()).orElseThrow();
        assertThat(approvedUser.getStatus()).isEqualTo(AccountStatus.APPROVED);
        assertThat(approvedStudent.getStatus()).isEqualTo(AccountStatus.APPROVED);

        // Step 6: Verify approved student can now sign in and receive a JWT token (200 OK)
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
