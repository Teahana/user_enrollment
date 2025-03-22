package group7.enrollmentSystem.dtos.classDtos;

import group7.enrollmentSystem.enums.PrerequisiteType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphicalPrerequisiteNode {
    private Long courseId;
    private String courseCode;
    private short level;

    // The AND/OR operator that applies *to this node’s children*
    @Enumerated(EnumType.STRING)
    private PrerequisiteType operator;

    // If this node “connects to the next group” (like operatorToNext in your DB),
    // store it here if needed. Not all designs require it.
    @Enumerated(EnumType.STRING)
    private PrerequisiteType operatorToNext;

    // The child “subgroups” or prerequisites
    private List<GraphicalPrerequisiteNode> children = new ArrayList<>();
}

