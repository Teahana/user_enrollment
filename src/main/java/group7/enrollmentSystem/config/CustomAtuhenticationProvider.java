package group7.enrollmentSystem.config;

import group7.enrollmentSystem.models.User;
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
            throw new DisabledException("Unpaid fees");
        }

        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
