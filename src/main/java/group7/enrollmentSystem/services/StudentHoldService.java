package group7.enrollmentSystem.services;

import group7.enrollmentSystem.config.CustomExceptions;
import group7.enrollmentSystem.dtos.classDtos.HoldRestrictionDto;
import group7.enrollmentSystem.dtos.classDtos.StudentHoldDto;
import group7.enrollmentSystem.dtos.classDtos.StudentHoldHistoryDto;
import group7.enrollmentSystem.dtos.classDtos.StudentHoldViewDto;
import group7.enrollmentSystem.dtos.serverKtDtos.MessageDto;
import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.models.HoldServiceRestriction;
import group7.enrollmentSystem.models.OnHoldStatus;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentHoldHistory;
import group7.enrollmentSystem.repos.HoldServiceRestrictionRepo;
import group7.enrollmentSystem.repos.StudentHoldHistoryRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHoldService {
    private final StudentRepo studentRepo;
    private final StudentHoldHistoryRepo studentHoldHistoryRepo;
    private final HoldServiceRestrictionRepo restrictionRepo;

    public enum HoldRestrictionType {
        COURSE_ENROLLMENT,
        VIEW_COMPLETED_COURSES,
        STUDENT_AUDIT,
        GENERATE_TRANSCRIPT,
        FORMS_APPLICATION,
    }

    public StudentHoldViewDto getStudentHoldDetails(String email) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentHoldViewDto dto = new StudentHoldViewDto();
        dto.setStudentId(student.getStudentId());
        dto.setFullName(student.getFirstName() + " " + student.getLastName());
        dto.setEmail(student.getEmail());

        List<OnHoldStatus> activeHolds = student.getOnHoldStatusList().stream()
                .filter(OnHoldStatus::isOnHold)
                .toList();

        dto.setHasHold(!activeHolds.isEmpty());
        dto.setActiveHolds(activeHolds);

        if (dto.isHasHold()) {
            dto.setHoldMessage("Account has " + activeHolds.size() + " active hold(s)");
            applyServiceRestrictions(dto, activeHolds);
        } else {
            dto.setHoldMessage("No active holds");
            enableAllServices(dto);
        }

        return dto;
    }

    public void checkAccess(String email, HoldRestrictionType restrictionType) {
        if (hasRestriction(email, restrictionType)) {
            throw new CustomExceptions.StudentOnHoldException(
                    getFirstRestrictingHoldType(email, restrictionType)
            );
        }
    }

    public boolean hasRestriction(String email, HoldRestrictionType restrictionType) {
        return getFirstRestrictingHoldType(email, restrictionType) != null;
    }

    private OnHoldTypes getFirstRestrictingHoldType(String email, HoldRestrictionType restrictionType) {
        Student student = studentRepo.findByEmail(email)
                .orElseThrow(() -> new CustomExceptions.StudentNotFoundException(email));

        return student.getOnHoldStatusList().stream()
                .filter(OnHoldStatus::isOnHold)
                .map(OnHoldStatus::getOnHoldType)
                .filter(holdType -> isRestricted(holdType, restrictionType))
                .findFirst()
                .orElse(null);
    }

    private boolean isRestricted(OnHoldTypes holdType, HoldRestrictionType restrictionType) {
        HoldServiceRestriction restriction = restrictionRepo.findByHoldType(holdType);
        if (restriction == null) return false;

        return switch (restrictionType) {
            case COURSE_ENROLLMENT -> restriction.isBlockCourseEnrollment();
            case VIEW_COMPLETED_COURSES -> restriction.isBlockViewCompletedCourses();
            case STUDENT_AUDIT -> restriction.isBlockStudentAudit();
            case GENERATE_TRANSCRIPT -> restriction.isBlockGenerateTranscript();
            case FORMS_APPLICATION -> restriction.isBlockForms();
        };
    }

    private void applyServiceRestrictions(StudentHoldViewDto dto, List<OnHoldStatus> activeHolds) {
        enableAllServices(dto); // Start with all enabled

        activeHolds.forEach(hold -> {
            HoldServiceRestriction restriction = restrictionRepo.findByHoldType(hold.getOnHoldType());
            if (restriction != null) {
                if (restriction.isBlockCourseEnrollment()) dto.setCanRegisterCourses(false);
                if (restriction.isBlockViewCompletedCourses()) dto.setCanViewCompletedCourses(false);
                if (restriction.isBlockStudentAudit()) dto.setCanViewStudentAudit(false);
                if (restriction.isBlockGenerateTranscript()) dto.setCanGenerateTranscript(false);
                if (restriction.isBlockForms()) dto.setCanApplyForGraduation(false);
            }
        });
    }

    public List<StudentHoldDto> getAllStudentsWithHoldStatus() {
        List<Student> students = studentRepo.findAllStudentsWithHolds();
        return students.stream().map(student -> {
            List<OnHoldTypes> activeHolds = student.getOnHoldStatusList().stream()
                    .filter(OnHoldStatus::isOnHold)
                    .map(OnHoldStatus::getOnHoldType)
                    .collect(Collectors.toList());

            return new StudentHoldDto(
                    student.getId(),
                    student.getEmail(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getStudentId(),
                    !activeHolds.isEmpty(),
                    activeHolds
            );
        }).collect(Collectors.toList());
    }

    public StudentHoldDto getStudentHolds(Long studentId) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<OnHoldTypes> activeHolds = student.getOnHoldStatusList().stream()
                .filter(OnHoldStatus::isOnHold)
                .map(OnHoldStatus::getOnHoldType)
                .collect(Collectors.toList());

        return new StudentHoldDto(
                student.getId(),
                student.getEmail(),
                student.getFirstName(),
                student.getLastName(),
                student.getStudentId(),
                !activeHolds.isEmpty(),
                activeHolds
        );
    }

    public ResponseEntity<MessageDto> placeHold(Long studentId, OnHoldTypes holdType, String actionBy) {
        try {
            placeStudentOnHold(studentId, holdType, actionBy);
            return ResponseEntity.ok(new MessageDto("Hold placed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageDto(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageDto("Error placing hold"));
        }
    }

    public ResponseEntity<MessageDto> removeHold(Long studentId, OnHoldTypes holdType, String actionBy) {
        try {
            removeHoldFromStudent(studentId, holdType, actionBy);
            return ResponseEntity.ok(new MessageDto("Hold removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new MessageDto("Error removing hold"));
        }
    }

    public ResponseEntity<List<OnHoldTypes>> getHoldTypes() {
        return ResponseEntity.ok(Arrays.asList(OnHoldTypes.values()));
    }

    public HoldRestrictionDto convertToDto(HoldServiceRestriction restriction) {
        HoldRestrictionDto dto = new HoldRestrictionDto();
        dto.setId(restriction.getId());
        dto.setHoldType(restriction.getHoldType());
        dto.setBlockCourseEnrollment(restriction.isBlockCourseEnrollment());
        dto.setBlockViewCompletedCourses(restriction.isBlockViewCompletedCourses());
        dto.setBlockStudentAudit(restriction.isBlockStudentAudit());
        dto.setBlockGenerateTranscript(restriction.isBlockGenerateTranscript());
        dto.setBlockGraduationApplication(restriction.isBlockForms());
        return dto;
    }

    @Transactional
    public void placeStudentOnHold(Long studentId, OnHoldTypes holdType, String actionBy) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if this hold type already exists and is active for the student
        boolean alreadyHasHold = student.getOnHoldStatusList().stream()
                .anyMatch(h -> h.getOnHoldType() == holdType && h.isOnHold());

        if (alreadyHasHold) {
            throw new IllegalArgumentException("Student already has an active " + holdType + " hold");
        }

        // Check if this hold type exists but is inactive
        Optional<OnHoldStatus> existingInactiveHold = student.getOnHoldStatusList().stream()
                .filter(h -> h.getOnHoldType() == holdType && !h.isOnHold())
                .findFirst();

        if (existingInactiveHold.isPresent()) {
            // Reactivate existing hold
            OnHoldStatus hold = existingInactiveHold.get();
            hold.setOnHold(true);
        } else {
            // Add new hold
            OnHoldStatus holdStatus = new OnHoldStatus();
            holdStatus.setOnHoldType(holdType);
            holdStatus.setOnHold(true);
            student.getOnHoldStatusList().add(holdStatus);
        }

        studentRepo.save(student);

        // Record in history
        StudentHoldHistory history = StudentHoldHistory.create(studentId, holdType, true, actionBy);
        studentHoldHistoryRepo.save(history);
    }

    @Transactional
    public void removeHoldFromStudent(Long studentId, OnHoldTypes holdType, String actionBy) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Find and deactivate the specific hold
        student.getOnHoldStatusList().stream()
                .filter(h -> h.getOnHoldType() == holdType && h.isOnHold())
                .findFirst()
                .ifPresent(hold -> {
                    hold.setOnHold(false);
                    studentRepo.save(student);

                    // Record in history
                    StudentHoldHistory history = StudentHoldHistory.create(
                            studentId, holdType, false, actionBy);
                    studentHoldHistoryRepo.save(history);
                });
    }

    public List<StudentHoldHistoryDto> getAllHoldHistory() {
        return studentHoldHistoryRepo.findAllHoldsHistory();
    }

    public List<StudentHoldHistoryDto> getHoldHistoryByStudent(Long studentId) {
        return studentHoldHistoryRepo.findHistoryByStudentId(studentId);
    }

    private void enableAllServices(StudentHoldViewDto dto) {
        dto.setCanRegisterCourses(true);
        dto.setCanViewCompletedCourses(true);
        dto.setCanViewStudentAudit(true);
        dto.setCanGenerateTranscript(true);
        dto.setCanApplyForGraduation(true);
    }
}