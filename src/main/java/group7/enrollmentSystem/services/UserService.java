package group7.enrollmentSystem.services;

import group7.enrollmentSystem.models.Student;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.StudentRepo;
import group7.enrollmentSystem.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final StudentRepo studentRepo;

    //Some changes for pull request demo
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

    @Transactional
    public void registerUser(String email, String password, String firstName, String lastName, String role, Student student) {
        // validate email not yet registered
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if ("ROLE_STUDENT".equalsIgnoreCase(role) && student != null) {
            if (student.getStudentId() == null || student.getStudentId().isEmpty()) {
                throw new IllegalArgumentException("Student ID is required");
            }
            student.setEmail(email);
            student.setPassword(passwordEncoder.encode(password));
            student.setRoles(Set.of("ROLE_STUDENT"));
            student.setFirstName(firstName);
            student.setLastName(lastName);

            studentRepo.save(student);
            System.out.println("Student user saved");
        } else if ("ROLE_ADMIN".equalsIgnoreCase(role)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRoles(Set.of("ROLE_ADMIN"));
            user.setFirstName(firstName);
            user.setLastName(lastName);

            userRepo.save(user);
            System.out.println("Admin user saved ");
        } else {
            throw new IllegalArgumentException("Unsupported role");
        }
    }

    public void save(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of("USER"));
        userRepo.save(user);

    }
}
