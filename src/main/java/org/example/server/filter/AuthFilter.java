package org.example.server.filter;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.exception.security_exception.NotAuthenticatedException;
import org.example.security.AuthContext;
import org.example.server.filter_chain.FilterChain;
import org.example.service.AuthenticationService;

import java.util.Set;

// TODO: переделать, сделать чище, сделать реализацию чейна, чтоб он сам управлял вызовами

@RequiredArgsConstructor
public class AuthFilter extends BaseRequestFilter {

    private final AuthenticationService authenticationService;

    private static final Set<String> EXCLUDED_COMMANDS = Set.of("user.login", "user.register");
    private static final String JWT_TOKEN_HEADER = "Authorization";

    @Override
    public void doFilterInternal(Request request) {

        if(isExcludedURI(request.getCommand())) return;

        String jwtToken = request.getValueFromHeader(JWT_TOKEN_HEADER);

        if (!authenticationService.validateJwtToken(jwtToken)) throw new NotAuthenticatedException("Неаутенцифицированный запрос. Невалидный jwt токен");

        AuthContext authContextFromToken = authenticationService.getAuthContextFromToken(jwtToken);

        AuthContext authContextFromRequest = request.getAuthContext();

        if(authContextFromRequest != null) {
            if(!authContextFromRequest.equals(authContextFromToken)) throw new IllegalStateException("Переданный контекст безопасности не совпадает с контекстом из токена");
            return;
        };

        request.setAuthContext(authContextFromToken);
    }

    private boolean isExcludedURI(String command) {
        for(String cmd : EXCLUDED_COMMANDS) {
            if (cmd.equals(command)) return true;
        }
        return false;
    }
}
