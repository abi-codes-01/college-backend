package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final FacultyRepository facultyRepository;
    private final CourseRepository courseRepository;
    private final NoticeRepository noticeRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final MarkRepository markRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            DepartmentRepository departmentRepository,
            FacultyRepository facultyRepository,
            CourseRepository courseRepository,
            NoticeRepository noticeRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            MarkRepository markRepository,
            PasswordEncoder passwordEncoder) {
        this.departmentRepository = departmentRepository;
        this.facultyRepository = facultyRepository;
        this.courseRepository = courseRepository;
        this.noticeRepository = noticeRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.markRepository = markRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Seed 4 Distinguished Departments if empty (or top up missing codes)
        List<Department> initialDepartments = List.of(
            new Department(
                "CSE",
                "Computer Science & Engineering",
                "Pioneering research and industry-aligned education in Cloud Computing, Full-Stack Enterprise Systems, Cybersecurity, and Distributed Architectures.",
                "Dr. Alan Turing",
                "Turing Innovation Complex, Block A",
                1998,
                180
            ),
            new Department(
                "AIDS",
                "Artificial Intelligence & Data Science",
                "Advancing frontier machine intelligence, Deep Neural Architectures, Large Language Models, Computer Vision, and Big Data Analytics.",
                "Dr. Geoffrey Hinton",
                "Neural Computing Pavilion, Block B",
                2018,
                150
            ),
            new Department(
                "ECE",
                "Electronics & Communication",
                "Focusing on Sub-Nanometer VLSI Design, Embedded IoT Systems, 6G Wireless Networks, Signal Processing, and Autonomous Robotics.",
                "Dr. Claude Shannon",
                "Maxwell Hall, Block C",
                2001,
                120
            ),
            new Department(
                "MECH",
                "Mechanical Engineering",
                "Excellence in Robotics & Mechatronics, Computational Fluid Dynamics, CAD/CAM Additive Manufacturing, and Sustainable Propulsion.",
                "Dr. Kalpana Chawla",
                "Visvesvaraya Engineering Complex, Block D",
                1992,
                120
            )
        );

        if (departmentRepository.count() == 0) {
            departmentRepository.saveAll(initialDepartments);
        } else if (departmentRepository.count() < 4) {
            for (Department dept : initialDepartments) {
                if (!departmentRepository.existsByCode(dept.getCode())) {
                    departmentRepository.save(dept);
                }
            }
        }

        // 2. Seed 15 Distinguished Faculty Members (Dr. / Prof. across the 4 departments)
        List<Faculty> initialFaculty = List.of(
            // Computer Science & Engineering (4)
            new Faculty(
                "Dr. Alan Turing",
                "turing@apexuniversity.edu",
                "Professor & Head of Department",
                "Computer Science & Engineering",
                "Ph.D. in Computer Science, Princeton",
                "Cloud Computing, Cryptography & Theoretical CS",
                "Block A, Room 301",
                "+1 (555) 234-5601",
                "Leading researcher in distributed cloud architectures, computational complexity, and secure systems."
            ),
            new Faculty(
                "Prof. Grace Hopper",
                "hopper@apexuniversity.edu",
                "Distinguished Professor",
                "Computer Science & Engineering",
                "Ph.D. in Mathematics & Compilers, Yale",
                "Compilers, Full-Stack Enterprise Engineering & Runtime Systems",
                "Block A, Room 305",
                "+1 (555) 234-5602",
                "Architect of modern compiler design and enterprise software engineering methodologies."
            ),
            new Faculty(
                "Prof. Linus Torvalds",
                "torvalds@apexuniversity.edu",
                "Professor of Systems Engineering",
                "Computer Science & Engineering",
                "M.Sc. in Computer Science, University of Helsinki",
                "Operating System Kernels, Containers & Distributed Version Control",
                "Block A, Room 310",
                "+1 (555) 234-5603",
                "Specializes in high-concurrency kernel internals, containerization, and open-source systems."
            ),
            new Faculty(
                "Dr. Whitfield Diffie",
                "diffie@apexuniversity.edu",
                "Associate Professor of Cybersecurity",
                "Computer Science & Engineering",
                "Ph.D. in Cryptographic Systems, Stanford",
                "Public-Key Cryptography, Zero-Trust Security & Network Defense",
                "Block A, Room 314",
                "+1 (555) 234-5604",
                "Pioneering educator in network security protocols and zero-trust enterprise architectures."
            ),

            // Artificial Intelligence & Data Science (4)
            new Faculty(
                "Dr. Geoffrey Hinton",
                "hinton@apexuniversity.edu",
                "Professor & Head of Department",
                "Artificial Intelligence & Data Science",
                "Ph.D. in Artificial Intelligence, Edinburgh",
                "Deep Learning, Backpropagation & Neural Representations",
                "Block B, Room 201",
                "+1 (555) 234-5605",
                "Directs the university's neural computation and foundational AI research initiatives."
            ),
            new Faculty(
                "Prof. Yann LeCun",
                "lecun@apexuniversity.edu",
                "Distinguished Professor of AI",
                "Artificial Intelligence & Data Science",
                "Ph.D. in Computer Science, Sorbonne University",
                "Convolutional Networks, Self-Supervised Learning & Autonomous Vision",
                "Block B, Room 204",
                "+1 (555) 234-5606",
                "Leads breakthrough research in visual perception and energy-based machine learning models."
            ),
            new Faculty(
                "Dr. Fei-Fei Li",
                "feifei@apexuniversity.edu",
                "Professor of Cognitive AI",
                "Artificial Intelligence & Data Science",
                "Ph.D. in Electrical Engineering, Caltech",
                "Human-Centered AI, Visual Intelligence & Healthcare Robotics",
                "Block B, Room 208",
                "+1 (555) 234-5607",
                "Championing ethical, human-centered artificial intelligence and large-scale multimodal datasets."
            ),
            new Faculty(
                "Prof. Andrew Ng",
                "andrew.ng@apexuniversity.edu",
                "Professor of Applied Data Science",
                "Artificial Intelligence & Data Science",
                "Ph.D. in Computer Science, UC Berkeley",
                "Data-Centric AI, MLOps & Scalable Machine Learning",
                "Block B, Room 212",
                "+1 (555) 234-5608",
                "Focuses on production-grade machine learning pipelines and industry-aligned AI education."
            ),

            // Electronics & Communication (4)
            new Faculty(
                "Dr. Claude Shannon",
                "shannon@apexuniversity.edu",
                "Professor & Head of Department",
                "Electronics & Communication",
                "Ph.D. in Electrical Engineering, MIT",
                "VLSI Design, Information Theory & Digital Switching Circuits",
                "Block C, Room 101",
                "+1 (555) 234-5609",
                "Founder of digital circuit design theory and high-density semiconductor architecture."
            ),
            new Faculty(
                "Prof. Sophie Wilson",
                "wilson@apexuniversity.edu",
                "Professor of Microprocessor Architecture",
                "Electronics & Communication",
                "Ph.D. in Computer & Microprocessor Engineering, Cambridge",
                "RISC Instruction Sets, Low-Power SoC Design & Embedded Systems",
                "Block C, Room 105",
                "+1 (555) 234-5610",
                "Co-architect of energy-efficient RISC processors powering billions of embedded devices."
            ),
            new Faculty(
                "Dr. Andrea Goldsmith",
                "goldsmith@apexuniversity.edu",
                "Professor of Wireless Communications",
                "Electronics & Communication",
                "Ph.D. in Electrical Engineering, UC Berkeley",
                "6G Wireless Networks, MIMO Signal Processing & Smart Spectrum",
                "Block C, Room 109",
                "+1 (555) 234-5611",
                "Spearheading next-generation ultra-low-latency wireless communication standards."
            ),
            new Faculty(
                "Prof. Rodney Brooks",
                "brooks@apexuniversity.edu",
                "Associate Professor of Autonomous Systems",
                "Electronics & Communication",
                "Ph.D. in Computer Science, Stanford",
                "Embedded Robotics, Sensor Fusion & Real-Time Control",
                "Block C, Room 114",
                "+1 (555) 234-5612",
                "Expert in autonomous mobile robotics and real-time hardware-software integration."
            ),

            // Mechanical Engineering (3)
            new Faculty(
                "Dr. Kalpana Chawla",
                "chawla@apexuniversity.edu",
                "Professor & Head of Department",
                "Mechanical Engineering",
                "Ph.D. in Aerospace & Mechanical Engineering, Colorado",
                "Computational Fluid Dynamics, Aerodynamics & Mechatronics",
                "Block D, Room 401",
                "+1 (555) 234-5613",
                "Dedicated to experimental aerodynamics, propulsion systems, and autonomous flight vehicles."
            ),
            new Faculty(
                "Prof. Fazle Hussain",
                "hussain@apexuniversity.edu",
                "Professor of Thermal & Fluid Sciences",
                "Mechanical Engineering",
                "Ph.D. in Mechanical Engineering, Stanford",
                "Turbulence Modeling, Vortex Dynamics & Clean Energy Systems",
                "Block D, Room 405",
                "+1 (555) 234-5614",
                "Renowned researcher in fluid mechanics, propulsion efficiency, and sustainable thermal systems."
            ),
            new Faculty(
                "Dr. Vikram Sarabhai",
                "sarabhai@apexuniversity.edu",
                "Distinguished Professor of Robotics & Manufacturing",
                "Mechanical Engineering",
                "Ph.D. in Physics & Systems Engineering, Cambridge",
                "CAD/CAM Additive Manufacturing, Space Systems & Industrial Robotics",
                "Block D, Room 410",
                "+1 (555) 234-5615",
                "Guides interdisciplinary research in precision manufacturing, satellite payloads, and robotics."
            )
        );

        if (facultyRepository.count() == 0) {
            facultyRepository.saveAll(initialFaculty);
        } else if (facultyRepository.count() < 15) {
            for (Faculty member : initialFaculty) {
                if (!facultyRepository.existsByEmail(member.getEmail())) {
                    facultyRepository.save(member);
                }
            }
        }

        // 3. Seed 15 Specialized Courses (3 to 4 per department)
        List<Course> initialCourses = List.of(
            // Computer Science & Engineering (4 courses)
            new Course(
                "CS101",
                "B.Tech in Cloud Computing",
                4,
                "Computer Science & Engineering",
                "Dr. Alan Turing",
                "Semester 5",
                "Distributed cloud-native architectures, Kubernetes orchestration, serverless microservices, and multi-cloud resilience."
            ),
            new Course(
                "CS204",
                "Full-Stack Enterprise Architecture",
                4,
                "Computer Science & Engineering",
                "Prof. Grace Hopper",
                "Semester 5",
                "Production-grade web engineering with React, Spring Boot, PostgreSQL optimization, and RESTful security."
            ),
            new Course(
                "CS302",
                "Operating Systems & Distributed Kernels",
                4,
                "Computer Science & Engineering",
                "Prof. Linus Torvalds",
                "Semester 6",
                "Process scheduling, virtual memory management, concurrency primitives, and distributed consensus protocols."
            ),
            new Course(
                "CS405",
                "Cybersecurity & Zero-Trust Networks",
                3,
                "Computer Science & Engineering",
                "Dr. Whitfield Diffie",
                "Semester 7",
                "Applied cryptography, identity federation, intrusion detection, and enterprise zero-trust security architecture."
            ),

            // Artificial Intelligence & Data Science (4 courses)
            new Course(
                "AI101",
                "B.Tech in Artificial Intelligence & Machine Learning",
                4,
                "Artificial Intelligence & Data Science",
                "Dr. Geoffrey Hinton",
                "Semester 3",
                "Foundations of statistical machine learning, supervised/unsupervised optimization, and neural computation."
            ),
            new Course(
                "AI202",
                "Deep Learning & Neural Architectures",
                4,
                "Artificial Intelligence & Data Science",
                "Prof. Yann LeCun",
                "Semester 5",
                "Convolutional networks, vision transformers, generative diffusion models, and GPU-accelerated training."
            ),
            new Course(
                "AI305",
                "Natural Language Processing & Large Language Models",
                4,
                "Artificial Intelligence & Data Science",
                "Dr. Fei-Fei Li",
                "Semester 6",
                "Transformer attention mechanisms, retrieval-augmented generation (RAG), alignment, and multimodal AI agents."
            ),
            new Course(
                "AI401",
                "Big Data Analytics & MLOps Engineering",
                3,
                "Artificial Intelligence & Data Science",
                "Prof. Andrew Ng",
                "Semester 7",
                "Distributed stream processing, feature stores, automated model deployment, and continuous monitoring."
            ),

            // Electronics & Communication (4 courses)
            new Course(
                "EC101",
                "M.Tech in VLSI Design",
                4,
                "Electronics & Communication",
                "Dr. Claude Shannon",
                "Semester 3",
                "CMOS sub-micron fabrication, RTL synthesis, Verilog/SystemVerilog verification, and physical chip layout."
            ),
            new Course(
                "EC204",
                "Embedded Systems & SoC Architecture",
                4,
                "Electronics & Communication",
                "Prof. Sophie Wilson",
                "Semester 5",
                "ARM/RISC-V instruction pipelines, real-time operating systems (RTOS), and hardware-software co-design."
            ),
            new Course(
                "EC308",
                "6G Wireless Networks & Signal Processing",
                4,
                "Electronics & Communication",
                "Dr. Andrea Goldsmith",
                "Semester 6",
                "Massive MIMO beamforming, millimeter-wave propagation, adaptive filtering, and software-defined radio."
            ),
            new Course(
                "EC402",
                "Autonomous Robotics & Sensor Fusion",
                3,
                "Electronics & Communication",
                "Prof. Rodney Brooks",
                "Semester 7",
                "LiDAR/IMU Kalman filtering, SLAM navigation, embedded actuators, and autonomous control loops."
            ),

            // Mechanical Engineering (3 courses)
            new Course(
                "ME101",
                "B.Tech in Robotics & Mechatronics",
                4,
                "Mechanical Engineering",
                "Dr. Kalpana Chawla",
                "Semester 3",
                "Kinematic modeling of robotic manipulators, electro-mechanical actuation, and intelligent automation."
            ),
            new Course(
                "ME205",
                "Computational Fluid Dynamics & Propulsion",
                4,
                "Mechanical Engineering",
                "Prof. Fazle Hussain",
                "Semester 5",
                "Navier-Stokes numerical solvers, finite volume methods, turbomachinery, and high-efficiency propulsion."
            ),
            new Course(
                "ME309",
                "Advanced CAD/CAM & Additive Manufacturing",
                3,
                "Mechanical Engineering",
                "Dr. Vikram Sarabhai",
                "Semester 6",
                "Generative structural topology optimization, multi-axis CNC machining, and metal 3D printing."
            )
        );

        if (courseRepository.count() == 0) {
            courseRepository.saveAll(initialCourses);
        } else if (courseRepository.count() < 15) {
            for (Course course : initialCourses) {
                if (!courseRepository.existsByCode(course.getCode())) {
                    courseRepository.save(course);
                }
            }
        }

        // 4. Seed Professional Campus Notices (idempotent by title)
        List<Notice> defaultNotices = List.of(
            new Notice(
                "Winter Hackathon Registration Open",
                "Innovation & Events",
                "Innovation Incubation Center",
                LocalDate.now().minusDays(1),
                "Form teams of up to 4 students for the 48-hour Winter Hackathon focused on AI, Cloud-Native Microservices, and FinTech solutions. Register via the Student ERP Portal.",
                true
            ),
            new Notice(
                "End-Semester Examination Schedule Published",
                "Examination Cell",
                "Controller of Examinations",
                LocalDate.now().minusDays(3),
                "The comprehensive End-Semester Examination timetable and hall allocations for all undergraduate and postgraduate programs have been published. Please verify course codes on your dashboard.",
                true
            ),
            new Notice(
                "Guest Lecture: Modern Full-Stack Architecture",
                "Academic Seminar",
                "Department of Computer Science & Engineering",
                LocalDate.now().minusDays(5),
                "Join Principal Cloud Architects for an interactive keynote on reactive frontend patterns, Spring Boot microservices, and zero-trust security in modern enterprise systems.",
                true
            )
        );

        for (Notice notice : defaultNotices) {
            if (!noticeRepository.existsByTitle(notice.getTitle())) {
                noticeRepository.save(notice);
            }
        }

        // 5. Seed Default Users (admin / admin123, turing / faculty123, student / student123)
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User(
                "admin",
                "admin@apexuniversity.edu",
                passwordEncoder.encode("admin123"),
                Role.ROLE_ADMIN,
                AccountStatus.APPROVED,
                "System Administrator"
            ));
        }

        if (!userRepository.existsByUsername("turing")) {
            userRepository.save(new User(
                "turing",
                "turing@apexuniversity.edu",
                passwordEncoder.encode("faculty123"),
                Role.ROLE_FACULTY,
                AccountStatus.APPROVED,
                "Dr. Alan Turing"
            ));
        }

        User demoStudentUser = userRepository.findByUsername("student").orElseGet(() ->
            userRepository.save(new User(
                "student",
                "aarav.sharma@apexuniversity.edu",
                passwordEncoder.encode("student123"),
                Role.ROLE_STUDENT,
                AccountStatus.APPROVED,
                "Aarav Sharma"
            ))
        );

        // 6. Seed at least 120 Enrolled Scholars with APPROVED account statuses
        Student aarav = studentRepository.findByEmail("aarav.sharma@apexuniversity.edu").orElseGet(() -> {
            Student s = new Student(
                "Aarav Sharma",
                "aarav.sharma@apexuniversity.edu",
                "B.Tech in Cloud Computing",
                "Semester 5",
                "Computer Science & Engineering",
                "+1 (555) 890-1001",
                AccountStatus.APPROVED
            );
            s.setEnrollmentNumber("APX-2026-0001");
            s.setUser(demoStudentUser);
            return studentRepository.save(s);
        });

        if (aarav.getUser() == null || aarav.getStatus() != AccountStatus.APPROVED) {
            aarav.setUser(demoStudentUser);
            aarav.setStatus(AccountStatus.APPROVED);
            if (aarav.getEnrollmentNumber() == null) {
                aarav.setEnrollmentNumber("APX-2026-0001");
            }
            aarav = studentRepository.save(aarav);
        }

        long currentApprovedCount = studentRepository.countByStatus(AccountStatus.APPROVED);
        if (currentApprovedCount < 120) {
            String[] firstNames = {
                "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan", "Krishna",
                "Ishaan", "Shaurya", "Atharv", "Advik", "Pranav", "Advaith", "Aaryan", "Dhruv",
                "Kabir", "Ritvik", "Aarush", "Kian", "Darsh", "Veer", "Ananya", "Diya",
                "Priya", "Kavya", "Aadhya", "Saanvi", "Meera", "Riya", "Neha", "isha",
                "Tanvi", "Pooja", "Shreya", "Nisha", "Rohan", "Vikram", "Karan", "Nikhil"
            };
            String[] lastNames = {
                "Patel", "Nair", "Verma", "Reddy", "Iyer", "Kulkarni", "Mehta", "Joshi",
                "Chatterjee", "bansal", "Malhotra", "Kapoor", "Deshmukh", "Rao", "Menon",
                "pillai", "Agarwal", "saxena", "bhatia", "Chopra", "Sen", "Das", "Gupta", "Bose"
            };
            String[] deptNames = {
                "Computer Science & Engineering",
                "Artificial Intelligence & Data Science",
                "Electronics & Communication",
                "Mechanical Engineering"
            };
            String[] programNames = {
                "B.Tech in Cloud Computing",
                "B.Tech in Artificial Intelligence & Machine Learning",
                "M.Tech in VLSI Design",
                "B.Tech in Robotics & Mechatronics"
            };
            String[] semesters = {
                "Semester 1", "Semester 3", "Semester 5", "Semester 7"
            };

            List<Student> batchToInsert = new ArrayList<>();
            int needed = (int) (120 - currentApprovedCount);
            int index = 2;

            while (batchToInsert.size() < needed && index <= 250) {
                String email = String.format("scholar.%03d@apexuniversity.edu", index);
                if (studentRepository.findByEmail(email).isEmpty()) {
                    String firstName = firstNames[(index - 2) % firstNames.length];
                    String rawLast = lastNames[((index - 2) / firstNames.length + (index % 7)) % lastNames.length];
                    String lastName = Character.toUpperCase(rawLast.charAt(0)) + rawLast.substring(1);
                    String fullName = Character.toUpperCase(firstName.charAt(0)) + firstName.substring(1) + " " + lastName;

                    int deptIdx = (index - 2) % deptNames.length;
                    String department = deptNames[deptIdx];
                    String course = programNames[deptIdx];
                    String semester = semesters[(index - 2) % semesters.length];
                    String phone = String.format("+1 (555) 890-%04d", 1000 + index);

                    Student scholar = new Student(
                        fullName,
                        email,
                        course,
                        semester,
                        department,
                        phone,
                        AccountStatus.APPROVED
                    );
                    scholar.setEnrollmentNumber(String.format("APX-2026-%04d", index));
                    batchToInsert.add(scholar);
                }
                index++;
            }

            if (!batchToInsert.isEmpty()) {
                studentRepository.saveAll(batchToInsert);
            }
        }

        // 7. Seed Attendance & Marks for Demo Student (Aarav Sharma) if empty
        Student finalAarav = aarav;
        List<Course> allCourses = courseRepository.findAll();
        List<Faculty> allFaculty = facultyRepository.findAll();

        if (!allCourses.isEmpty() && attendanceRepository.count() == 0) {
            Course c1 = allCourses.get(0);
            Course c2 = allCourses.size() > 1 ? allCourses.get(1) : c1;
            Course c3 = allCourses.size() > 2 ? allCourses.get(2) : c1;
            Course c4 = allCourses.size() > 4 ? allCourses.get(4) : c1;

            Faculty f1 = allFaculty.size() > 0 ? allFaculty.get(0) : null;
            Faculty f2 = allFaculty.size() > 1 ? allFaculty.get(1) : null;
            Faculty f3 = allFaculty.size() > 2 ? allFaculty.get(2) : null;
            Faculty f4 = allFaculty.size() > 4 ? allFaculty.get(4) : null;

            attendanceRepository.saveAll(List.of(
                new Attendance(finalAarav, c1, f1, LocalDate.now().minusDays(1), true, 38, 42, "Regular attendance"),
                new Attendance(finalAarav, c2, f2, LocalDate.now().minusDays(1), true, 35, 40, "Active lab participation"),
                new Attendance(finalAarav, c4, f4, LocalDate.now().minusDays(2), true, 36, 38, "Excellent consistency"),
                new Attendance(finalAarav, c3, f3, LocalDate.now().minusDays(3), false, 29, 40, "Borderline attendance")
            ));

            if (markRepository.count() == 0) {
                markRepository.saveAll(List.of(
                    new Mark(finalAarav, c1, f1, "End-Semester", 92.0, 100.0, "A+", "Semester 5", LocalDate.now().minusDays(10)),
                    new Mark(finalAarav, c2, f2, "End-Semester", 86.0, 100.0, "A", "Semester 5", LocalDate.now().minusDays(9)),
                    new Mark(finalAarav, c4, f4, "End-Semester", 95.0, 100.0, "O", "Semester 5", LocalDate.now().minusDays(8)),
                    new Mark(finalAarav, c3, f3, "End-Semester", 78.0, 100.0, "B+", "Semester 5", LocalDate.now().minusDays(7))
                ));
            }
        }
    }
}
