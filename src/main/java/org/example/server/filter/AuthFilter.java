package org.example.server.filter;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.security.AuthContext;
import org.example.server.filter_chain.FilterChain;
import org.example.service.AuthenticationService;

// TODO: переделать, сделать чище, сделать реализацию чейна, чтоб он сам управлял вызовами

@RequiredArgsConstructor
public class AuthFilter extends BaseRequestFilter {

    private final AuthenticationService authenticationService;

    private final String[] EXCLUDED_URIs = {"user.login", "user.register"};
    private final String JWT_TOKEN_HEADER = "jwt";

    @Override
    public void doFilter(Request request, FilterChain filterChain) {

        if(isExcludedURI(request.getCommand())) return;

        String jwtToken = request.getValueFromHeader(JWT_TOKEN_HEADER);

        if (!authenticationService.validateJwtToken(jwtToken)) throw new NotAuthenticatedException();

        AuthContext authContextFromToken = authenticationService.getAuthContextFromToken(jwtToken);

        AuthContext authContextFromRequest = request.getAuthContext();

        if(authContextFromRequest != null) {
            if(!authContextFromRequest.equals(authContextFromRequest)) throw new IllegalStateException();
            else return;
        };

        request.setAuthContext(authContextFromToken);
    }

    private boolean isExcludedURI(String command) {
        for(String cmd : EXCLUDED_URIs) {
            if (cmd.equals(command)) return true;
        }
        return false;
    }
}
