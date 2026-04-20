package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class Response {
    private boolean success;
    private int statusCode;
    private String message;
    private Object data;

    public Response() {}

    public static Response success(Object data) {
        Response r = new Response();
        r.setSuccess(true);
        r.setStatusCode(200);
        r.setData(data);
        return r;
    }

    public static Response success(String msg) {
        Response r = new Response();
        r.setSuccess(true);
        r.setStatusCode(200);
        r.setMessage(msg);
        return r;
    }

    public static Response error(String message, int statusCode) {
        Response r = new Response();
        r.setSuccess(false);
        r.setStatusCode(statusCode);
        r.setMessage(message);
        return r;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
