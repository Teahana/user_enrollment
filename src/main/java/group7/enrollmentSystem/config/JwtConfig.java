package group7.enrollmentSystem.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Key;
import java.util.Base64;

@Configuration
public class JwtConfig {

    @Value("${SECURITY_JWT_SECRET}")
    private String secretKey;

    @Bean
    public Key jwtKey() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("JWT secret key is not set or resolved!");
        }
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        Key key = Keys.hmacShaKeyFor(decodedKey);

        // 🔍  print the exact bytes Spring will sign with
        System.out.println("SPRING JWT KEY  (base-64url): "
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(key.getEncoded()));
        System.out.println("SPRING property value: " + secretKey);   // raw text from properties


        return key;
    }
}