package group7.enrollmentSystem.config;

import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.StudentProgrammeService;
import group7.enrollmentSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourseRepo courseRepo;
    private final ProgrammeRepo programmeRepo;
    private final UserService userService;
    private final StudentProgrammeService studentProgrammeService;
    private final StudentRepo studentRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final UserRepo userRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentStateRepo enrollmentStateRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final StudentHoldHistoryRepo studentHoldHistoryRepo;

    @Override
    public void run(String... args) {
        initializeCourseEnrollmentStatus();
        initializeAdminUser();
        initializeStudents();
        initializeStudentHolds();
        initializeCourses();
        initializeProgrammes();         // Now BSE & BNS
        initializeCourseProgrammes();   // Link BSE & BNS
        linkStudentsToProgrammes();     // Assign students to BSE or BNS
        initializeCourseEnrollments();
        restoreCoursePrerequisiteFromBackup();
    }
    private void restoreCoursePrerequisiteFromBackup() {
        if (coursePrerequisiteRepo.count() > 0) {
            System.out.println("course_prerequisite already exists. Skipping restore.");
            return;
        }

        String dbUser = "root";
        String dbPassword = "12345";
        String dbName = "enrollment_database";
        String mysqlBinary = "mysql";

        try {
            // Step 1: Load SQL from resources
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("backup/course_prerequisite_backup1.sql");
            if (inputStream == null) {
                throw new FileNotFoundException("course_prerequisite_backup.sql not found in resources!");
            }

            // Step 2: Save it temporarily
            Path tempFile = Files.createTempFile("course_prerequisite_backup", ".sql");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();

            // Step 3: Build MySQL command
            List<String> command = Arrays.asList(
                    mysqlBinary,
                    "-u" + dbUser,
                    "-p" + dbPassword,
                    "-D", dbName,
                    "--execute", "source " + tempFile.toAbsolutePath()
            );

            // Step 4: Run it
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("course_prerequisite restored successfully from backup!");
            } else {
                System.err.println("Error restoring course_prerequisite. Exit code: " + exitCode);
            }

            Files.deleteIfExists(tempFile); // Cleanup

        } catch (Exception e) {
            System.err.println("Exception during course_prerequisite restore: " + e.getMessage());
            e.printStackTrace();
        }
    }



    // --------------------------------------------------------------
    //  1) Enrollment Status
    // --------------------------------------------------------------
    private void initializeCourseEnrollmentStatus() {
        if(enrollmentStateRepo.count() > 0){
            System.out.println("Enrollment status already initialized. Skipping.");
            return;
        }
        EnrollmentState enrollmentState = new EnrollmentState();
        enrollmentState.setId(1L);
        enrollmentState.setOpen(true);
        enrollmentState.setSemesterOne(true);
        enrollmentStateRepo.save(enrollmentState);
    }

    // --------------------------------------------------------------
    //  2) Admin User
    // --------------------------------------------------------------
    private void initializeAdminUser() {
        String adminEmail = "adriandougjonajitino@gmail.com";
        String adminFirstName = "Admin";
        String adminLastName = "Boss";
        String password = "12345";

        if (userRepo.findByEmail(adminEmail).isPresent()) {
            System.out.println("Admin user (" + adminEmail + ") is already registered. Skipping admin initialization.");
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRoles(Set.of("ROLE_ADMIN"));
        admin.setFirstName(adminFirstName);
        admin.setLastName(adminLastName);

        userRepo.save(admin);
        System.out.println("Admin user initialized successfully.");
    }

    // --------------------------------------------------------------
    //  3) Students
    // --------------------------------------------------------------
    private void initializeStudents() {
        if (studentRepo.count() > 0) {
            System.out.println("Students already initialized. Skipping initialization.");
            return;
        }

        List<Student> students = List.of(
                new Student("s11212749", "Tino", "Potoi", "12 Bakshi Street, Suva", "1234567"),
                new Student("s11209521", "Adrian","Alamu", "12 Bakshi Street, Suva", "9429576"),
                new Student("s11212750", "Jane", "Qio", "57 Ratu Mara Rd, Nabua", "2345678"),
                new Student("s11212751", "Pita", "Kumar", "Lot 5 Brown Street, Lautoka", "3456789"),
                new Student("s11212752", "Mary", "Singh", "Lot 3 Princes Rd, Tamavua", "4567890"),
                new Student("s11212753", "Tomasi", "Prasad", "10 Victoria Parade, Suva", "5678901")
        );

        students.forEach(student -> {
            String email = student.getStudentId() + "@student.usp.ac.fj";
            student.setEmail(email);
            student.setPassword(passwordEncoder.encode("12345")); // Encrypt password
            student.setRoles(Set.of("ROLE_STUDENT"));

            studentRepo.save(student);
            System.out.println("Registered student: " + email);
        });
    }

    // --------------------------------------------------------------
    //  4) Student Holds
    // --------------------------------------------------------------
    private void initializeStudentHolds() {
        if (studentHoldHistoryRepo.count() > 0) {
            System.out.println("Student holds already initialized. Skipping.");
            return;
        }

        String adminEmail = "admin@gmail.com";

        // Initialize hold for s11212749 (UNPAID_FEES)
        studentRepo.findByStudentId("s11212749").ifPresent(student -> {
            OnHoldStatus unpaidFeeStatus = new OnHoldStatus();
            unpaidFeeStatus.setOnHoldType(OnHoldTypes.UNPAID_FEES);
            unpaidFeeStatus.setOnHold(true);
            student.getOnHoldStatusList().add(unpaidFeeStatus);
            studentRepo.save(student);

            // Record in history
            StudentHoldHistory history = StudentHoldHistory.create(
                    student.getId(),
                    OnHoldTypes.UNPAID_FEES,
                    true,
                    adminEmail
            );
            studentHoldHistoryRepo.save(history);
            System.out.println(student.getStudentId() + " Hold initialized");
        });

        // Initialize hold for s11212750 (DISCIPLINARY_ISSUES)
        studentRepo.findByStudentId("s11212750").ifPresent(student -> {
            OnHoldStatus disciplinaryStatus = new OnHoldStatus();
            disciplinaryStatus.setOnHoldType(OnHoldTypes.DISCIPLINARY_ISSUES);
            disciplinaryStatus.setOnHold(true);
            student.getOnHoldStatusList().add(disciplinaryStatus);
            studentRepo.save(student);

            // Record in history
            StudentHoldHistory history = StudentHoldHistory.create(
                    student.getId(),
                    OnHoldTypes.DISCIPLINARY_ISSUES,
                    true,
                    adminEmail
            );
            studentHoldHistoryRepo.save(history);
            System.out.println(student.getStudentId() + " Hold initialized");
        });
    }

    // --------------------------------------------------------------
    //  4) Courses
    // --------------------------------------------------------------
    private void initializeCourses() {
        if (courseRepo.count() == 0) {
            List<Course> courses = new ArrayList<>();

            // Helper lambdas to pick cost & level from the course code
            Function<String, Short> deriveLevel = code -> {
                if (code.equals("CS001")) return (short) 200;
                // extract numeric portion from code
                int num = Integer.parseInt(code.replaceAll("\\D", ""));
                int hundreds = num / 100;
                return (short) (hundreds * 100);
            };
            Function<Short, Double> deriveCost = lvl -> {
                switch (lvl) {
                    case 100: return 500.0;
                    case 200: return 600.0;
                    case 300: return 700.0;
                    case 400: return 800.0;
                    default:  return 600.0; // fallback
                }
            };

            // MA111
            courses.add(new Course(
                    "MA111",
                    "Calculus I",
                    "Introductory calculus: limits, derivatives, and integrals.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // MA161
            courses.add(new Course(
                    "MA161",
                    "Algebra and Trigonometry",
                    "Basic algebraic operations, functions, trig identities, and equations.",
                    15.0,
                    (short)100,
                    500.0,
                    false,
                    true
            ));

            // MG101
            courses.add(new Course(
                    "MG101",
                    "Introduction to Management",
                    "Overview of management principles and organizational behavior.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // ST131
            courses.add(new Course(
                    "ST131",
                    "Introduction to Statistics",
                    "Basic statistical methods, probability, and data analysis.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // YEAR-2 MISSING COURSES
            // UU200
            courses.add(new Course(
                    "UU200",
                    "Ethics & Governance",
                    "Explores ethics, governance, and civic responsibility in the modern world.",
                    15.0,
                    (short)200,
                    600.0,
                    true,
                    true
            ));

            // IS221
            courses.add(new Course(
                    "IS221",
                    "Introduction to Information Systems",
                    "Focuses on how IS support organizations; covers hardware, software, and databases.",
                    15.0,
                    (short)200,
                    600.0,
                    true,
                    true
            ));

            // IS222
            courses.add(new Course(
                    "IS222",
                    "Systems Analysis & Design",
                    "Covers requirements gathering, system modeling, and design approaches.",
                    15.0,
                    (short)200,
                    600.0,
                    true,
                    true
            ));

            // YEAR-3 MISSING COURSES
            // IS314
            courses.add(new Course(
                    "IS314",
                    "Database Systems",
                    "Relational models, SQL, and fundamentals of database design.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    true
            ));

            // IS328
            courses.add(new Course(
                    "IS328",
                    "E-Commerce Systems",
                    "Study of e-business frameworks, payment systems, and online security.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    true
            ));

            // IS333
            courses.add(new Course(
                    "IS333",
                    "Cloud Infrastructure",
                    "Introduction to cloud environments, virtualization, and deployment models.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    true
            ));

            // UU114
            courses.add(new Course(
                    "UU114",
                    "English Language Skills for Tertiary Studies",
                    "Focuses on academic reading, writing, listening, and speaking at tertiary level.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // UU100A
            courses.add(new Course(
                    "UU100A",
                    "Communications & Information Literacy",
                    "Develops capacity to locate, evaluate and use information effectively; RSD framework.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // CS001
            courses.add(new Course(
                    "CS001",
                    "Foundations of Professional Practice (FPP)",
                    "Intro to ICT professional roles, e-Portfolio use, and mentoring for skill development.",
                    15.0,
                    deriveLevel.apply("CS001"),
                    deriveCost.apply(deriveLevel.apply("CS001")),
                    true,
                    true
            ));

            // CS111
            courses.add(new Course(
                    "CS111",
                    "Introduction to Computing Science",
                    "Covers programming basics, problem solving, and intro to computer organization.",
                    15.0,
                    (short)100,
                    500.0,
                    true,
                    true
            ));

            // CS112
            courses.add(new Course(
                    "CS112",
                    "Data Structures & Algorithms",
                    "Focus on C++ programming, arrays, queues, stacks, trees; searching and sorting.",
                    15.0,
                    (short)100,
                    500.0,
                    false,
                    true
            ));

            // CS140
            courses.add(new Course(
                    "CS140",
                    "Introduction to Software Engineering",
                    "Covers the basics of software development life cycle, design, and testing.",
                    15.0,
                    (short)100,
                    500.0,
                    false,
                    true
            ));

            // CS150
            courses.add(new Course(
                    "CS150",
                    "Introduction to Computer Networks & Security",
                    "Fundamentals of network topologies, operating systems, and basic security concepts.",
                    15.0,
                    (short)100,
                    500.0,
                    false,
                    true
            ));

            // CS211
            courses.add(new Course(
                    "CS211",
                    "Computer Organisation",
                    "Data representation, logic circuits, CPU architecture, and assembly language.",
                    15.0,
                    (short)200,
                    600.0,
                    true,
                    false
            ));

            // CS214
            courses.add(new Course(
                    "CS214",
                    "Design & Analysis of Algorithms",
                    "Dynamic programming, divide-and-conquer, greedy strategies, and complexity.",
                    15.0,
                    (short)200,
                    600.0,
                    false,
                    true
            ));

            // CS215
            courses.add(new Course(
                    "CS215",
                    "Computer Communications & Management",
                    "TCP/IP fundamentals, access control, wireless network components, routing, subnetting.",
                    15.0,
                    (short)200,
                    600.0,
                    false,
                    true
            ));

            // CS218
            courses.add(new Course(
                    "CS218",
                    "Mobile Computing",
                    "Intro to mobile devices, networks, telephony, and hands-on mobile app development.",
                    15.0,
                    (short)200,
                    600.0,
                    false,
                    true
            ));

            // CS219
            courses.add(new Course(
                    "CS219",
                    "Cloud Computing",
                    "Covers cloud models, standards, deployment, privacy, and security issues.",
                    15.0,
                    (short)200,
                    600.0,
                    false,
                    true
            ));

            // CS230
            courses.add(new Course(
                    "CS230",
                    "Requirements Engineering",
                    "Requirement elicitation, analysis, validation, prioritization, and basic design.",
                    15.0,
                    (short)200,
                    600.0,
                    true,
                    false
            ));

            // CS241
            courses.add(new Course(
                    "CS241",
                    "Software Design & Implementation",
                    "Covers design, testing, documentation, and project-based software implementation.",
                    15.0,
                    (short)200,
                    600.0,
                    false,
                    true
            ));

            // CS310
            courses.add(new Course(
                    "CS310",
                    "Computer Networks",
                    "Focus on TCP/IP, IP addressing, routing, transport layers, and modern network concepts.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    false
            ));

            // CS311
            courses.add(new Course(
                    "CS311",
                    "Operating Systems",
                    "OS architectures, resource allocation, process scheduling, and memory management.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    false
            ));

            // CS317
            courses.add(new Course(
                    "CS317",
                    "Computer & Network Security",
                    "Intro to cryptography, network attacks, security protocols, and mitigation strategies.",
                    15.0,
                    (short)300,
                    700.0,
                    false,
                    true
            ));

            // CS324
            courses.add(new Course(
                    "CS324",
                    "Distributed Computing",
                    "Distributed systems, interprocess communication, multi-tier architectures, file sharing.",
                    15.0,
                    (short)300,
                    700.0,
                    false,
                    true
            ));

            // CS341
            courses.add(new Course(
                    "CS341",
                    "Software Quality Assurance & Testing",
                    "Static/dynamic testing, metrics, quality plans, testing tools, and risk assessment.",
                    15.0,
                    (short)300,
                    700.0,
                    false,
                    true
            ));

            // CS350
            courses.add(new Course(
                    "CS350",
                    "Wireless Networks",
                    "Covers wireless protocols, methods, standards, and emerging wireless technologies.",
                    15.0,
                    (short)300,
                    700.0,
                    false,
                    true
            ));

            // CS351
            courses.add(new Course(
                    "CS351",
                    "Network Design & Administration",
                    "Advanced networking problems, system administration, and resilient network strategies.",
                    15.0,
                    (short)300,
                    700.0,
                    false,
                    true
            ));

            // CS352
            courses.add(new Course(
                    "CS352",
                    "Cybersecurity Principles",
                    "Covers information assurance, cyber threats, defensive controls, and risk management.",
                    15.0,
                    (short)300,
                    700.0,
                    true,
                    false
            ));

            // CS400
            courses.add(new Course(
                    "CS400",
                    "Industry Experience Project",
                    "Capstone: real-life ICT project applying advanced skills in a professional setting.",
                    15.0,
                    (short)400,
                    800.0,
                    false,
                    true
            ));

            // CS401
            courses.add(new Course(
                    "CS401",
                    "Cybersecurity Principles (PG)",
                    "Foundations of cybersecurity threats, controls, and risk management for PG students.",
                    15.0,
                    (short)400,
                    800.0,
                    true,
                    false
            ));

            // CS402
            courses.add(new Course(
                    "CS402",
                    "Cybercrime",
                    "Overview of risks, threats, vulnerabilities, and strategies to combat cybercrime.",
                    15.0,
                    (short)400,
                    800.0,
                    false,
                    true
            ));

            // CS403
            courses.add(new Course(
                    "CS403",
                    "Cyber Defense: Governance & Risk Management",
                    "Focus on cyber governance, managing key systems, and risk policies (e.g., e-commerce).",
                    15.0,
                    (short)400,
                    800.0,
                    true,
                    false
            ));

            // CS404
            courses.add(new Course(
                    "CS404",
                    "Network Security Operations",
                    "Examines network threats, defense mechanisms, and hands-on mitigation strategies.",
                    15.0,
                    (short)400,
                    800.0,
                    false,
                    true
            ));

            // CS412
            courses.add(new Course(
                    "CS412",
                    "Artificial Intelligence",
                    "Explores AI areas: data science, ML, optimization, robotics, pattern recognition.",
                    15.0,
                    (short)400,
                    800.0,
                    true,
                    false
            ));

            // CS415
            courses.add(new Course(
                    "CS415",
                    "Advanced Software Engineering",
                    "Advanced theory, design, measurement, metrics, and testing in software engineering.",
                    15.0,
                    (short)400,
                    800.0,
                    true,
                    false
            ));

            // CS424
            courses.add(new Course(
                    "CS424",
                    "Big Data Technologies",
                    "Big data fundamentals, Hadoop ecosystem, Spark, and column-based DBMS (HBase, Cassandra).",
                    15.0,
                    (short)400,
                    800.0,
                    false,
                    true
            ));

            // CS427
            courses.add(new Course(
                    "CS427",
                    "Mobile Communications",
                    "Study of mobile communication principles, applications, and current developments.",
                    15.0,
                    (short)400,
                    800.0,
                    false,
                    true
            ));

            courseRepo.saveAll(courses);
            System.out.println("Courses initialized successfully.");
        } else {
            System.out.println("Courses already exist. Skipping initialization.");
        }
    }



    // --------------------------------------------------------------
    //  5) Programmes (BSE, BNS)
    // --------------------------------------------------------------
    private void initializeProgrammes() {
        /*
         * Remove BEE.
         * We'll keep BSE, and add BNS = "Bachelor of Networks & Security".
         */
        if (programmeRepo.count() == 0) {
            List<Programme> programmes = List.of(
                    new Programme("BSE", "Bachelor of Software Engineering", "STEMP"),
                    new Programme("BNS", "Bachelor of Networks & Security", "STEMP"),
                    new Programme("APD","Postgraduate Diploma","STEMP")
            );
            programmeRepo.saveAll(programmes);
            System.out.println("Programmes (BSE, BNS) initialized successfully.");
        } else {
            System.out.println("Programmes already exist. Skipping.");
        }
    }

    // --------------------------------------------------------------
    //  6) Link Courses to BSE and BNS
    // --------------------------------------------------------------
    private void initializeCourseProgrammes() {
        if (courseProgrammeRepo.count() == 0) {
            Programme bse = programmeRepo.findByProgrammeCode("BSE")
                    .orElseThrow(() -> new RuntimeException("BSE programme not found"));
            Programme bns = programmeRepo.findByProgrammeCode("BNS")
                    .orElseThrow(() -> new RuntimeException("BNS programme not found"));

            List<CourseProgramme> courseProgrammes = new ArrayList<>();

            /* ========== BSE ========== */
            // Year I
            for (String code : List.of("CS111","CS112","CS140","MA111","MA161","MG101","ST131","UU100A","UU114")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse, false));
            }
            // Year II
            for (String code : List.of("CS211","CS214","CS218","CS219","CS230","CS241","IS221","IS222","UU200","CS001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse, false));
            }
            // Year III
            for (String code : List.of("CS310","CS311","CS324","CS341","CS352","IS314","IS328","IS333")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse, false));
            }
            // Year IV
            for (String code : List.of("CS415","CS403","CS412","CS424","CS400")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse, false));
            }

            /* ========== BNS (Bachelor of Networks & Security) ========== */
            // Year I: CS111, CS112, CS150, MA111, MA161, MG101, ST131, UU100A, UU114
            for (String code : List.of("CS111","CS112","CS150","MA111","MA161","MG101","ST131","UU100A","UU114")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year II: CS211, CS214, CS215, CS218, CS219, IS221, IS222, UU200, CS001
            for (String code : List.of("CS211","CS214","CS215","CS218","CS219","IS221","IS222","UU200","CS001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year III: CS310, CS311, CS317, CS324, CS350, CS351, CS352, IS333
            for (String code : List.of("CS310","CS311","CS317","CS324","CS350","CS351","CS352","IS333")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year IV: any two of [CS403, CS412, CS424] plus CS400
            // We'll just link them all; actual "any two" logic can happen at enrollment time
            for (String code : List.of("CS403","CS412","CS424","CS400")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }

            courseProgrammeRepo.saveAll(courseProgrammes);
            System.out.println("Course-Programme links (BSE & BNS) initialized successfully.");
        } else {
            System.out.println("Course-Programme links already exist. Skipping.");
        }
    }

    // --------------------------------------------------------------
    //  7) Link Students to Programmes
    // --------------------------------------------------------------
    private void linkStudentsToProgrammes() {
        if(!studentProgrammeService.getAllStudentProgrammes().isEmpty()){
            System.out.println("Student-Programme links already initialized. Skipping initialization.");
            return;
        }

        List<Student> students = studentRepo.findAll();
        List<Programme> programmes = programmeRepo.findAll();

        if (students.isEmpty() || programmes.isEmpty()) {
            throw new RuntimeException("Must initialize students and programmes first!");
        }

        studentProgrammeService.saveStudentProgramme(students.get(0).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(1).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(2).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(3).getId(), programmes.get(1).getId(), true); // BNS
        studentProgrammeService.saveStudentProgramme(students.get(4).getId(), programmes.get(1).getId(), true); // BNS
        studentProgrammeService.saveStudentProgramme(students.get(5).getId(), programmes.get(2).getId(), true); // BNS

        System.out.println("Linked students to (BSE/BNS) programmes successfully.");
    }

    // --------------------------------------------------------------
    //  8) Course Enrollments
    // --------------------------------------------------------------
    private void initializeCourseEnrollments() {
        String studentId = "s11212749"; // Student ID to enroll
        // 1) Find the Student by ID
        Student student = studentRepo.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student with ID " + studentId + " not found"));

        // Avoid re-initializing if already done
        if (courseEnrollmentRepo.count() > 0) {
            System.out.println("Course enrollments already initialized. Skipping.");
            return;
        }

        // 2) Find the student's *current* StudentProgramme
        Optional<StudentProgramme> studentProg = studentProgrammeRepo.findByStudentAndCurrentProgrammeTrue(student);
        if(studentProg.isEmpty()){
            throw new RuntimeException("Student " + studentId + " is not linked to any programme!");
        }
        Programme currentProgramme = studentProg.get().getProgramme();
        List<Course> programmeCourses = courseProgrammeRepo.findAllByProgramme(currentProgramme);
        if (programmeCourses.size() < 8) {
            throw new RuntimeException("Not enough courses in the " + currentProgramme.getName() + " programme!");
        }

        // We'll pick 8 courses from that list
        List<CourseEnrollment> enrollments = new ArrayList<>();

        // 4) Create 4 "currently taking" enrollments (just grabbing the first 4 in the list)
        for (int i = 0; i < 4; i++) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(programmeCourses.get(i));
            enrollment.setCompleted(false);
            enrollment.setDateEnrolled(LocalDate.now().minusMonths(6));
            enrollment.setCurrentlyTaking(true);
            enrollment.setSemesterEnrolled(1);
            enrollments.add(enrollment);
        }

        // Create 4 "completed" enrollments (the next 4 in the list)
        for (int i = 4; i < 8; i++) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(programmeCourses.get(i));
            enrollment.setCompleted(true);
            enrollment.setDateEnrolled(LocalDate.now().minusYears(1));
            enrollment.setCurrentlyTaking(false);
            enrollment.setSemesterEnrolled(1);
            enrollments.add(enrollment);
        }

        // Finally, save to DB
        courseEnrollmentRepo.saveAll(enrollments);
        System.out.println("Initialized 4 currently taking and 4 completed course enrollments for student " + studentId
                + " in programme " + currentProgramme.getName());
    }

}
