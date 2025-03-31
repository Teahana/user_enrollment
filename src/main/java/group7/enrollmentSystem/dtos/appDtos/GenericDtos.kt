package group7.enrollmentSystem.dtos.appDtos

class GenericDtos {
}

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val userId: Long, val userType: String, val token: String)
data class UserDto(val id: Long, val name: String, val email: String, val type: String, val studentId: String? = null)
data class TokenLogin(val token: String)
