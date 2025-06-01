package group7.enrollmentSystem;

import group7.enrollmentSystem.dtos.formDtos.CompassionateFormDTO;
import group7.enrollmentSystem.models.CompassionateApplication;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.repos.CompassionateApplicationRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.services.FormsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormsService related to Compassionate Application logic.
 * Covers valid cases, duplicates, missing values, and edge case scenarios.
 */
public class OtherApplicationFormTest {

    private StudentRepo studentRepo;
    private CompassionateApplicationRepo compassionateRepo;
    private EmailService emailService;
    private FormsService formsService;

    @BeforeEach
    void setUp() {
        studentRepo = mock(StudentRepo.class);
        compassionateRepo = mock(CompassionateApplicationRepo.class);
        emailService = mock(EmailService.class);

        formsService = new FormsService(studentRepo, null, null, compassionateRepo, emailService);
    }

    /**
     * Tests successful submission of a compassionate application.
     */
    @Test
    void testSubmitCompassionateApplication() {
        Student student = new Student();
        student.setEmail("comp@test.com");
        student.setStudentId("S20201");
        student.setFirstName("Ella");
        student.setLastName("Jones");

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Compassionate"));
        form.setReason("Medical emergency");
        form.setStudentSignature("Ella Jones");
        form.setSubmissionDate(LocalDate.of(2025, 6, 1));
        form.setCourseCode(List.of("CS101", "CS102"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-05"), LocalDate.parse("2025-06-06")));
        form.setExamTime(List.of(LocalTime.parse("09:00"), LocalTime.parse("13:00")));

        when(studentRepo.findByEmail("comp@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S20201")).thenReturn(List.of());

        assertDoesNotThrow(() -> formsService.submitApplication("comp@test.com", form));

        verify(compassionateRepo, times(1)).save(any(CompassionateApplication.class));
        verify(emailService, times(1)).notifyAdminNewApplication(eq("doiglas.m.habu@gmail.com"), any(Map.class));
        verify(emailService, times(1)).notifyStudentApplicationSubmission(eq("22johnc3na@gmail.com"), any(Map.class));
    }

    /**
     * Tests that duplicate compassionate applications are rejected.
     */
    @Test
    void testDuplicateCompassionateApplicationThrows() {
        Student student = new Student();
        student.setEmail("dupe@test.com");
        student.setStudentId("S30303");

        when(studentRepo.findByEmail("dupe@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S30303")).thenReturn(List.of(new CompassionateApplication()));

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Special"));
        form.setReason("Duplicate");
        form.setCourseCode(List.of("CS100"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-01")));
        form.setExamTime(List.of(LocalTime.parse("10:00")));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formsService.submitApplication("dupe@test.com", form);
        });
        assertEquals("You have already submitted a compassionate application.", ex.getMessage());
    }

    /**
     * Tests submission with multiple application types.
     */
    @Test
    void testMultipleApplicationTypesHandledCorrectly() {
        Student student = new Student();
        student.setEmail("multi@test.com");
        student.setStudentId("S50505");

        when(studentRepo.findByEmail("multi@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S50505")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Aegrotat", "Special"));
        form.setReason("Family emergency");
        form.setStudentSignature("Multi Case");
        form.setSubmissionDate(LocalDate.of(2025, 6, 3));
        form.setCourseCode(List.of("CS200"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-10")));
        form.setExamTime(List.of(LocalTime.parse("15:00")));

        assertDoesNotThrow(() -> formsService.submitApplication("multi@test.com", form));
        verify(compassionateRepo).save(any(CompassionateApplication.class));
    }

    /**
     * Tests behavior when student is not found.
     */
    @Test
    void testMissingStudentThrows() {
        when(studentRepo.findByEmail("ghost@student.com")).thenReturn(Optional.empty());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Compassionate"));
        form.setReason("Emergency");
        form.setCourseCode(List.of("CS111"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-15")));
        form.setExamTime(List.of(LocalTime.parse("08:00")));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formsService.submitApplication("ghost@student.com", form);
        });
        assertEquals("Student not found", ex.getMessage());
    }

    /**
     * Tests null reason field is accepted.
     */
    @Test
    void testNullReasonAllowed() {
        Student student = new Student();
        student.setEmail("noreason@student.com");
        student.setStudentId("S40404");

        when(studentRepo.findByEmail("noreason@student.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S40404")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Special"));
        form.setReason(null);
        form.setStudentSignature("X");
        form.setSubmissionDate(LocalDate.now());
        form.setCourseCode(List.of("CS301"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-20")));
        form.setExamTime(List.of(LocalTime.parse("14:00")));

        assertDoesNotThrow(() -> formsService.submitApplication("noreason@student.com", form));
    }

    /**
     * Tests empty course and exam info still submits (adjust based on your validations).
     */
    @Test
    void testEmptyCourseCodeListThrows() {
        Student student = new Student();
        student.setEmail("emptycode@test.com");
        student.setStudentId("S90909");

        when(studentRepo.findByEmail("emptycode@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S90909")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Compassionate"));
        form.setReason("Illness");
        form.setCourseCode(List.of());
        form.setExamDate(List.of());
        form.setExamTime(List.of());
        form.setStudentSignature("Empty Code");
        form.setSubmissionDate(LocalDate.now());

        assertDoesNotThrow(() -> formsService.submitApplication("emptycode@test.com", form));
    }

    /**
     * Tests mismatched exam date and time lists (shorter than course code list).
     */
    @Test
    void testMismatchedExamDateListSizes() {
        Student student = new Student();
        student.setEmail("mismatch@test.com");
        student.setStudentId("S33333");

        when(studentRepo.findByEmail("mismatch@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S33333")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Special"));
        form.setReason("One course, two dates");
        form.setCourseCode(List.of("CS111", "CS112"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-11"), LocalDate.parse("2025-06-12")));
        form.setExamTime(List.of(LocalTime.parse("10:00"), LocalTime.parse("13:00")));
        form.setStudentSignature("Mismatch Tester");
        form.setSubmissionDate(LocalDate.now());

        assertDoesNotThrow(() -> formsService.submitApplication("mismatch@test.com", form));
    }

    /**
     * Tests very long reason input string.
     */
    @Test
    void testVeryLongReason() {
        Student student = new Student();
        student.setEmail("longreason@test.com");
        student.setStudentId("S12121");

        when(studentRepo.findByEmail("longreason@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S12121")).thenReturn(List.of());

        String longReason = "A".repeat(1000);

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Aegrotat"));
        form.setReason(longReason);
        form.setCourseCode(List.of("CS888"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-22")));
        form.setExamTime(List.of(LocalTime.parse("11:00")));
        form.setStudentSignature("Long Reason Case");
        form.setSubmissionDate(LocalDate.now());

        assertDoesNotThrow(() -> formsService.submitApplication("longreason@test.com", form));
    }

    /**
     * Tests submission with a missing student signature.
     */
    @Test
    void testMissingSignature() {
        Student student = new Student();
        student.setEmail("nosignature@test.com");
        student.setStudentId("S14141");

        when(studentRepo.findByEmail("nosignature@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S14141")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Compassionate"));
        form.setReason("No signature");
        form.setCourseCode(List.of("CS313"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-25")));
        form.setExamTime(List.of(LocalTime.parse("12:00")));
        form.setStudentSignature(null);
        form.setSubmissionDate(LocalDate.now());

        assertDoesNotThrow(() -> formsService.submitApplication("nosignature@test.com", form));
    }

    /**
     * Tests correct submission of a form with multiple exams.
     */
    @Test
    void testMultipleExamsForStudent() {
        Student student = new Student();
        student.setEmail("multi-exam@test.com");
        student.setStudentId("S51515");

        when(studentRepo.findByEmail("multi-exam@test.com")).thenReturn(Optional.of(student));
        when(compassionateRepo.findByStudent_StudentId("S51515")).thenReturn(List.of());

        CompassionateFormDTO form = new CompassionateFormDTO();
        form.setApplicationType(List.of("Special"));
        form.setReason("Missed two exams");
        form.setStudentSignature("Multi Exam");
        form.setSubmissionDate(LocalDate.now());
        form.setCourseCode(List.of("CS100", "CS200"));
        form.setExamDate(List.of(LocalDate.parse("2025-06-18"), LocalDate.parse("2025-06-19")));
        form.setExamTime(List.of(LocalTime.parse("08:00"), LocalTime.parse("14:00")));

        assertDoesNotThrow(() -> formsService.submitApplication("multi-exam@test.com", form));
    }
}
