package group7.enrollmentSystem.config;

import group7.enrollmentSystem.repos.CourseRepo;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DataInitializer {
    private final CourseRepo courseRepo;

}
