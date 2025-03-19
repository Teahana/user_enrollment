package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

import java.util.List;

@Data
public class AddCourseReq {
    private Long courseId;
    private List<Long> prerequisites;
}
