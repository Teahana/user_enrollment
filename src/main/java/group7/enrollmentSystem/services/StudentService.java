//package group7.enrollmentSystem.services;
//
//import group7.enrollmentSystem.dtos.classDtos.CourseEnrollmentDto;
//import group7.enrollmentSystem.models.Programme;
//import group7.enrollmentSystem.models.Student;
//import group7.enrollmentSystem.repos.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class StudentService {
//    private final CourseRepo courseRepo;
//    private final StudentRepo studentRepo;
//    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
//    private final CourseEnrollmentRepo courseEnrollmentRepo;
//    private final StudentProgrammeRepo studentProgrammeRepo;
//    private final CourseProgrammeRepo courseProgrammeRepo;
//
//    public List<CourseEnrollmentDto> getEligibleCourses(String email){
//        //get the student
//        Student student = studentRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("Student not found with email: " + email));
//        //get the programme
//        Programme programme = studentProgrammeRepo.findStudentCurrentProgramme(student).orElseThrow(() -> new RuntimeException("Programme not found for student with email: " + email));
//        //get all courseIds of courses in that programme.
//        List<Long> courseIds = courseProgrammeRepo.getCourseIdsByProgramme(programme);
//        //get the completed courses idss
//        List<Long> completedCourseIds = courseEnrollmentRepo.getCompletedCourseIdsByStudent(student);
//        //get the courses they currently applied for but not yet taking.
//        List<Long> appliedCourseIds = courseEnrollmentRepo.getAppliedCourseIdsByStudent(student);
//
//    }
//}
//
