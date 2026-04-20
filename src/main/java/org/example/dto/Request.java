package org.example.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.security.AuthContext;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Request {
    private String command;
    private AuthContext authContext;  // токен, userId, роли
    private Map<String, String> headers;
    private RequestPayload payload;

    @JsonCreator
    public Request(@JsonProperty("command") String command) {
        this.command = command;
        this.headers = new HashMap<>();
    }

    public Request(String command, AuthContext authContext, RequestPayload payload, Map<String, String> headers) {
        this.command = command;
        this.authContext = authContext;
        this.payload = payload;
        this.headers = headers == null ? new HashMap<>() : headers;
    }

    public String getValueFromHeader(String header) {
        if(header != null) return headers.get(header);
        return "";
    }

    @Override
    public String toString() {
        return "Request{" +
                "command='" + command + '\'' +
                ", authContext=" + authContext +
                ", headers=" + headers +
                ", payload=" + payload +
                '}';
    }
}
