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
                        //.requestMatchers("/admin/**").hasRole("ADMIN")// Restrict /admin/** to ADMIN role
                        .anyRequest().permitAll()  // Allow all other endpoints
                )
                .formLogin(login -> login
                        .loginPage("/login")   // Custom login page
                        .defaultSuccessUrl("/home", true)  // Redirect to /home after successful login
                        .failureHandler((request, response, exception) -> {
                            if(exception.getMessage().equalsIgnoreCase("Unpaid fees")){
                                response.sendRedirect("/login?disabled");
                            }
                            else{
                               response.sendRedirect("/login?error");
                            }

                        })  // Redirect to /login?error after failed login
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


