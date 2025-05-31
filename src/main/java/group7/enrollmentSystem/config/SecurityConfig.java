package group7.enrollmentSystem.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAtuhenticationProvider customAtuhenticationProvider;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RoleBasedSuccessHandler roleBasedSuccessHandler;
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(customAtuhenticationProvider)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")
                        .requestMatchers("/login","/api/**", "/register", "/styles/**", "/images/**").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(login -> login
                        .loginPage("/login")
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof CustomExceptions.StudentOnHoldException) {
                                response.sendRedirect("/login?hold=" +
                                        ((CustomExceptions.StudentOnHoldException)exception).getHoldType().name());
                            } else if (exception instanceof DisabledException) {
                                response.sendRedirect("/login?disabled");
                            } else {
                                response.sendRedirect("/login?error");
                            }
                        })

//                        .successHandler((request, response, authentication) -> {
//                            String redirectUrl = "/home";
//                            boolean isAdmin = authentication.getAuthorities().stream()
//                                    .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
//                            boolean isStudent = authentication.getAuthorities().stream()
//                                    .anyMatch(s -> s.getAuthority().equals("ROLE_STUDENT"));
//                            if (isAdmin) redirectUrl = "/admin/dashboard";
//                            else if (isStudent) redirectUrl = "/home";
//                            response.sendRedirect(redirectUrl);
//                        })
                        .successHandler(roleBasedSuccessHandler)
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
