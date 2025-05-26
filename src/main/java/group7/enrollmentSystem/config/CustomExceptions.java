package group7.enrollmentSystem.config;


import group7.enrollmentSystem.enums.OnHoldTypes;
import org.springframework.security.authentication.DisabledException;

public class CustomExceptions {

    public static class StudentOnHoldException extends DisabledException {
        private final OnHoldTypes holdType;


        public static class ServiceRestrictedException extends RuntimeException {
            public ServiceRestrictedException(String message) {
                super(message);
            }
        }
        public StudentOnHoldException(OnHoldTypes holdType) {
            super(getHoldMessage(holdType));
            this.holdType = holdType;
        }

        public OnHoldTypes getHoldType() {
            return holdType;
        }

        public static String getHoldMessage(OnHoldTypes holdType) {
            return switch (holdType) {
                case UNPAID_FEES -> "Your account is on hold for unpaid fees. Please pay your fees to continue.";
                case UNPAID_REGISTRATION ->
                        "Your account is on hold for unpaid registration. Please complete your registration payment.";
                case DISCIPLINARY_ISSUES ->
                        "Your account is on hold due to disciplinary issues. Please contact the administration.";
                case UNSATISFACTORY_ACADEMIC_PROGRESS ->
                        "Your account is on hold due to unsatisfactory academic progress. Please meet with your advisor.";
                default -> "Your account is on hold. Please contact the administration.";
            };
        }
    }

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
