package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.ProgrammeDto;
import group7.enrollmentSystem.models.Programme;
import group7.enrollmentSystem.repos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgrammeService {

    private final ProgrammeRepo programmeRepo;
    private final CourseProgrammeRepo courseProgrammeRepo;
    private final CoursePrerequisiteRepo coursePrerequisiteRepo;
    private final StudentProgrammeRepo studentProgrammeRepo;
    private final CourseEnrollmentRepo courseEnrollmentRepo;


    // Add a new programme
    public void addProgramme(ProgrammeDto programmeDto) {
        Programme programme = new Programme();
        programme.setProgrammeCode(programmeDto.getProgrammeCode());
        programme.setName(programmeDto.getName());
        programme.setFaculty(programmeDto.getFaculty());
        programmeRepo.save(programme);
    }
    
    
    //------------------------For testing on PostMan--------------------------------------------//
    public void saveProgramme(String name, String programmeCode, String faculty) {
        Programme programme = new Programme();
        programme.setName(name);
        programme.setProgrammeCode(programmeCode);
        programme.setFaculty(faculty);
        programmeRepo.save(programme);
    }
    //--------------------------------------------------------------

    // Get all programmes records
    public List<Programme> getAllProgrammes() {
        return programmeRepo.findAll();
    }

    // Get 1 record
    public Optional<Programme> getProgrammeByCode(String programmeCode) {
        return programmeRepo.findByProgrammeCode(programmeCode);
    }

    // Update a programme record
    public void updateProgramme(String programmeCode, String name, String faculty) {
        Optional<Programme> optionalProgramme = programmeRepo.findByProgrammeCode(programmeCode);
        if (optionalProgramme.isPresent()) {
            Programme programme = optionalProgramme.get();
            programme.setName(name);
            programme.setProgrammeCode(programmeCode);
            programme.setFaculty(faculty);
            programmeRepo.save(programme);
        } else {
            throw new RuntimeException("Programme not found with programme code: " + programmeCode);
        }

    }
    @Transactional
    public void deleteProgramme(String programmeCode) {
        Optional<Programme> optionalProgramme = programmeRepo.findByProgrammeCode(programmeCode);
        if (optionalProgramme.isPresent()) {
            Programme programme = optionalProgramme.get();
            //Delete first from tables that references programme before deleting programmes since foreign keys and constraints will stop you
            //cause we use @Mappings in Jpa
            courseProgrammeRepo.deleteAllByProgramme(programme);
            coursePrerequisiteRepo.deleteAllByProgramme(programme);
            studentProgrammeRepo.deleteAllByProgramme(programme);
            courseEnrollmentRepo.deleteAllByProgramme(programme);
            programmeRepo.delete(programme);
        } else {
            throw new RuntimeException("Programme not found with programme code: " + programmeCode);
        }
    }
}
