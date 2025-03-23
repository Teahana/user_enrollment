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
    @Enumerated(EnumType.STRING)
    private PrerequisiteType operator;
    @Enumerated(EnumType.STRING)
    private PrerequisiteType operatorToNext;
    private List<GraphicalPrerequisiteNode> children = new ArrayList<>();
}

