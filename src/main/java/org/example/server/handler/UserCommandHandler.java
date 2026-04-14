package org.example.server.handler;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.request.PageRequest;
import org.example.dto.user.PasswordChangeDto;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserLoginDto;
import org.example.dto.user.UserUpdateDto;
import org.example.security.AuthContext;
import org.example.service.UserService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.util.Map;

@RequiredArgsConstructor
public class UserCommandHandler implements BaseRequestHandler {

    private final UserService userService;

    @Override
    public Response handle(Request request) {

        String[] parts = request.getCommand().split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Передана некорректная команда в UserCommandHandler: " + request.getCommand());
        }

        String command = parts[1];

        if(command == null) {
            throw new IllegalArgumentException("Передана некорректная команда в UserCommandHandler: " + request.getCommand());
        }

        return switch (command) {
            case "register" -> handleRegister(request);
            case "login" -> handleLogin(request);
            case "findById" -> handleFindById(request, parts);
            case "findAll" -> handleFindAll(request);
            case "updateUser" -> handleUpdateUser(request, parts);
            case "changePassword" -> handleChangePassword(request, parts);
            case "deleteUser" -> handleDeleteUser(request, parts);
            default -> Response.error("Unknown command: " + command, 404);
        };
    }

    private Response handleRegister(Request request) {

        UserCreateDto dto = JsonUtil.convert(request.getPayload(), UserCreateDto.class);

        ValidatorUtil.validate(dto);

        return Response.success(userService.register(dto));
    }

    private Response handleLogin(Request request) {
        UserLoginDto dto = JsonUtil.convert(request.getPayload(), UserLoginDto.class);

        ValidatorUtil.validate(dto);

        return Response.success(userService.login(dto));
    }

    private Response handleFindById(Request request, String[] parts) {
        // user.findById.1

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);


        Long id = getIdFromParts(parts);

        return Response.success(userService.findById(id));
    }

    private Response handleFindAll(Request request) {
        // user.findAll — только payload с PageRequest или без

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        SecurityUtil.checkAdmin(authContext);

        PageRequest pageRequest = request.getPayload() != null
                ? JsonUtil.convert(request.getPayload(), PageRequest.class)
                : PageRequest.of(0, 20);

        return Response.success(userService.findAll(request.getAuthContext(), pageRequest));
    }

    private Response handleUpdateUser(Request request, String[] parts) {
        // user.updateUser.123
        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        UserUpdateDto dto = JsonUtil.convert(request.getPayload(), UserUpdateDto.class);

        ValidatorUtil.validate(dto);

        return Response.success(userService.updateUser(id, dto));
    }

    private Response handleChangePassword(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        PasswordChangeDto dto = JsonUtil.convert(request.getPayload(), PasswordChangeDto.class);

        ValidatorUtil.validate(dto);
        
        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        userService.changePassword(id, dto);

        return Response.success("Password changed successfully");
    }

    private Response handleDeleteUser(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        String password = extractPassword(request.getHeaders());

        userService.deleteUser(id, password);

        return Response.success("User deleted successfully");
    }

    private String extractPassword(Map<String, String> headers) {
        String password = headers.get("password");
        if (password == null) {
            throw new IllegalArgumentException("Password is required");
        }
        return password;
    }

    private Long getIdFromParts(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("User id is required");
        }
        
        return Long.parseLong(parts[2]);
    }
}