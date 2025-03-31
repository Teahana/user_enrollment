package group7.enrollmentSystem.config;

import group7.enrollmentSystem.helpers.JwtService;
import group7.enrollmentSystem.models.User;
import group7.enrollmentSystem.repos.UserRepo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException, java.io.IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
           // System.out.println("No Bearer token found in Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.parseToken(jwt);
            Date issuedAt = claims.getIssuedAt();
            long now = System.currentTimeMillis();
            long issuedAtMillis = issuedAt.getTime();
            long threshHold = 15 * 60 * 1000;
            if (now - issuedAtMillis >= threshHold) {
                //Generate a new token with a new expiration
                String username = claims.getSubject();
                User user = userRepo.findByEmail(username).orElseThrow();
                List<String> roles = claims.get("roles", List.class);
                String newToken = jwtService.generateToken(user, 3600);
                response.setHeader("X-New-Token", newToken);
            }

            // System.out.println("JWT successfully parsed");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
          //  System.out.println("JWT expired: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expired\"}");
            return;
        } catch (Exception e) {
           // System.out.println("JWT invalid: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid token\"}");
            return;
        }

        String username = claims.getSubject();
       // System.out.println("Authenticated user from token: " + username);

        List<String> roles = claims.get("roles", List.class);
       // System.out.println("Roles from token: " + roles);

        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
           // System.out.println("SecurityContext authentication set");
        }

        filterChain.doFilter(request, response);
    }
}


