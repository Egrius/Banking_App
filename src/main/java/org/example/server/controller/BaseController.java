package org.example.server.controller;

import org.example.dto.Request;
import org.example.dto.Response;

public interface BaseController {
    Response handle(Request command);
}
