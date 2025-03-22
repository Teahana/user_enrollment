package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.StudentProgramme;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.StudentProgrammeRepo;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.ProgrammeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentProgrammeService {

    private final StudentProgrammeRepo studentProgrammeRepo;
    private final StudentRepo studentRepo;
    private final ProgrammeRepo programmeRepo;

    public Optional<StudentProgramme> getCurrentProgramme(Student student) {
        return studentProgrammeRepo.findByStudentAndCurrentProgrammeTrue(student);
    }

    // Create a new StudentProgramme
    public void saveStudentProgramme(Long studentId, Long programmeId, boolean currentProgramme) {
        Optional<Student> student = studentRepo.findById(studentId);
        Optional<Programme> programme = programmeRepo.findById(programmeId);

        if (student.isPresent() && programme.isPresent()) {
            StudentProgramme studentProgramme = new StudentProgramme();
            studentProgramme.setStudent(student.get());
            studentProgramme.setProgramme(programme.get());
            studentProgramme.setCurrentProgramme(currentProgramme);
            studentProgrammeRepo.save(studentProgramme);
        } else {
            throw new RuntimeException("Student or Programme not found");
        }
    }

    // Get all StudentProgrammes
    public List<StudentProgramme> getAllStudentProgrammes() {
        return studentProgrammeRepo.findAll();
    }

    // Get a single StudentProgramme by ID
    public Optional<StudentProgramme> getStudentProgrammeById(Long id) {
        return studentProgrammeRepo.findById(id);
    }

    // Update a StudentProgramme
    public void updateStudentProgramme(Long id, Long studentId, Long programmeId, boolean currentProgramme) {
        Optional<StudentProgramme> optionalStudentProgramme = studentProgrammeRepo.findById(id);
        Optional<Student> student = studentRepo.findById(studentId);
        Optional<Programme> programme = programmeRepo.findById(programmeId);

        if (optionalStudentProgramme.isPresent() && student.isPresent() && programme.isPresent()) {
            StudentProgramme studentProgramme = optionalStudentProgramme.get();
            studentProgramme.setStudent(student.get());
            studentProgramme.setProgramme(programme.get());
            studentProgramme.setCurrentProgramme(currentProgramme);
            studentProgrammeRepo.save(studentProgramme);
        } else {
            throw new RuntimeException("StudentProgramme, Student, or Programme not found");
        }
    }

    // Delete a StudentProgramme
    public void deleteStudentProgramme(Long id) {
        studentProgrammeRepo.deleteById(id);
    }

}
