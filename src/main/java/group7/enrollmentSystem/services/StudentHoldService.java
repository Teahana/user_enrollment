package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.StudentHoldDto;
import group7.enrollmentSystem.dtos.classDtos.StudentHoldHistoryDto;
import group7.enrollmentSystem.enums.OnHoldTypes;
import group7.enrollmentSystem.models.OnHoldStatus;
import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentHoldHistory;
import group7.enrollmentSystem.repos.StudentHoldHistoryRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentHoldService {
    private final StudentRepo studentRepo;
    private final StudentHoldHistoryRepo studentHoldHistoryRepo;

    public List<StudentHoldDto> getAllStudentsWithHoldStatus() {
        return studentRepo.findAllStudentsWithHoldStatus();
    }

    @Transactional
    public void placeStudentOnHold(Long studentId, OnHoldTypes holdType, String actionBy) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Remove any existing active holds
        student.getOnHoldStatusList().forEach(h -> h.setOnHold(false));

        // Add new hold
        OnHoldStatus holdStatus = new OnHoldStatus();
        holdStatus.setOnHoldType(holdType);
        holdStatus.setOnHold(true);
        student.getOnHoldStatusList().add(holdStatus);

        studentRepo.save(student);

        // Record in history
        StudentHoldHistory history = StudentHoldHistory.create(studentId, holdType, true, actionBy);
        studentHoldHistoryRepo.save(history);
    }

    @Transactional
    public void removeHoldFromStudent(Long studentId, String actionBy) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Get the current hold type before removing
        OnHoldTypes currentHoldType = student.getOnHoldStatusList().stream()
                .filter(OnHoldStatus::isOnHold)
                .findFirst()
                .map(OnHoldStatus::getOnHoldType)
                .orElse(null);

        student.getOnHoldStatusList().forEach(h -> h.setOnHold(false));
        studentRepo.save(student);

        // Record in history if there was a hold to remove
        if (currentHoldType != null) {
            StudentHoldHistory history = StudentHoldHistory.create(studentId, currentHoldType, false, actionBy);
            studentHoldHistoryRepo.save(history);
        }
    }

    public List<StudentHoldHistoryDto> getAllHoldHistory() {
        return studentHoldHistoryRepo.findAllHoldsHistory();
    }

    public List<StudentHoldHistoryDto> getHoldHistoryByStudent(Long studentId) {
        return studentHoldHistoryRepo.findHistoryByStudentId(studentId);
    }

    public List<StudentHoldDto> getStudentsForFilter() {
        return studentRepo.findAllStudentsWithHoldStatus();
    }

}