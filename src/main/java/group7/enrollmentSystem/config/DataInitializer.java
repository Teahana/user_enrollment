package group7.enrollmentSystem.config;

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

    @Override
    public void run(String... args) {
        initializeAdminUser();
        initializeStudents();
        initializeCourses();
        initializeProgrammes();
        initializeCourseProgrammes();
        linkStudentsToProgrammes();
        initializeCourseEnrollments();
    }

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

    private void initializeStudents() {
        if (studentRepo.count() > 0) {
            System.out.println("Students already initialized. Skipping initialization.");
            return;
        }

        List<Student> students = List.of(
                new Student("s11212749", "Tino", "Potoi", "12 Bakshi Street, Suva", "1234567"),
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

    private void initializeProgrammes() {
        // Only keep BSE and BEE
        if (programmeRepo.count() == 0) {
            List<Programme> programmes = List.of(
                    new Programme("BSE", "Bachelor of Software Engineering", "STEMP"),
                    new Programme("BEE", "Bachelor of Engineering (Electrical & Electronics)", "STEMP")
            );
            programmeRepo.saveAll(programmes);
            System.out.println("Programmes (BSE, BEE) initialized successfully.");
        } else {
            System.out.println("Programmes already exist. Skipping.");
        }
    }

    private void initializeCourses() {
        if (courseRepo.count() == 0) {
            /*
             * Keep only the courses relevant to BSE and BEE.
             * BSE uses:
             *  Year 1:  CS111, CS112, CS140, MA111, MA161, MG101, ST131, UU100A, UU114
             *  Year 2:  CS211, CS214, CS218, CS219, CS230, CS241, IS221, IS222, UU200, CS001
             *  Year 3:  CS310, CS311, CS324, CS341, CS352, IS314, IS328, IS333
             *  Year 4:  CS415, CS403, CS412, CS424, CS400
             *
             * BEE uses:
             *  Year 1:  PH102, MM101, UU114, MA111, EE102, MM103, CS111, MA112
             *  Year 2:  EE212, EE213, EE222, MA211, EE211, EE224, EE225, MA272, EN001
             *  Year 3:  EE316, EE312, EE313, EE314, EE321, EE323, EE325, EE326
             *  Year 4:  EE463, EE464, EE488, EE491, EE492, EE499, PH302, EE461, EE462, EE467
             */
            List<Course> courses = List.of(
                    // -- Shared / BSE-lower-level / BEE-lower-level courses --
                    new Course("CS001",  "Foundations of Professional Practice",
                            "Professional practice basics",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("CS111", "Introduction to Programming",
                            "Introductory programming concepts",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("CS112", "Intermediate Programming",
                            "Intermediate-level programming",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MA111", "Calculus I",
                            "Introduction to calculus",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MA112", "Calculus II",
                            "Integration & series",
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
                    new Course("MA161", "Algebra & Trigonometry",
                            "Core algebra and trig",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("UU200", "Ethics & Governance",
                            "Social ethics and governance",
                            7.5, (short)200, 600.0,  true, true),

                    // BSE-specific courses
                    new Course("CS140", "Web Development Fundamentals",
                            "Basic web tech & design",
                            7.5, (short)100, 500.0,  true, true),
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

                    // BEE-specific courses
                    new Course("PH102", "Physics 1",
                            "Basic mechanics and waves",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MM101", "Engineering Mathematics I",
                            "Engineering math fundamentals",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("EE102", "Introduction to Electrical Engineering",
                            "Basic EEE principles",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("MM103", "Engineering Mathematics II",
                            "Continuation of Eng. Math I",
                            7.5, (short)100, 500.0,  true, true),
                    new Course("EE212", "Circuit Analysis",
                            "Advanced circuit theory",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE213", "Electronics I",
                            "Intro to diodes, transistors",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE222", "Digital Systems",
                            "Intro to digital logic",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("MA211", "Engineering Math II",
                            "Advanced engineering math",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE211", "Signals & Systems",
                            "Continuous/discrete signals",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE224", "Electromagnetics I",
                            "Fields & waves",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE225", "Electrical Machines",
                            "Basic machines & drives",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("MA272", "Advanced Calculus",
                            "Vector calculus, PDEs",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EN001", "Industrial Work Experience",
                            "Supervised industry experience",
                            7.5, (short)200, 600.0,  true, true),
                    new Course("EE316", "Electronics II",
                            "Amplifiers & advanced circuits",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE312", "Control Systems I",
                            "Linear control systems",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE313", "Microprocessors",
                            "CPU architecture & assembly",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE314", "Power Systems",
                            "Transmission & distribution",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE321", "Signals & Systems II",
                            "Advanced signal processing",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE323", "Telecommunication Systems",
                            "Basics of telecom networks",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE325", "Embedded Systems",
                            "Microcontroller-based systems",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE326", "Power Electronics",
                            "Converters, inverters, design",
                            7.5, (short)300, 700.0,  true, true),
                    new Course("EE463", "Advanced Electronics Design",
                            "High-speed & RF design",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("EE464", "Control Systems II",
                            "Advanced control theory",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("EE488", "Special Topics in EEE",
                            "Current EEE innovations",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("EE491", "EEE Project I",
                            "Capstone project part 1",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("EE492", "EEE Project II",
                            "Capstone project part 2",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("EE499", "EEE Internship/Thesis",
                            "Extended EEE project",
                            7.5, (short)400, 800.0,  true, true),
                    new Course("PH302", "Modern Physics",
                            "Quantum, relativity basics",
                            7.5, (short)400, 800.0,  false, true),
                    new Course("EE461", "Renewable Energy Systems",
                            "Solar, wind, hydro design",
                            7.5, (short)400, 800.0,  false, true),
                    new Course("EE462", "VLSI Design",
                            "Intro to chip design",
                            7.5, (short)400, 800.0,  false, true),
                    new Course("EE467", "Power System Protection",
                            "Relays & protective devices",
                            7.5, (short)400, 800.0,  false, true)
            );

            courseRepo.saveAll(courses);
            System.out.println("Courses (only for BSE & BEE) initialized successfully.");
        } else {
            System.out.println("Courses already exist. Skipping.");
        }
    }

    private void initializeCourseProgrammes() {
        if (courseProgrammeRepo.count() == 0) {
            // Retrieve BSE & BEE Programmes
            Programme bse = programmeRepo.findByProgrammeCode("BSE")
                    .orElseThrow(() -> new RuntimeException("BSE programme not found"));
            Programme bee = programmeRepo.findByProgrammeCode("BEE")
                    .orElseThrow(() -> new RuntimeException("BEE programme not found"));

            List<CourseProgramme> courseProgrammes = new ArrayList<>();

            /*
             * ---------- Link BSE courses ----------
             */
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

            /*
             * ---------- Link BEE courses ----------
             */
            // Year I
            for (String code : List.of("PH102","MM101","UU114","MA111","EE102","MM103","CS111","MA112")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee, false));
            }
            // Year II
            for (String code : List.of("EE212","EE213","EE222","MA211","EE211","EE224","EE225","MA272","EN001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee, false));
            }
            // Year III
            for (String code : List.of("EE316","EE312","EE313","EE314","EE321","EE323","EE325","EE326")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee, false));
            }
            // Year IV
            for (String code : List.of("EE463","EE464","EE488","EE491","EE492","EE499","PH302","EE461","EE462","EE467")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee, false));
            }

            courseProgrammeRepo.saveAll(courseProgrammes);
            System.out.println("Course-Programme links (BSE & BEE) initialized successfully.");
        } else {
            System.out.println("Course-Programme links already exist. Skipping.");
        }
    }

    private void linkStudentsToProgrammes() {
        // Link a few students to BSE or BEE
        if(!studentProgrammeService.getAllStudentProgrammes().isEmpty()){
            System.out.println("Student-Programme links already initialized. Skipping initialization.");
            return;
        }

        List<Student> students = studentRepo.findAll();
        List<Programme> programmes = programmeRepo.findAll();

        if (students.isEmpty() || programmes.isEmpty()) {
            throw new RuntimeException("Must initialize students and programmes first!");
        }

        // We'll just link the first few students, for example:
        // Student 0 -> BSE
        studentProgrammeService.saveStudentProgramme(
                students.get(0).getId(),
                programmes.get(0).getId(),
                true
        );
        // Student 1 -> BEE
        studentProgrammeService.saveStudentProgramme(
                students.get(1).getId(),
                programmes.get(1).getId(),
                true
        );
        // Student 2 -> BSE
        studentProgrammeService.saveStudentProgramme(
                students.get(2).getId(),
                programmes.get(0).getId(),
                true
        );
        // Student 3 -> BEE
        studentProgrammeService.saveStudentProgramme(
                students.get(3).getId(),
                programmes.get(1).getId(),
                true
        );
        // Student 4 -> BSE
        studentProgrammeService.saveStudentProgramme(
                students.get(4).getId(),
                programmes.get(0).getId(),
                true
        );

        System.out.println("Linked students to (BSE/BEE) programmes successfully.");
    }

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
