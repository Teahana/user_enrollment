package group7.enrollmentSystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAtuhenticationProvider customAtuhenticationProvider;

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(customAtuhenticationProvider)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disable CSRF
            .cors(cors -> cors.disable())  // Disable CORS
            .authorizeHttpRequests(auth -> auth
                    //.requestMatchers("/api/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")  // Restrict /admin/** to ADMIN role
                    .requestMatchers("/student/**").hasRole("STUDENT")
                    .requestMatchers("/login","/styles/**","/images/**").permitAll()
                    .anyRequest().authenticated()
            )
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Custom role-based redirect logic here
                            String redirectUrl = "/home"; // default

                            boolean isAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
                            boolean isStudent = authentication.getAuthorities().stream()
                                    .anyMatch(s -> s.getAuthority().equals("ROLE_STUDENT"));


                            if (isAdmin) {
                                System.out.println("Admin logged in");
                                redirectUrl = "/admin/dashboard";
                            } else if (isStudent) {
                                System.out.println("Student logged in");
                                redirectUrl = "/student/dashboard";

                            }


                            response.sendRedirect(redirectUrl);
                        })
//                        .failureHandler((request, response, exception) -> {
//                            if ("Unpaid fees".equalsIgnoreCase(exception.getMessage())) {
//                                response.sendRedirect("/login?disabled");
//                            } else {
//                                response.sendRedirect("/login?error");
//                            }
//
//                        })  // Redirect to /login?error after failed login
                    .permitAll()
            )
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            );

        return http.build();
    }
}
