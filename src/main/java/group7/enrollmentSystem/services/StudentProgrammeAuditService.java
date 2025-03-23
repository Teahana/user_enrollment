package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.CourseAuditDto;
import group7.enrollmentSystem.dtos.classDtos.StudentFullAuditDto;
import group7.enrollmentSystem.models.*;
import group7.enrollmentSystem.repos.CourseEnrollmentRepo;
import group7.enrollmentSystem.repos.CourseProgrammeRepo;
import group7.enrollmentSystem.repos.StudentProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentProgrammeAuditService {

    private final StudentRepo studentRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;

    public StudentFullAuditDto getFullAudit(String studentId) {
        Student student = studentRepo.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        StudentProgramme studentProgramme = studentProgrammeRepo.findByStudent_StudentId(studentId)
                .stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No programme"));

        Programme programme = studentProgramme.getProgramme();

        List<CourseProgramme> courseProgrammeList = courseProgrammeRepo.findByProgrammeId(programme.getId());
        List<CourseEnrollment> enrollments = courseEnrollmentRepo.findByStudent(student);

        Set<Long> enrolledCourseIds = enrollments.stream()
                .filter(CourseEnrollment::isCurrentlyTaking)
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        Set<Long> completedCourseIds = enrollments.stream()
                .filter(CourseEnrollment::isCompleted)
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        List<CourseAuditDto> courseAudit = courseProgrammeList.stream()
                .map(cp -> {
                    Course course = cp.getCourse();
                    boolean isEnrolled = enrolledCourseIds.contains(course.getId());
                    boolean isCompleted = completedCourseIds.contains(course.getId());
                    return new CourseAuditDto(
                            course.getTitle(),
                            course.getCourseCode(),
                            isEnrolled,
                            course.getLevel(),
                            isCompleted
                    );
                })
                .collect(Collectors.toList());

        return new StudentFullAuditDto(
                student.getStudentId(),
                student.getFirstName() + " " + student.getLastName(),
                programme.getName(),
                studentProgramme.getStatus().name(),
                courseAudit
        );
    }
}
