package org.example.server.handler;

import org.example.dto.Request;
import org.example.dto.Response;

public interface BaseRequestHandler {
    Response handle(Request command);
}
