package group7.enrollmentSystem.config;

import group7.enrollmentSystem.models.Course;
import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final CourseRepo courseRepo;

    @Override
    public void run(String... args) {
        initializeCourses();
    }

    private void initializeCourses() {
        if (courseRepo.count() == 0) { // Prevent duplicate initialization
            List<Course> courses = List.of(
                    new Course("MA111", "Mathematics 1", "Mathematics foundation", (short)3, (short)1, true, true),
                    new Course("MA161", "Mathematics 2", "Advanced mathematics", (short)3, (short)1, true, false),
                    new Course("CS111", "Programming 1", "Intro to programming", (short)3, (short)1, true, true),
                    new Course("CS112", "Programming 2", "Object-oriented programming", (short)3, (short)1, false, true),
                    new Course("UU114", "English for Academic Purposes", "Academic writing skills", (short)3, (short)1, true, true),
                    new Course("CS211", "Data Structures & Algorithms", "Fundamentals of DS & Algo", (short)3, (short)2, true, true),
                    new Course("CS214", "Computer Organization", "Computer architecture basics", (short)3, (short)2, true, false),
                    new Course("IS222", "Information Systems Analysis", "Intro to system analysis", (short)3, (short)2, true, true),
                    new Course("CS215", "Software Engineering", "Intro to software engineering", (short)3, (short)2, false, true),
                    new Course("UU200", "Ethics and Governance", "Ethical decision making", (short)3, (short)2, true, true),
                    new Course("UU204", "Pacific Worlds", "Understanding the Pacific", (short)3, (short)2, true, true),
                    new Course("CS311", "Operating Systems", "OS concepts & design", (short)3, (short)3, true, true),
                    new Course("IS314", "Advanced Databases", "Database systems & optimization", (short)3, (short)3, true, false),
                    new Course("CS317", "Machine Learning", "Intro to ML", (short)3, (short)3, false, true),
                    new Course("CS350", "Artificial Intelligence", "AI fundamentals", (short)3, (short)3, true, true),
                    new Course("CS310", "Computer Networks", "Networking concepts", (short)3, (short)3, true, true),
                    new Course("CS324", "Cybersecurity", "Security concepts", (short)3, (short)3, true, false),
                    new Course("IS328", "E-Commerce Systems", "Online business systems", (short)3, (short)3, true, true),
                    new Course("IS333", "Cloud Computing", "Cloud infrastructure & design", (short)3, (short)3, true, false),
                    new Course("CS351", "Software Project Management", "Managing software projects", (short)3, (short)3, false, true)
            );

            courseRepo.saveAll(courses);
            System.out.println("Courses initialized successfully.");
        } else {
            System.out.println("Courses already initialized. Skipping data seeding.");
        }
    }
}

