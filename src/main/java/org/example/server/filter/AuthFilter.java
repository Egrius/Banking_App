package org.example.server.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.Request;
import org.example.exception.security_exception.NotAuthenticatedException;
import org.example.security.AuthContext;
import org.example.service.AuthenticationService;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class AuthFilter extends BaseRequestFilter {

    private final AuthenticationService authenticationService;

    private static final Set<String> EXCLUDED_COMMANDS = Set.of("user.login", "user.register");
    private static final String JWT_TOKEN_HEADER = "Authorization";

    @Override
    public void doFilterInternal(Request request) {

        log.debug("Request got: {}", request.getCommand());

        if(isExcludedURI(request.getCommand())) return;

        // Убирает Bearer_
        String jwtToken = request.getValueFromHeader(JWT_TOKEN_HEADER).substring(7);

        log.debug("Jwt token got from request '{}' : {}", request.getCommand(), jwtToken);

        if (!authenticationService.validateJwtToken(jwtToken)) throw new NotAuthenticatedException("Неаутенцифицированный запрос. Невалидный jwt токен");

        AuthContext authContextFromToken = authenticationService.getAuthContextFromToken(jwtToken);

        AuthContext authContextFromRequest = request.getAuthContext();

        if(authContextFromRequest != null) {
            if(!authContextFromRequest.equals(authContextFromToken)) throw new IllegalStateException("Переданный контекст безопасности не совпадает с контекстом из токена");
            return;
        };

        log.debug("Created auth context for request '{}', authContext user email - '{}' ", request.getCommand(), authContextFromToken.getEmail());

        request.setAuthContext(authContextFromToken);
    }

    private boolean isExcludedURI(String command) {
        for(String cmd : EXCLUDED_COMMANDS) {
            if (cmd.equals(command)) return true;
        }
        return false;
    }
}