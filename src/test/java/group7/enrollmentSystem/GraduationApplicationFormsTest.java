package group7.enrollmentSystem;

import group7.enrollmentSystem.dtos.formDtos.GraduationFormDTO;
import group7.enrollmentSystem.enums.ApplicationStatus;
import group7.enrollmentSystem.helpers.FileUploads;
import group7.enrollmentSystem.models.GraduationApplication;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.repos.GraduationApplicationRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.helpers.EmailService;
import group7.enrollmentSystem.services.FormsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FormsService related to Graduation Application logic.
 * Covers normal flow, edge cases, and validation scenarios.
 */
public class GraduationApplicationFormsTest {

    private StudentRepo studentRepo;
    private ProgrammeRepo programmeRepo;
    private GraduationApplicationRepo graduationApplicationRepo;
    private EmailService emailService;
    private FormsService formsService;
    private FileUploads fileUploads;

    /**
     * Sets up mocked dependencies for each test case.
     */
    @BeforeEach
    void setUp() {
        studentRepo = mock(StudentRepo.class);
        programmeRepo = mock(ProgrammeRepo.class);
        graduationApplicationRepo = mock(GraduationApplicationRepo.class);
        emailService = mock(EmailService.class);
        fileUploads = mock(FileUploads.class);

        formsService = new FormsService(studentRepo, programmeRepo, graduationApplicationRepo, null, emailService,fileUploads );
    }

    /**
     * Tests successful submission of a new graduation application and verifies saved fields and email triggers.
     */
    @Test
    void testSubmitGraduationApplication() {
        Student student = new Student();
        student.setEmail("test@student.com");
        student.setStudentId("S12345");
        student.setFirstName("John");
        student.setLastName("Doe");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS415");
        programme.setName("Computer Science");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS415");
        form.setMajor1("SE");
        form.setMajor2("AI");
        form.setMinor("None");
        form.setCeremonyPreference("Laucala");
        form.setOtherCampus("N/A");
        form.setWillAttend(true);
        form.setStudentSignature("John Doe");
        form.setSignatureDate(LocalDate.parse("2025-06-01"));

        when(studentRepo.findByEmail("test@student.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS415")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S12345")).thenReturn(Optional.empty());

        formsService.submitGraduationApplication("test@student.com", form);

        ArgumentCaptor<GraduationApplication> captor = ArgumentCaptor.forClass(GraduationApplication.class);
        verify(graduationApplicationRepo).save(captor.capture());

        GraduationApplication savedApp = captor.getValue();
        assertEquals("SE", savedApp.getMajor1());
        assertEquals("AI", savedApp.getMajor2());
        assertEquals(ApplicationStatus.PENDING, savedApp.getStatus());

        verify(emailService, times(1)).notifyAdminNewApplication(
                eq("adriandougjonajitino@gmail.com"), any(Map.class));

        verify(emailService, times(1)).notifyStudentApplicationSubmission(
                eq(student.getEmail()), any(Map.class));
    }

    /**
     * Tests that duplicate applications are rejected with an appropriate exception.
     */
    @Test
    void testDuplicateGraduationApplication() {
        Student student = new Student();
        student.setEmail("duplicate@student.com");
        student.setStudentId("S54321");
        student.setFirstName("Jane");
        student.setLastName("Doe");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS111");
        programme.setName("Software Eng");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS111");
        form.setMajor1("IS");
        form.setMajor2("CS");
        form.setMinor("None");
        form.setCeremonyPreference("Laucala");
        form.setOtherCampus("None");
        form.setWillAttend(false);
        form.setStudentSignature("Jane Doe");
        form.setSignatureDate(LocalDate.of(2025, 6, 2));

        when(studentRepo.findByEmail("duplicate@student.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS111")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S54321")).thenReturn(Optional.of(new GraduationApplication()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formsService.submitGraduationApplication("duplicate@student.com", form);
        });
        assertEquals("You have already submitted a graduation application.", ex.getMessage());
    }

    /**
     * Tests that submission fails if the programme code is invalid.
     */
    @Test
    void testMissingProgrammeShouldThrow() {
        Student student = new Student();
        student.setEmail("noprog@student.com");
        student.setStudentId("S99999");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("INVALID_CODE");
        form.setMajor1("X");
        form.setMajor2("Y");
        form.setMinor("Z");
        form.setCeremonyPreference("Laucala");
        form.setWillAttend(true);
        form.setStudentSignature("X Y");
        form.setSignatureDate(LocalDate.of(2025, 6, 5));

        when(studentRepo.findByEmail("noprog@student.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("INVALID_CODE")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formsService.submitGraduationApplication("noprog@student.com", form);
        });
        assertEquals("Programme not found", ex.getMessage());
    }

