package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class FlatCoursePrerequisiteRequest {
    private Long courseId;
    private List<FlatCoursePrerequisiteDTO> prerequisites;
}

