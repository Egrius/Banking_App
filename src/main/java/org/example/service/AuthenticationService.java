package org.example.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.security.AuthContext;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Slf4j
public class AuthenticationService {

    /*
    Летит запрос с токеном jwt (header: Bearer iqwewiqdjpaxjkp... ), далее проходим через филтьтр и насыщаем запрос контекстом
    При первом логине: летит на сервис UserService, при успехе вызвать authService.generateJwtToken -> отправить в респонс
     */

    private String jwtSecret = "dGhpcy1pcy1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci1qd3Qtc2lnbmluZw";

    private int jwtExpirationMs = 86400000;

    public AuthContext createAuthContextForUser(User loggedInUser) {
        return new AuthContext(loggedInUser.getId(), loggedInUser.getEmail(), loggedInUser.getRoles().stream()
                .map(Role::getName)
                .toList());
    }

    public String generateJwtToken(AuthContext authContext) {
        return Jwts.builder()
                .subject(authContext.getUserId().toString())
                .claim("email", authContext.getEmail())
                .claim("roles", authContext.getRoles())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(secretKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(jwtSecret));
    }

    public String getUserEmailFromJwtToken(String token) {
        return Jwts.parser().verifyWith(secretKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getUserRolesFromJwtToken(String token) {
        return Jwts.parser().verifyWith(secretKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles", List.class);
    }

    public Long getUserIdFromJwtToken(String token) {
        return Long.parseLong(Jwts.parser().verifyWith(secretKey()).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject());
    }

    public AuthContext getAuthContextFromToken(String token) {
        return new AuthContext(
                getUserIdFromJwtToken(token),
                getUserEmailFromJwtToken(token),
                getUserRolesFromJwtToken(token));
    }

    public boolean validateJwtToken(String jwtToken) {
        try{
            Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(jwtToken);
            return true;
        }  catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

}
