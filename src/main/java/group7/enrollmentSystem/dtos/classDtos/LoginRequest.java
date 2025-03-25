package group7.enrollmentSystem.dtos.classDtos;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}

