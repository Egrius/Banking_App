package org.example.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.security.AuthContext;

import java.util.Map;

@Getter
@Setter
public class Request {
    private String command;
    private AuthContext authContext;  // токен, userId, роли
    private Map<String, String> headers;
    private Object payload;

    public Request() {}

    public Request(String command) {
        this.command = command;
    }

    public Request(String command, AuthContext authContext, Object payload, Map<String, String> headers) {
        this.command = command;
        this.authContext = authContext;
        this.payload = payload;
        this.headers = headers;
    }
}
