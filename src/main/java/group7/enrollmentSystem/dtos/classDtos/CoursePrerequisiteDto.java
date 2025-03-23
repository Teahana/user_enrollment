package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.PrerequisiteType;
import lombok.Data;

@Data
public class CoursePrerequisiteDto {
    private Long courseId;
    private Long prerequisiteId;
    private String prerequisiteCode;
    private PrerequisiteType prerequisiteType;
    private int groupId;

    public CoursePrerequisiteDto(Long courseId, Long prerequisiteId, String prerequisiteCode, PrerequisiteType prerequisiteType, int groupId) {
        this.courseId = courseId;
        this.prerequisiteId = prerequisiteId;
        this.prerequisiteCode = prerequisiteCode;
        this.prerequisiteType = prerequisiteType;
        this.groupId = groupId;
    }
}

