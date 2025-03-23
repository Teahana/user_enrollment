package group7.enrollmentSystem.config;

import group7.enrollmentSystem.enums.PrerequisiteType;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.StudentProgrammeService;
import group7.enrollmentSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final EnrollmentStatusRepo enrollmentStatusRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;

    @Override
    public void run(String... args) {
        initializeCourseEnrollmentStatus();
        initializeAdminUser();
        initializeStudents();
        initializeCourses();
        initializeProgrammes();         // Now BSE & BNS
        initializeCourseProgrammes();   // Link BSE & BNS
        linkStudentsToProgrammes();     // Assign students to BSE or BNS
        initializeCourseEnrollments();
    }

    // --------------------------------------------------------------
    //  1) Enrollment Status
    // --------------------------------------------------------------
    private void initializeCourseEnrollmentStatus() {
        if(enrollmentStatusRepo.count() > 0){
            System.out.println("Enrollment status already initialized. Skipping.");
            return;
        }
        EnrollmentState enrollmentState = new EnrollmentState();
        enrollmentState.setId(1L);
        enrollmentState.setOpen(true);
        enrollmentStatusRepo.save(enrollmentState);
    }

    // --------------------------------------------------------------
    //  2) Admin User
    // --------------------------------------------------------------
    private void initializeAdminUser() {
        String adminEmail = "admin@gmail.com";
        String adminFirstName = "Adrian";
        String adminLastName = "Alamu";
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
            student.setRoles(Set.of("STUDENT"));

            studentRepo.save(student);
            System.out.println("Registered student: " + email);
        });
    }

    // --------------------------------------------------------------
    //  4) Courses
    // --------------------------------------------------------------
    private void initializeCourses() {
        if (courseRepo.count() == 0) {
            /*
             * We keep BSE and BNS only. We'll remove all BEE references,
             * and add the BNS courses (including new ones: CS150, CS215, CS317, CS350, CS351).
             */
            List<Course> courses = List.of(
                    // ---------- Shared or BSE-lower-level courses ----------
                    new Course("CS001",  "Foundations of Professional Practice",
                            "Professional practice basics",
                            7.5, (short)200, 600.0,  true, true),

                    new Course("CS111", "Introduction to Programming",
                            "Introductory programming concepts",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("CS112", "Intermediate Programming",
                            "Intermediate-level programming",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("CS140", "Web Development Fundamentals",
                            "Basic web tech & design",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MA111", "Calculus I",
                            "Introduction to calculus",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MA161", "Algebra & Trigonometry",
                            "Core algebra and trig",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MG101", "Introduction to Management",
                            "Basic management principles",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("ST131", "Statistics I",
                            "Basic statistics",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("UU100A","Communication & Information Literacy",
                            "Academic writing & research",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("UU114", "English for Academic Purposes",
                            "Academic English skills",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("UU200", "Ethics & Governance",
                            "Social ethics and governance",
                            7.5, (short)200, 600.0,  true, true),

                    // ---------- Additional BNS-lower-level courses ----------
                    // BNS Year I has CS150
                    new Course("CS150", "Computing Fundamentals",
                            "Basic computing principles",
                            7.5, (short)100, 500.0,  true, true),
                    // BNS Year II has CS215
                    new Course("CS215", "Software Engineering Basics",
                            "Fundamentals of SE",
                            7.5, (short)200, 600.0,  true, true),

                    // ---------- BSE-specific courses (existing) ----------
                    new Course("CS211", "Data Structures & Algorithms",
                            "Fundamental data structures",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS214", "Computer Organization",
                            "Intro to computer architecture",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS218", "Discrete Mathematics for Computing",
                            "Sets, logic, combinatorics",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS219", "Systems Programming",
                            "Low-level programming concepts",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS230", "Object-Oriented Analysis & Design",
                            "OO methodologies",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS241", "Mobile App Development",
                            "Developing mobile apps",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS310", "Computer Networks",
                            "Principles of networking",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS311", "Operating Systems",
                            "OS concepts & design",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS324", "Cybersecurity Fundamentals",
                            "Security concepts & practice",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS341", "Advanced Software Engineering",
                            "Design patterns & architecture",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS352", "Advanced Networking",
                            "Advanced network architectures",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS400", "Industry Experience Project",
                            "Practical software/network project",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("CS403", "Network Security Advanced Topics",
                            "In-depth security protocols",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("CS412", "Advanced Database Systems",
                            "Complex DB design & optimization",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("CS415", "Software Testing & Quality Assurance",
                            "Testing methodologies",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("CS424", "Cloud & Virtualization Security",
                            "Secure virtualization & cloud",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("IS221", "Information Systems Principles",
                            "Intro to IS in organizations",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("IS222", "Systems Analysis & Design",
                            "Methods for analyzing systems",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("IS314", "Database Management Systems",
                            "Fundamentals of databases",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("IS328", "E-Commerce Systems",
                            "Online business & technology",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("IS333", "Cloud Computing",
                            "Cloud infra & design",
                            7.5, (short)300, 700.0,  true, true),

                    // ---------- Additional BNS courses not in BSE ----------
                    // BNS Year III references: CS317, CS350, CS351
                    new Course("CS317", "Machine Learning",
                            "Intro to ML algorithms",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS350", "Artificial Intelligence",
                            "Basic AI concepts",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("CS351", "Software Project Management",
                            "Managing software projects",
                            7.5, (short)300, 700.0,  true, true)
            );

            courseRepo.saveAll(courses);
            System.out.println("Courses (for BSE & BNS) initialized successfully.");
        } else {
            System.out.println("Courses already exist. Skipping.");
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
                    new Programme("BNS", "Bachelor of Networks & Security", "STEMP")
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

        // Suppose we link first 3 to BSE, last 2 to BNS, as an example
        // (Or however you wish)
        studentProgrammeService.saveStudentProgramme(students.get(0).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(1).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(2).getId(), programmes.get(0).getId(), true); // BSE
        studentProgrammeService.saveStudentProgramme(students.get(3).getId(), programmes.get(1).getId(), true); // BNS
        studentProgrammeService.saveStudentProgramme(students.get(4).getId(), programmes.get(1).getId(), true); // BNS

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
        StudentProgramme studentProg = studentProgrammeService.getAllStudentProgrammes().stream()
                .filter(sp -> sp.getStudent().equals(student) && sp.isCurrentProgramme())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No current programme found for student " + studentId));

        Programme currentProgramme = studentProg.getProgramme();
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
