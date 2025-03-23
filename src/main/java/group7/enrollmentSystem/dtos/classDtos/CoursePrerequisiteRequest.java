package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class CoursePrerequisiteRequest {
    private Long courseId; // The course that prerequisites are being added to
    private List<PrerequisiteGroupDTO> prerequisiteGroups; // Top-level groups
}