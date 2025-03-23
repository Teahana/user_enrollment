package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.PrerequisiteType;
import lombok.Data;

import java.util.List;

import lombok.Data;
import java.util.List;

@Data
public class PrerequisiteGroupDTO {
    private Long id; // Group ID (optional for new groups)
    private String type; // AND / OR
    private List<Long> courses; // Courses in this group
    private List<PrerequisiteGroupDTO> subGroups; // Nested subgroups
    private String operatorToNext; // AND / OR (if linking to another top-level group)
}
