package group7.enrollmentSystem.config;

import group7.enrollmentSystem.enums.OnHoldTypes;
<<<<<<< Updated upstream
=======
import group7.enrollmentSystem.models.OnHoldStatus;
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
//            Student student = studentRepo.findById(user.getId())
//                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));;
//            OnHoldTypes onHoldType = student.getOnHoldType();
//            Student student = (Student) user;
//            System.out.println("student: " + student);

            throw new DisabledException("Unpaid fees");
=======
            if (user instanceof Student) {
                Student student = (Student) user;
                Optional<OnHoldStatus> activeHold = student.getActiveHold();
                if (activeHold.isPresent()) {
                    throw new CustomExceptions.StudentOnHoldException(activeHold.get().getOnHoldType());
                }
            }
            throw new DisabledException("Your account is disabled.");
>>>>>>> Stashed changes
        }

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
