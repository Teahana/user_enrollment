package group7.enrollmentSystem.config;

public class CustomErrorHandler {

    // Custom error handling logic can be added here
    // For example, you can log the error details or send notifications

    // You can also create custom exception classes for specific error scenarios
    // and handle them in this class if needed


    public static class StudentNotFoundException extends RuntimeException {
        public StudentNotFoundException(String email) {
            super("Student " + email + " not found.");
        }
    }
    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String email) {
            super("User " + email + " not found.");
        }
    }
}