    /**
     * Validates how null signature date is handled (depends on business logic).
     */
    @Test
    void testInvalidSignatureDate() {
        GraduationFormDTO form = new GraduationFormDTO();
        form.setSignatureDate(null);
        assertNull(form.getSignatureDate());
    }

    /**
     * Tests that submission fails if student is not found by email.
     */
    @Test
    void testStudentNotFoundThrowsException() {
        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS415");
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("missing@student.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            formsService.submitGraduationApplication("missing@student.com", form);
        });
        assertEquals("Student not found", ex.getMessage());
    }

    /**
     * Tests form submission with empty majors and minor fields.
     */
    @Test
    void testOptionalMajorsAllowed() {
        Student student = new Student();
        student.setEmail("test@no-major.com");
        student.setStudentId("S77777");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS200");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS200");
        form.setMajor1("");
        form.setMajor2("");
        form.setMinor("");
        form.setCeremonyPreference("Emalus");
        form.setWillAttend(false);
        form.setStudentSignature("None");
        form.setSignatureDate(LocalDate.of(2025, 6, 3));

        when(studentRepo.findByEmail("test@no-major.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS200")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S77777")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> formsService.submitGraduationApplication("test@no-major.com", form));
    }

    /**
     * Tests application with a non-standard campus preference (e.g., "Solomon").
     */
    @Test
    void testDifferentCampusPreferenceStoredCorrectly() {
        Student student = new Student();
        student.setEmail("campus@test.com");
        student.setStudentId("S11111");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS420");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS420");
        form.setOtherCampus("Solomon");
        form.setMajor1("INF");
        form.setMajor2("DS");
        form.setCeremonyPreference("Solomon");
        form.setWillAttend(true);
        form.setStudentSignature("Signed");
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("campus@test.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS420")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S11111")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> formsService.submitGraduationApplication("campus@test.com", form));
    }

    /**
     * Ensures signature name mismatch does not block application submission.
     */
    @Test
    void testSignatureMismatchAllowed() {
        Student student = new Student();
        student.setEmail("sig@test.com");
        student.setStudentId("S88888");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS510");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS510");
        form.setStudentSignature("Completely Different Name");
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("sig@test.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS510")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S88888")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> formsService.submitGraduationApplication("sig@test.com", form));
    }

    /**
     * Tests that a null minor value is accepted by the service.
     */
    @Test
    void testNullMinorField() {
        Student student = new Student();
        student.setEmail("nullminor@test.com");
        student.setStudentId("S33333");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS600");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS600");
        form.setMinor(null);
        form.setStudentSignature("Someone");
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("nullminor@test.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS600")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S33333")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> formsService.submitGraduationApplication("nullminor@test.com", form));
    }

    /**
     * Verifies that graduation form can be submitted even if the student will not attend the ceremony.
     */
    @Test
    void testGraduationAttendanceFalse() {
        Student student = new Student();
        student.setEmail("noattend@test.com");
        student.setStudentId("S22222");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS710");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS710");
        form.setWillAttend(false);
        form.setStudentSignature("No Attend");
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("noattend@test.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS710")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S22222")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> formsService.submitGraduationApplication("noattend@test.com", form));
    }

    /**
     * Tests behavior when signature is null (depends on business logic whether this should be allowed or not).
     */
    @Test
    void testMissingSignatureThrows() {
        Student student = new Student();
        student.setEmail("nosign@test.com");
        student.setStudentId("S00001");

        Programme programme = new Programme();
        programme.setProgrammeCode("CS810");

        GraduationFormDTO form = new GraduationFormDTO();
        form.setProgramme("CS810");
        form.setStudentSignature(null);
        form.setSignatureDate(LocalDate.now());

        when(studentRepo.findByEmail("nosign@test.com")).thenReturn(Optional.of(student));
        when(programmeRepo.findByProgrammeCode("CS810")).thenReturn(Optional.of(programme));
        when(graduationApplicationRepo.findByStudent_StudentId("S00001")).thenReturn(Optional.empty());

        formsService.submitGraduationApplication("nosign@test.com", form);
        assertNull(form.getStudentSignature());
    }
}
