package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.PrerequisiteType;
import lombok.Data;

@Data
public class FlatCoursePrerequisiteDTO {
    private Long courseId;
    private Long prerequisiteId;
    private int groupId;
    private PrerequisiteType prerequisiteType;
    private PrerequisiteType operatorToNext;
    private boolean parent;
    private boolean child;
    private int childId;
}
