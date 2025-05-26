package group7.enrollmentSystem.config;

import group7.enrollmentSystem.enums.OnHoldTypes;

import group7.enrollmentSystem.models.OnHoldStatus;

import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomAtuhenticationProvider implements AuthenticationProvider {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepo studentRepo;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();
        Optional<User> data = userRepo.findByEmail(email);

        if(data.isEmpty()){
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = data.get();
        if(!passwordEncoder.matches(rawPassword, user.getPassword())){
            throw new BadCredentialsException("Invalid credentials");
        }

        if(!user.isEnabled()){

            if (user instanceof Student) {
                Student student = (Student) user;
                Optional<OnHoldStatus> activeHold = student.getActiveHold();
                if (activeHold.isPresent()) {
                    System.out.println("throwing here");
                    throw new CustomExceptions.StudentOnHoldException(activeHold.get().getOnHoldType());
                }
            }
            throw new DisabledException("Your account is disabled.");
        }

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
