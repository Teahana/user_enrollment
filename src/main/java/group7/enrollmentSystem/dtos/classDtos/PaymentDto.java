package group7.enrollmentSystem.dtos.classDtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private String courseCode;
    private String title;
    private String description;
    private Double cost;
    private Boolean paid;
}
