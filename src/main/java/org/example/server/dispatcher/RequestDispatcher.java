package org.example.server.dispatcher;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.exception.CustomValidationException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.server.filter_chain.FilterChain;
import org.example.server.controller.*;
import org.example.service.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RequestDispatcher {
    private static final Map<String, BaseController> controllers = new HashMap<>();
    private final FilterChain filterChain;

    public RequestDispatcher(UserService userService, AccountService accountService,
                             CardService cardService, RoleService roleService,
                             TransactionService transactionService, FilterChain filterChain) {

        this.filterChain = filterChain;

        controllers.put("user", new UserController(userService));
        controllers.put("account", new AccountController(accountService));
        controllers.put("card", new CardController(cardService));
        controllers.put("role", new RoleController(roleService));
        controllers.put("transaction", new TransactionController(transactionService));
        // handlers.put("audit",);
    }

    public Response dispatch(Request request) {
        try{

            // Проход фильтров
            filterChain.execute(request);

            // Выполнение запроса
            String[] parts = request.getCommand().split("\\.");

            if (parts.length < 1) {
                return Response.error("Empty command", 400);
            }

            String handlerName = parts[0];

            BaseController handler = controllers.get(handlerName);

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