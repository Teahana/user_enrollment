package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("Email: " + email);
        Optional<User> user = userRepo.findByEmail(email);
        if(user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        else{
            return user.get();
        }
    }
    public void save(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_USER");
        user.setFeesPaid(true);
        userRepo.save(user);
    }
    @Transactional
    public void updateFees(String email, boolean feesPaid) {
        userRepo.updateFeesPaid(email, feesPaid);
    }
}
