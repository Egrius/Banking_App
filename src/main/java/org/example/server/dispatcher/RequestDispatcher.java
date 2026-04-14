package org.example.server.dispatcher;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.exception.CustomValidationException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.server.handler.*;
import org.example.service.AccountService;
import org.example.service.CardService;
import org.example.service.RoleService;
import org.example.service.UserService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RequestDispatcher {
    private static final Map<String, BaseRequestHandler> handlers = new HashMap<>();

    public RequestDispatcher(UserService userService, AccountService accountService,
                             CardService cardService, RoleService roleService) {

        handlers.put("user", new UserCommandHandler(userService));
        handlers.put("account", new AccountCommandHandler(accountService));
        handlers.put("card", new CardCommandHandler(cardService));
        handlers.put("role", new RoleCommandHandler(roleService));
        handlers.put("transaction", new RoleCommandHandler(roleService));
        // handlers.put("audit",);
    }

    public Response dispatch(Request request) {
        try{
            String[] parts = request.getCommand().split("\\.");

            if (parts.length < 1) {
                return Response.error("Empty command", 400);
            }

            String handlerName = parts[0];

            BaseRequestHandler handler = handlers.get(handlerName);

            if (handler == null) {
                return Response.error("Не найден handler для запроса " + request.getCommand(), 404);
            }

            return handler.handle(request);
        } catch (IllegalArgumentException e) {
            // Ошибки валидации входных данных (неверный ID, неверный формат)
            return Response.error(e.getMessage(), 400);

        } catch (AccessDeniedException e) {
            // Ошибки доступа (не авторизован, недостаточно прав)
            return Response.error(e.getMessage(), 403);

        } catch (EntityNotFoundException e) {
            // Сущность не найдена
            return Response.error(e.getMessage(), 404);

        } catch (CustomValidationException e) {
            // Ошибки валидации DTO
            return Response.error(e.getMessage(), 422);

        } catch (Exception e) {
            // Неожиданные ошибки
            log.error("Unexpected error", e);
            return Response.error("Internal server error", 500);
        }
    }
}