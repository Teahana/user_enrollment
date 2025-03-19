package group7.enrollmentSystem.config;

import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.*;
import group7.enrollmentSystem.services.StudentProgrammeService;
import group7.enrollmentSystem.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

        userService.registerUser(adminEmail, password, adminFirstName, adminLastName, "ROLE_ADMIN", null);
        System.out.println("Admin user initialized successfully.");
    }


    private void initializeStudents() {
        if(studentRepo.count() > 0){
            System.out.println("Students already initialized. Skipping initialization.");
            return;
        }

        List<Student> students = List.of(
                createStudent("s11212749", "Tino", "Potoi", "12 Bakshi Street, Suva", "1234567"),
                createStudent("s11212750", "Jane", "Qio", "57 Ratu Mara Rd, Nabua", "2345678"),
                createStudent("s11212751", "Pita", "Kumar", "Lot 5 Brown Street, Lautoka", "3456789"),
                createStudent("s11212752", "Mary", "Singh", "Lot 3 Princes Rd, Tamavua", "4567890"),
                createStudent("s11212753", "Tomasi", "Prasad", "10 Victoria Parade, Suva", "5678901")
        );

        students.forEach(student -> {
            String email = student.getStudentId() + "@student.usp.ac.fj";
            String password = "12345"; // default sample password for initialization

            userService.registerUser(email, password, student.getFirstName(), student.getLastName(),"ROLE_STUDENT", student);
            System.out.println("Registered student user: " + email);
        });
    }

    private Student createStudent(String studentId, String firstName, String lastName, String address, String phoneNumber) {
        Student student = new Student();
        student.setStudentId(studentId);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setAddress(address);
        student.setPhoneNumber(phoneNumber);
        return student;
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
                courseProgrammes.add(new CourseProgramme(null, c, bns));
            }
            // Year II
            for (String code : List.of("CS211","CS214","CS215","CS218","CS219","IS221","IS222","UU200","CS001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns));
            }
            // Year III
            for (String code : List.of("CS310","CS311","CS317","CS324","CS350","CS351","CS352","IS333")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns));
            }
            // Year IV
            for (String code : List.of("CS403","CS412","CS424","CS400")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bns));
            }

            // ---- Link BSE courses ----
            // Year I
            for (String code : List.of("CS111","CS112","CS140","MA111","MA161","MG101","ST131","UU100A","UU114")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse));
            }
            // Year II
            for (String code : List.of("CS211","CS214","CS218","CS219","CS230","CS241","IS221","IS222","UU200","CS001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse));
            }
            // Year III
            for (String code : List.of("CS310","CS311","CS324","CS341","CS352","IS314","IS328","IS333")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse));
            }
            // Year IV
            for (String code : List.of("CS415","CS403","CS412","CS424","CS400")) {
                // Note: The handbook says "one of CS403, CS412, CS424", but let's just link them all
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bse));
            }

            // ---- Link BEC courses ----
            // Year I (Semester I + II combined here for simplicity)
            for (String code : List.of("PH102","MM101","UU114","MA111","UU100A","EE102","MM103","CS111","MA112")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec));
            }
            // Year II
            for (String code : List.of("MA211","CV211","CV212","EV201","EN001","MA272","CV203","CV222","CH205")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec));
            }
            // Year III
            for (String code : List.of("CV311","CV312","CV313","CV316","CV321","CV322","CV323","CV324","EN001")) {
                // The handbook repeats EN001 across multiple years, if needed.
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec));
            }
            // Year IV
            for (String code : List.of("CV461","CV469","CV488","CV491","CV492","CV499",
                    "CV462","CV463","CV464","CV493","CV494","CV495","CV496")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bec));
            }

            // ---- Link BEE courses ----
            // Year I
            for (String code : List.of("PH102","MM101","UU114","MA111","EE102","MM103","CS111","MA112")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee));
            }
            // Year II
            for (String code : List.of("EE212","EE213","EE222","MA211","EE211","EE224","EE225","MA272","EN001")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee));
            }
            // Year III
            for (String code : List.of("EE316","EE312","EE313","EE314","EE321","EE323","EE325","EE326")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee));
            }
            // Year IV
            for (String code : List.of("EE463","EE464","EE488","EE491","EE492","EE499","PH302","EE461","EE462","EE467")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bee));
            }

            // ---- Link BEM courses ----
            // Year I
            for (String code : List.of("PH102","MM101","UU114","MA111","UU100A","EE102","MM103","CS111","MA112")) {
                // (similar to BEC year I data, so reusing the same set)
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem));
            }
            // Year II
            for (String code : List.of("MA211","MM221","MM212","MM211","EN001","MA272","MM222","MM223","MM214","EN001")) {
                // EN001 can appear multiple times in different semesters
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem));
            }
            // Year III
            for (String code : List.of("MM311","MM312","MM315","MM316","EN001","MM321","MM322","MM323","MM324")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem));
            }
            // Year IV
            for (String code : List.of("MM469","MM488","MM491","MM492","MM499","MM461","MM462","MM463","MM465","MM466","MM467","MM468")) {
                Course c = courseRepo.findByCourseCode(code).orElseThrow();
                courseProgrammes.add(new CourseProgramme(null, c, bem));
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
