package group7.enrollmentSystem.config;

import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.StudentProgrammeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CourseRepo courseRepo;
    private final ProgrammeRepo programmeRepo;
    private final StudentProgrammeService studentProgrammeService;
    private final StudentRepo studentRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final UserRepo userRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;
    private final PasswordEncoder passwordEncoder;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;

    @Override
    public void run(String... args) {
        initializeAdminUser();
        initializeStudents();
        initializeCourses();
        initializeProgrammes();
        initializeCoursePrerequisites();
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
            student.setRoles(Set.of("ROLE_STUDENT"));

            studentRepo.save(student);
            System.out.println("Registered student: " + email);
        });
    }


    private void initializeProgrammes() {
        if (programmeRepo.count() == 0) {
            List<Programme> programmes = List.of(
                    new Programme("BNS", "Bachelor of Networks & Security", "STEMP"),
                    new Programme("BSE", "Bachelor of Software Engineering", "STEMP"),
                    new Programme("BEC", "Bachelor of Engineering (Civil)", "STEMP"),
                    new Programme("BEE", "Bachelor of Engineering (Electrical & Electronics)", "STEMP"),
                    new Programme("BEM", "Bachelor of Engineering (Mechanical)", "STEMP")
            );
            programmeRepo.saveAll(programmes);
            System.out.println("Programmes initialized successfully.");
        } else {
            System.out.println("Programmes already exist. Skipping.");
        }
    }

    private void initializeCourses() {
        if (courseRepo.count() == 0) {

            List<Course> courses = List.of(
                    // -- BNS (Bachelor of Networks & Security) --
                    new Course("CS001", "Foundations of Professional Practice",
                            "Professional practice basics",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS111", "Introduction to Programming",
                            "Introductory programming concepts",
                            7.5, (short)100, 500.0, true, true),

                    new Course("CS112", "Intermediate Programming",
                            "Intermediate-level programming",
                            7.5, (short)100, 500.0, true, true),

                    new Course("CS150", "Computing Fundamentals",
                            "Basic computing principles",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MA111", "Calculus I",
                            "Introduction to calculus",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MA161", "Algebra & Trigonometry",
                            "Core algebra and trig",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MG101", "Introduction to Management",
                            "Basic management principles",
                            7.5, (short)100, 500.0, true, true),

                    new Course("ST131", "Statistics I",
                            "Basic statistics",
                            7.5, (short)100, 500.0, true, true),

                    new Course("UU100A", "Communication & Information Literacy",
                            "Academic writing & research",
                            7.5, (short)100, 500.0, true, true),

                    new Course("UU114", "English for Academic Purposes",
                            "Academic English skills",
                            7.5, (short)100, 500.0, true, true),

                    new Course("CS211", "Data Structures & Algorithms",
                            "Fundamental data structures",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS214", "Computer Organization",
                            "Intro to computer architecture",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS215", "Software Engineering Basics",
                            "Fundamentals of SE",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS218", "Discrete Mathematics for Computing",
                            "Sets, logic, combinatorics",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS219", "Systems Programming",
                            "Low-level programming concepts",
                            7.5, (short)200, 600.0, true, true),

                    new Course("IS221", "Information Systems Principles",
                            "Intro to IS in organizations",
                            7.5, (short)200, 600.0, true, true),

                    new Course("IS222", "Systems Analysis & Design",
                            "Methods for analyzing systems",
                            7.5, (short)200, 600.0, true, true),

                    new Course("UU200", "Ethics & Governance",
                            "Social ethics and governance",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS310", "Computer Networks",
                            "Principles of networking",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS311", "Operating Systems",
                            "OS concepts & design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS317", "Machine Learning",
                            "Intro to ML algorithms",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS324", "Cybersecurity Fundamentals",
                            "Security concepts & practice",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS350", "Artificial Intelligence",
                            "Basic AI concepts",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS351", "Software Project Management",
                            "Managing software projects",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS352", "Advanced Networking",
                            "Advanced network architectures",
                            7.5, (short)300, 700.0, true, true),

                    new Course("IS333", "Cloud Computing",
                            "Cloud infra & design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS400", "Industry Experience Project",
                            "Practical software/network project",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CS403", "Network Security Advanced Topics",
                            "In-depth security protocols",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CS412", "Advanced Database Systems",
                            "Complex DB design & optimization",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CS424", "Cloud & Virtualization Security",
                            "Secure virtualization & cloud",
                            7.5, (short)400, 800.0, true, true),

                    // -- BSE Extras --
                    new Course("CS140", "Web Development Fundamentals",
                            "Basic web tech & design",
                            7.5, (short)100, 500.0, true, true),

                    new Course("CS230", "Object-Oriented Analysis & Design",
                            "OO methodologies",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS241", "Mobile App Development",
                            "Developing mobile apps",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CS341", "Advanced Software Engineering",
                            "Design patterns & architecture",
                            7.5, (short)300, 700.0, true, true),

                    new Course("IS314", "Database Management Systems",
                            "Fundamentals of databases",
                            7.5, (short)300, 700.0, true, true),

                    new Course("IS328", "E-Commerce Systems",
                            "Online business & technology",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CS415", "Software Testing & Quality Assurance",
                            "Testing methodologies",
                            7.5, (short)400, 800.0, true, true),

                    // -- BEC (Civil Eng) --
                    new Course("PH102", "Physics 1",
                            "Basic mechanics and waves",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MM101", "Engineering Mathematics I",
                            "Engineering math fundamentals",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MA112", "Calculus II",
                            "Integration & series",
                            7.5, (short)100, 500.0, true, true),

                    new Course("CV211", "Structural Mechanics I",
                            "Basics of structural analysis",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CV212", "Surveying & Geomatics",
                            "Intro to surveying techniques",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EV201", "Environmental Engineering Fundamentals",
                            "Intro environmental issues",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EN001", "Industrial Work Experience",
                            "Supervised industry experience",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MA211", "Engineering Math II",
                            "Advanced engineering math",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MA272", "Advanced Calculus",
                            "Vector calculus, PDEs",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CV203", "Civil Drawing & Drafting",
                            "Intro to civil CAD tools",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CV222", "Material Science for Civil",
                            "Properties of civil materials",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CH205", "Chemistry for Engineers",
                            "Basic chemical principles",
                            7.5, (short)200, 600.0, true, true),

                    new Course("CV311", "Structural Mechanics II",
                            "Advanced structural analysis",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV312", "Hydraulics",
                            "Flow of fluids in civil systems",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV313", "Geotechnical Engineering",
                            "Soil mechanics & foundation",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV316", "Transportation Engineering I",
                            "Intro to road/transport design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV321", "Reinforced Concrete Design",
                            "Concrete structures design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV322", "Steel Design",
                            "Design of steel structures",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV323", "Water Resources Engineering",
                            "Hydrology & water mgmt",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV324", "Transportation Engineering II",
                            "Advanced transport design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("CV461", "Structural Analysis & Design",
                            "Complex structures analysis",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CV469", "Project Management for Engineers",
                            "PM concepts in civil eng",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CV488", "Special Topics in Civil Eng",
                            "Current trends in civil eng",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CV491", "Civil Engineering Project I",
                            "Capstone design project part 1",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CV492", "Civil Engineering Project II",
                            "Capstone design project part 2",
                            7.5, (short)400, 800.0, true, true),

                    new Course("CV499", "Internship/Thesis",
                            "Extended industry project",
                            7.5, (short)400, 800.0, true, true),

                    // Electives
                    new Course("CV462", "Bridge Engineering",
                            "Analysis/design of bridges",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV463", "Earthquake Engineering",
                            "Seismic design and analysis",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV464", "Urban Drainage Systems",
                            "Advanced drainage design",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV493", "Advanced Concrete Structures",
                            "In-depth reinforced concrete",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV494", "Geotechnical Design",
                            "Advanced soil-structure design",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV495", "Traffic Engineering",
                            "Advanced traffic analysis",
                            7.5, (short)400, 800.0, false, true),

                    new Course("CV496", "Sustainable Design",
                            "Green building design",
                            7.5, (short)400, 800.0, false, true),

                    // -- BEE (Electrical & Electronics) --
                    new Course("EE102", "Introduction to Electrical Engineering",
                            "Basic EEE principles",
                            7.5, (short)100, 500.0, true, true),

                    new Course("MM103", "Engineering Mathematics II",
                            "Continuation of Eng. Math I",
                            7.5, (short)100, 500.0, true, true),

                    new Course("EE212", "Circuit Analysis",
                            "Advanced circuit theory",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE213", "Electronics I",
                            "Intro to diodes, transistors",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE222", "Digital Systems",
                            "Intro to digital logic",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE211", "Signals & Systems",
                            "Continuous/discrete signals",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE224", "Electromagnetics I",
                            "Fields & waves",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE225", "Electrical Machines",
                            "Basic machines & drives",
                            7.5, (short)200, 600.0, true, true),

                    new Course("EE316", "Electronics II",
                            "Amplifiers & advanced circuits",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE312", "Control Systems I",
                            "Linear control systems",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE313", "Microprocessors",
                            "CPU architecture & assembly",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE314", "Power Systems",
                            "Transmission & distribution",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE321", "Signals & Systems II",
                            "Advanced signal processing",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE323", "Telecommunication Systems",
                            "Basics of telecom networks",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE325", "Embedded Systems",
                            "Microcontroller-based systems",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE326", "Power Electronics",
                            "Converters, inverters, design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("EE463", "Advanced Electronics Design",
                            "High-speed & RF design",
                            7.5, (short)400, 800.0, true, true),

                    new Course("EE464", "Control Systems II",
                            "Advanced control theory",
                            7.5, (short)400, 800.0, true, true),

                    new Course("EE488", "Special Topics in EEE",
                            "Current EEE innovations",
                            7.5, (short)400, 800.0, true, true),

                    new Course("EE491", "EEE Project I",
                            "Capstone project part 1",
                            7.5, (short)400, 800.0, true, true),

                    new Course("EE492", "EEE Project II",
                            "Capstone project part 2",
                            7.5, (short)400, 800.0, true, true),

                    new Course("EE499", "EEE Internship/Thesis",
                            "Extended EEE project",
                            7.5, (short)400, 800.0, true, true),

                    new Course("PH302", "Modern Physics",
                            "Quantum, relativity basics",
                            7.5, (short)400, 800.0, false, true),

                    new Course("EE461", "Renewable Energy Systems",
                            "Solar, wind, hydro design",
                            7.5, (short)400, 800.0, false, true),

                    new Course("EE462", "VLSI Design",
                            "Intro to chip design",
                            7.5, (short)400, 800.0, false, true),

                    new Course("EE467", "Power System Protection",
                            "Relays & protective devices",
                            7.5, (short)400, 800.0, false, true),

                    // -- BEM (Mechanical) --
                    new Course("MM221", "Dynamics I",
                            "Study of motion & forces",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM212", "Thermodynamics I",
                            "Energy, heat, work concepts",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM211", "Engineering Drawing",
                            "Drafting for mechanical eng",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM222", "Materials Science",
                            "Materials & their properties",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM223", "Mechanics of Materials",
                            "Stress/strain analysis",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM214", "Workshop Practice",
                            "Hands-on mechanical workshop",
                            7.5, (short)200, 600.0, true, true),

                    new Course("MM311", "Fluid Mechanics",
                            "Behavior of fluids",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM312", "Thermodynamics II",
                            "Advanced thermal systems",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM315", "Machine Design I",
                            "Design of mechanical parts",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM316", "Manufacturing Processes",
                            "Machining, casting, forming",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM321", "Machine Design II",
                            "Advanced mechanical design",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM322", "Heat Transfer",
                            "Conduction, convection, rad",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM323", "Dynamics II",
                            "Advanced kinematics & dynamics",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM324", "Control Systems (Mech)",
                            "Intro to mechanical controls",
                            7.5, (short)300, 700.0, true, true),

                    new Course("MM469", "Project Management for Mech Eng",
                            "Managing mechanical projects",
                            7.5, (short)400, 800.0, true, true),

                    new Course("MM488", "Special Topics in Mech Eng",
                            "Current mechanical trends",
                            7.5, (short)400, 800.0, true, true),

                    new Course("MM491", "Mechanical Project I",
                            "Capstone project part 1",
                            7.5, (short)400, 800.0, true, true),

                    new Course("MM492", "Mechanical Project II",
                            "Capstone project part 2",
                            7.5, (short)400, 800.0, true, true),

                    new Course("MM499", "Mech Internship/Thesis",
                            "Extended mechanical project",
                            7.5, (short)400, 800.0, true, true),

                    // Electives
                    new Course("MM461", "Automotive Engineering",
                            "Design of vehicle systems",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM462", "Aerodynamics",
                            "Airflow and flight basics",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM463", "Robotics",
                            "Kinematics & control of robots",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM465", "Advanced CAD/CAM",
                            "Computer-aided design & mfg",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM466", "Renewable Energy Systems (Mech)",
                            "Solar, wind, thermal systems",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM467", "Mechatronics",
                            "Electro-mechanical systems",
                            7.5, (short)400, 800.0, false, true),

                    new Course("MM468", "Finite Element Analysis",
                            "Numerical methods for analysis",
                            7.5, (short)400, 800.0, false, true)
            );

            courseRepo.saveAll(courses);
            System.out.println("Courses initialized successfully with double creditPoints, level=100..400, and cost.");
        } else {
            System.out.println("Courses already exist. Skipping.");
        }
    }

    private void initializeCoursePrerequisites() {
        if (coursePrerequisiteRepo.count() > 0) {
            System.out.println("Course prerequisites already initialized. Skipping.");
            return;
        }

        // Retrieve all courses from the database
        List<Course> allCourses = courseRepo.findAll();

        // Map to store course codes to course objects for quick lookup
        Map<String, Course> courseMap = allCourses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, course -> course));

        // List to store all CoursePrerequisite entities to be saved
        List<CoursePrerequisite> prerequisites = new ArrayList<>();

        // Define prerequisites for each course based on the handbook data
        // Format: courseCode -> list of prerequisite course codes
        Map<String, List<String>> prerequisiteMap = new HashMap<>();
        prerequisiteMap.put("CS001", List.of("UU100A"));
        prerequisiteMap.put("CS112", List.of("CS111"));
        prerequisiteMap.put("CS140", List.of()); // No prerequisites
        prerequisiteMap.put("CS150", List.of()); // No prerequisites
        prerequisiteMap.put("CS211", List.of("CS111"));
        prerequisiteMap.put("CS214", List.of("CS112"));
        prerequisiteMap.put("CS215", List.of("CS111", "CS150")); // CS111 and CS150
        prerequisiteMap.put("CS218", List.of("CS112"));
        prerequisiteMap.put("CS219", List.of("CS112"));
        prerequisiteMap.put("CS230", List.of("CS111", "CS140")); // CS111 and CS140
        prerequisiteMap.put("CS241", List.of("CS112", "CS230")); // CS112 and CS230
        prerequisiteMap.put("CS310", List.of("CS211"));
        prerequisiteMap.put("CS311", List.of("CS211"));
        prerequisiteMap.put("CS317", List.of("CS215"));
        prerequisiteMap.put("CS324", List.of("CS218", "CS219", "CS214", "CS215")); // CS218 or CS219 or CS214 or CS215
        prerequisiteMap.put("CS341", List.of("CS241"));
        prerequisiteMap.put("CS350", List.of("CS215"));
        prerequisiteMap.put("CS351", List.of("CS310"));
        prerequisiteMap.put("CS352", List.of()); // No prerequisites (admission to BSE or BNS)
        prerequisiteMap.put("CS400", List.of()); // Completion of all 100, 200, 300, and 400 level courses
        prerequisiteMap.put("CS403", List.of("CS401", "CS352")); // CS401 or CS352
        prerequisiteMap.put("CS412", List.of()); // Admission into Postgraduate Diploma or 75% completion of 300 level courses
        prerequisiteMap.put("CS415", List.of()); // Admission into Postgraduate Diploma or 75% completion of 300 level courses
        prerequisiteMap.put("CS424", List.of("CS324")); // CS324
        prerequisiteMap.put("CV203", List.of("MA112", "MM101")); // MA112 and MM101
        prerequisiteMap.put("CV211", List.of("MA112", "MM103")); // MA112 and MM103
        prerequisiteMap.put("CV212", List.of("MM103")); // MM103
        prerequisiteMap.put("CV222", List.of("PH102", "MA111", "MA112")); // PH102 and (MA111 or MA112)
        prerequisiteMap.put("CV311", List.of("MM212")); // MM212
        prerequisiteMap.put("CV312", List.of("CV211")); // CV211
        prerequisiteMap.put("CV313", List.of("MM222", "CV222")); // MM222 or CV222
        prerequisiteMap.put("CV316", List.of()); // 100% completion of 1st & 2nd year courses
        prerequisiteMap.put("CV321", List.of("CV311")); // CV311
        prerequisiteMap.put("CV322", List.of("CV313")); // CV313
        prerequisiteMap.put("CV323", List.of("MM312", "CV312")); // MM312 or CV312
        prerequisiteMap.put("CV324", List.of("CV311")); // CV311
        prerequisiteMap.put("CV461", List.of("CV323")); // CV323
        prerequisiteMap.put("CV462", List.of("CH205")); // CH205
        prerequisiteMap.put("CV463", List.of("CV461")); // CV461
        prerequisiteMap.put("CV464", List.of("CV311")); // CV311
        prerequisiteMap.put("CV469", List.of()); // 100% of 1st & 2nd year courses and 75% of 3rd year courses
        prerequisiteMap.put("CV488", List.of()); // Successful completion of all 100 and 200 level courses
        prerequisiteMap.put("CV491", List.of("CV322")); // CV322
        prerequisiteMap.put("CV492", List.of("CV324")); // CV324
        prerequisiteMap.put("CV493", List.of("CV323")); // CV323
        prerequisiteMap.put("CV494", List.of("CV492", "EV302")); // CV492 and EV302
        prerequisiteMap.put("CV495", List.of("CV322")); // CV322
        prerequisiteMap.put("CV496", List.of()); // Completion of all 100, 200, and 75% of 300 level courses
        prerequisiteMap.put("CV499", List.of("CV488")); // CV488
        prerequisiteMap.put("EE102", List.of("MA111", "PH102")); // MA111 and PH102
        prerequisiteMap.put("EE211", List.of("EE213", "MA112")); // EE213 and MA112
        prerequisiteMap.put("EE212", List.of("EE102")); // EE102
        prerequisiteMap.put("EE213", List.of("EE102", "MA112")); // EE102 and MA112
        prerequisiteMap.put("EE222", List.of("EE102", "MA112", "MA161")); // EE102 and (MA112 or MA161)
        prerequisiteMap.put("EE224", List.of("EE102", "MA211")); // EE102 and MA211
        prerequisiteMap.put("EE225", List.of("EE212", "EE213")); // EE212 and EE213
        prerequisiteMap.put("EE312", List.of("EE224")); // EE224
        prerequisiteMap.put("EE313", List.of("EE222")); // EE222
        prerequisiteMap.put("EE314", List.of()); // Completion of all 100 and 200 level courses
        prerequisiteMap.put("EE316", List.of()); // 100% completion of 1st & 2nd year courses
        prerequisiteMap.put("EE321", List.of("EE211")); // EE211
        prerequisiteMap.put("EE323", List.of("EE312")); // EE312
        prerequisiteMap.put("EE325", List.of("EE312", "EE213", "EE321", "EE225")); // EE312 and EE213 and EE321 and EE225
        prerequisiteMap.put("EE326", List.of("EE313", "CS211")); // EE313 and CS211
        prerequisiteMap.put("EE461", List.of()); // Completion of all 100, 200, and 75% of 300 level courses
        prerequisiteMap.put("EE462", List.of("EE323")); // EE323
        prerequisiteMap.put("EE463", List.of("EE312", "EE323")); // EE312 or EE323
        prerequisiteMap.put("EE464", List.of("EE321", "EE325")); // EE321 and EE325
        prerequisiteMap.put("EE467", List.of("PH302", "EE492")); // PH302 and EE492
        prerequisiteMap.put("EE488", List.of()); // Successful completion of all 100 and 200 level courses
        prerequisiteMap.put("EE491", List.of()); // Completion of all 100 and 200 level courses
        prerequisiteMap.put("EE492", List.of("EE224", "MA272", "EE323")); // EE224 and MA272 and EE323
        prerequisiteMap.put("EE499", List.of("EE488")); // EE488
        prerequisiteMap.put("EN001", List.of("MM101", "MM103", "EE102")); // MM101 and MM103 and EE102
        prerequisiteMap.put("IS221", List.of("CS111", "IS122", "IS104")); // CS111 or IS122 or IS104
        prerequisiteMap.put("IS222", List.of("CS111", "IS122", "IS104")); // CS111 or IS122 or IS104
        prerequisiteMap.put("IS226", List.of("IS222")); // IS222
        prerequisiteMap.put("IS302", List.of("IS202")); // IS202
        prerequisiteMap.put("IS314", List.of("IS222", "CS241", "CS214", "IS226")); // IS222 and (CS241 or CS214 or IS226)
        prerequisiteMap.put("IS322", List.of("IS222")); // IS222
        prerequisiteMap.put("IS328", List.of("IS222")); // IS222
        prerequisiteMap.put("IS333", List.of()); // Completion of 200 level CS/IS courses
        prerequisiteMap.put("IS351", List.of("IS222")); // IS222
        prerequisiteMap.put("IS413", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS414", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS421", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS428", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS431", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS432", List.of("IS431")); // IS431
        prerequisiteMap.put("IS433", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("IS434", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("MA111", List.of()); // Year 13/Form 7 Mathematics or equivalent
        prerequisiteMap.put("MA112", List.of()); // Year 13/Form 7 Mathematics or equivalent
        prerequisiteMap.put("MA161", List.of()); // Year 13/Form 7 Mathematics or equivalent
        prerequisiteMap.put("MA211", List.of("MA112")); // MA112
        prerequisiteMap.put("MA221", List.of("MA111")); // MA111
        prerequisiteMap.put("MA262", List.of("MA161", "MA111", "MA112")); // MA161 or MA111 or MA112
        prerequisiteMap.put("MA272", List.of("MA112")); // MA112
        prerequisiteMap.put("MA312", List.of("MA211")); // MA211
        prerequisiteMap.put("MA313", List.of("MA211")); // MA211
        prerequisiteMap.put("MA321", List.of("MA221")); // MA221
        prerequisiteMap.put("MA341", List.of("MA221", "MA211")); // MA221 or MA211
        prerequisiteMap.put("MA411", List.of("MA313")); // MA313
        prerequisiteMap.put("MA416", List.of("MA211", "MA312")); // MA211 and MA312
        prerequisiteMap.put("MM101", List.of()); // Admission into Undergraduate Programme
        prerequisiteMap.put("MM103", List.of("MA111", "PH102")); // MA111 and PH102
        prerequisiteMap.put("MM211", List.of("MA112", "MM103")); // MA112 and MM103
        prerequisiteMap.put("MM212", List.of("MM103")); // MM103
        prerequisiteMap.put("MM214", List.of("MM103")); // MM103
        prerequisiteMap.put("MM221", List.of("PH102", "MA111", "MA112")); // PH102 and (MA111 or MA112)
        prerequisiteMap.put("MM222", List.of("PH102", "MA111", "MA112")); // PH102 and (MA111 or MA112)
        prerequisiteMap.put("MM223", List.of("MM212")); // MM212
        prerequisiteMap.put("MM301", List.of("MA211")); // MA211
        prerequisiteMap.put("MM311", List.of("MM221", "MM222")); // MM221 and MM222
        prerequisiteMap.put("MM312", List.of("MM211")); // MM211
        prerequisiteMap.put("MM315", List.of("MM211")); // MM211
        prerequisiteMap.put("MM316", List.of()); // 100% completion of 1st & 2nd year courses
        prerequisiteMap.put("MM321", List.of("MM221")); // MM221
        prerequisiteMap.put("MM322", List.of("EE102")); // EE102
        prerequisiteMap.put("MM323", List.of("MM223")); // MM223
        prerequisiteMap.put("MM324", List.of("MM312", "MM315")); // MM312 and MM315
        prerequisiteMap.put("MM461", List.of("MM323")); // MM323
        prerequisiteMap.put("MM462", List.of("MM311")); // MM311
        prerequisiteMap.put("MM463", List.of("MM301", "MM311")); // MM301 and MM311
        prerequisiteMap.put("MM465", List.of("MM324")); // MM324
        prerequisiteMap.put("MM466", List.of()); // Completion of all 100, 200, and 75% of 300 level courses
        prerequisiteMap.put("MM467", List.of("MM324")); // MM324
        prerequisiteMap.put("MM468", List.of("MM214", "MA272")); // MM214 and MA272
        prerequisiteMap.put("MM469", List.of()); // 100% of 1st & 2nd year courses and 75% of 3rd year courses
        prerequisiteMap.put("MM488", List.of()); // Successful completion of all 100 and 200 level courses
        prerequisiteMap.put("MM491", List.of()); // Completion of all 100 and 200 level courses
        prerequisiteMap.put("MM492", List.of()); // Completion of all 100 and 200 level courses
        prerequisiteMap.put("MM499", List.of("MM488")); // MM488
        prerequisiteMap.put("PH102", List.of()); // Year 13/Form 7 Physics or equivalent
        prerequisiteMap.put("PH103", List.of()); // Year 13/Form 7 Physics or equivalent
        prerequisiteMap.put("PH106", List.of()); // Year 12/Form 6 Physics
        prerequisiteMap.put("PH202", List.of("PH102", "PH103", "MA111", "MA112")); // PH102 or PH103 and MA111 or MA112
        prerequisiteMap.put("PH204", List.of("PH102", "PH103", "MA112")); // PH102 or PH103 and MA112
        prerequisiteMap.put("PH206", List.of("PH102", "PH103", "MA111", "MA112")); // PH102 or PH103 and MA111 or MA112
        prerequisiteMap.put("PH301", List.of("PH202")); // PH202
        prerequisiteMap.put("PH302", List.of("PH206", "EE224", "EE212", "EE225")); // PH206 or EE224 and (EE212 or EE225)
        prerequisiteMap.put("PH304", List.of("PH204")); // PH204
        prerequisiteMap.put("PH306", List.of("PH206")); // PH206
        prerequisiteMap.put("PH402", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("PH407", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("PH414", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("PH416", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("PH420", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("PH421", List.of()); // Admission into Postgraduate Programme
        prerequisiteMap.put("ST130", List.of()); // Year 12/Form 6 Mathematics
        prerequisiteMap.put("ST131", List.of()); // Year 13/Form 7 Mathematics or equivalent
        prerequisiteMap.put("ST403", List.of("MA341")); // MA341
        prerequisiteMap.put("ST420", List.of()); // Admission into Postgraduate Programme

        // Iterate through the prerequisite map and create CoursePrerequisite entities
        for (Map.Entry<String, List<String>> entry : prerequisiteMap.entrySet()) {
            String courseCode = entry.getKey();
            List<String> prereqCodes = entry.getValue();

            Course course = courseMap.get(courseCode);
            if (course == null) {
                System.out.println("Course not found: " + courseCode);
                continue;
            }

            for (String prereqCode : prereqCodes) {
                Course prerequisite = courseMap.get(prereqCode);
                if (prerequisite == null) {
                    System.out.println("Prerequisite course not found: " + prereqCode);
                    continue;
                }

                CoursePrerequisite coursePrerequisite = new CoursePrerequisite();
                coursePrerequisite.setCourse(course);
                coursePrerequisite.setPrerequisite(prerequisite);
                prerequisites.add(coursePrerequisite);
            }
        }

        // Save all prerequisites to the database
        coursePrerequisiteRepo.saveAll(prerequisites);
        System.out.println("Course prerequisites initialized successfully.");
    }


    private void initializeCourseProgrammes() {
        if (courseProgrammeRepo.count() == 0) {
            // Retrieve all Programmes from DB (by code, or any other method).
            Programme bns = programmeRepo.findByProgrammeCode("BNS").orElseThrow();
            Programme bse = programmeRepo.findByProgrammeCode("BSE").orElseThrow();
            Programme bec = programmeRepo.findByProgrammeCode("BEC").orElseThrow();
            Programme bee = programmeRepo.findByProgrammeCode("BEE").orElseThrow();
            Programme bem = programmeRepo.findByProgrammeCode("BEM").orElseThrow();

            List<CourseProgramme> courseProgrammes = new ArrayList<>();

            // ---- Example: Link BNS courses ----
            // Year I
            for (String code : List.of("CS111","CS112","CS150","MA111","MA161","MG101","ST131","UU100A","UU114")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year II
            for (String code : List.of("CS211","CS214","CS215","CS218","CS219","IS221","IS222","UU200","CS001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year III
            for (String code : List.of("CS310","CS311","CS317","CS324","CS350","CS351","CS352","IS333")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }
            // Year IV
            for (String code : List.of("CS403","CS412","CS424","CS400")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns, false));
            }

            // ---- Link BSE courses ----
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
                // Note: The handbook says "one of CS403, CS412, CS424", but let's just link them all
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse, false));
            }

            // ---- Link BEC courses ----
            // Year I (Semester I + II combined here for simplicity)
            for (String code : List.of("PH102","MM101","UU114","MA111","UU100A","EE102","MM103","CS111","MA112")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec, false));
            }
            // Year II
            for (String code : List.of("MA211","CV211","CV212","EV201","EN001","MA272","CV203","CV222","CH205")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec, false));
            }
            // Year III
            for (String code : List.of("CV311","CV312","CV313","CV316","CV321","CV322","CV323","CV324","EN001")) {
                // The handbook repeats EN001 across multiple years, if needed.
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec, false));
            }
            // Year IV
            for (String code : List.of("CV461","CV469","CV488","CV491","CV492","CV499",
                    "CV462","CV463","CV464","CV493","CV494","CV495","CV496")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec, false));
            }

            // ---- Link BEE courses ----
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

            // ---- Link BEM courses ----
            // Year I
            for (String code : List.of("PH102","MM101","UU114","MA111","UU100A","EE102","MM103","CS111","MA112")) {
                // (similar to BEC year I data, so reusing the same set)
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem, false));
            }
            // Year II
            for (String code : List.of("MA211","MM221","MM212","MM211","EN001","MA272","MM222","MM223","MM214","EN001")) {
                // EN001 can appear multiple times in different semesters
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem, false));
            }
            // Year III
            for (String code : List.of("MM311","MM312","MM315","MM316","EN001","MM321","MM322","MM323","MM324")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem, false));
            }
            // Year IV
            for (String code : List.of("MM469","MM488","MM491","MM492","MM499","MM461","MM462","MM463","MM465","MM466","MM467","MM468")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem, false));
            }

            courseProgrammeRepo.saveAll(courseProgrammes);
            System.out.println("Course-Programme links initialized successfully.");
        } else {
            System.out.println("Course-Programme links already exist. Skipping.");
        }
    }

    private void linkStudentsToProgrammes() {
        if(!studentProgrammeService.getAllStudentProgrammes().isEmpty()){
            System.out.println("Student-Programme links already initialized. Skipping initialization.");
            return;
        }

        // Retrieve all initialized students and programmes
        List<Student> students = studentRepo.findAll();
        List<Programme> programmes = programmeRepo.findAll();

        if (students.isEmpty() || programmes.isEmpty()) {
            throw new RuntimeException("Must initialize students and programmes first!");
        }

        // Example association logic (simple example)
        studentProgrammeService.saveStudentProgramme(
                students.get(0).getId(),
                programmes.get(0).getId(),
                true
        );

        studentProgrammeService.saveStudentProgramme(
                students.get(1).getId(),
                programmes.get(1 % programmes.size()).getId(),
                true
        );

        studentProgrammeService.saveStudentProgramme(
                students.get(2).getId(),
                programmes.get(2 % programmes.size()).getId(),
                true
        );

        studentProgrammeService.saveStudentProgramme(
                students.get(3).getId(),
                programmes.get(0).getId(),
                true
        );

        studentProgrammeService.saveStudentProgramme(
                students.get(4).getId(),
                programmes.get(1 % programmes.size()).getId(),
                true
        );

        System.out.println("Clearly linked students to programmes successfully.");
    }
    private void initializeCourseEnrollments() {
        String studentId = "s11212749"; // Student ID
        Student student = studentRepo.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student with ID " + studentId + " not found"));

        if (courseEnrollmentRepo.count() > 0) {
            System.out.println("Course enrollments already initialized. Skipping.");
            return;
        }

        List<Course> courses = courseRepo.findAll();
        if (courses.size() < 8) {
            throw new RuntimeException("Not enough courses in the system!");
        }

        List<CourseEnrollment> enrollments = new ArrayList<>();

        // Create 4 currently taking enrollments
        for (int i = 0; i < 4; i++) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(courses.get(i));
            enrollment.setCompleted(false);
            enrollment.setDateEnrolled(LocalDate.now().minusMonths(6));
            enrollment.setCurrentlyTaking(true);
            enrollment.setSemesterEnrolled(1);
            enrollments.add(enrollment);
        }

        // Create 4 completed enrollments
        for (int i = 4; i < 8; i++) {
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(courses.get(i));
            enrollment.setCompleted(true);
            enrollment.setDateEnrolled(LocalDate.now().minusYears(1));
            enrollment.setCurrentlyTaking(false);
            enrollment.setSemesterEnrolled(1);
            enrollments.add(enrollment);
        }

        courseEnrollmentRepo.saveAll(enrollments);
        System.out.println("Initialized 4 currently taking and 4 completed course enrollments for student " + studentId);
    }

}
