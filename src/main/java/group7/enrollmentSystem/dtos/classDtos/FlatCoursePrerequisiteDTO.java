package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.PrerequisiteType;
import lombok.Data;

import java.util.List;

@Data
public class FlatCoursePrerequisiteDTO {
    private Long courseId;
    private Long prerequisiteId;   // normal courses only
    private Long programmeId;      // normal items only
    private int groupId;
    private PrerequisiteType prerequisiteType;
    private PrerequisiteType operatorToNext;
    private boolean parent;
    private boolean child;
    private int childId;
    private int parentId;
    private boolean special;
    private String specialType;    // e.g. "ADMISSION_PROGRAMME" or "COMPLETION_LEVEL_PERCENT"
    private Integer targetLevel;
    private Double percentageValue;
}

