package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

@Data
public class ErrorResponseDTO {
    private String message;
    private int status;
    public ErrorResponseDTO(String message, int i) {
        this.message = message;
        this.status = i;
    }
}
