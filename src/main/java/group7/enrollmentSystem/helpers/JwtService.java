package group7.enrollmentSystem.helpers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final Key jwtKey; // Injected from JwtConfig

    public String generateToken(UserDetails userDetails, long expirationInSeconds) {
        Map<String, Object> claims = Map.of("roles", userDetails.getAuthorities()
                .stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expirationInSeconds * 1000))
                .signWith(jwtKey)
                .compact();
    }



    /**
     * Parse and validate a JWT token.
     * Throws JwtException if the token is invalid or expired.
     *
     * @param token The JWT token to parse.
     * @return The claims from the JWT.
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

