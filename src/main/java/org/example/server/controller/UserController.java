package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.error.ViolationDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.ValidationErrorResponse;
import org.example.dto.user.PasswordChangeDto;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserLoginDto;
import org.example.dto.user.UserUpdateDto;
import org.example.exception.CustomValidationException;
import org.example.security.AuthContext;
import org.example.service.UserService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class UserController implements BaseController {

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

        log.debug("handleRegister called with command: {}", request.getCommand());
        log.debug("Payload: {}", request.getPayload());

        if (request.getPayload() == null) {
            return Response.error("Payload is null", 400);
        }

        if (!(request.getPayload() instanceof UserCreateDto)) {
            log.error("Payload type: {}", request.getPayload().getClass());
            return Response.error("Invalid payload type", 400);
        }

        UserCreateDto dto = (UserCreateDto) request.getPayload();

        ValidatorUtil.validate(dto);

        return Response.success(userService.register(dto));
    }

    private Response handleLogin(Request request) {
        UserLoginDto dto = (UserLoginDto) request.getPayload();

        ValidatorUtil.validate(dto);

        return Response.success(userService.login(dto));
    }

    private Response handleFindById(Request request, String[] parts) {
        // user.findById.1

        AuthContext authContext = request.getAuthContext();

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        return Response.success(userService.findById(id));
    }

    private Response handleFindAll(Request request) {
        // user.findAll — только payload с PageRequest или без

        AuthContext authContext = request.getAuthContext();

        SecurityUtil.checkAdmin(authContext);

        PageRequest pageRequest = request.getPayload() != null
                ? JsonUtil.convert(request.getPayload(), PageRequest.class)
                : PageRequest.of(0, 20);

        return Response.success(userService.findAll(request.getAuthContext(), pageRequest));
    }

    private Response handleUpdateUser(Request request, String[] parts) {
        // user.updateUser.123
        AuthContext authContext = request.getAuthContext();

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        UserUpdateDto dto = JsonUtil.convert(request.getPayload(), UserUpdateDto.class);

        ValidatorUtil.validate(dto);

        return Response.success(userService.updateUser(id, dto));
    }

    private Response handleChangePassword(Request request, String[] parts) {

        AuthContext authContext = request.getAuthContext();

        PasswordChangeDto dto = JsonUtil.convert(request.getPayload(), PasswordChangeDto.class);

        ValidatorUtil.validate(dto);

        if(!dto.newPassword().equals(dto.confirmPassword())) throw new CustomValidationException(new ValidationErrorResponse(List.of(new ViolationDto("confirmPassword", "Подтвержденный пароль не совпадает с подтверждаемым", dto.confirmPassword()))));

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        userService.changePassword(id, dto);

        return Response.success("Password changed successfully");
    }

    private Response handleDeleteUser(Request request, String[] parts) {

        AuthContext authContext = request.getAuthContext();

        Long id = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, id);

        String password = request.getHeaders().get("password");

        if(password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

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