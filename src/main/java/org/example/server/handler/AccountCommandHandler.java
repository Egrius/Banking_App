package org.example.server.handler;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.request.PageRequest;
import org.example.security.AuthContext;
import org.example.service.AccountService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;

// TODO: пофиксить дырки: передавать контекст в сервис и смотреть там
@RequiredArgsConstructor
public class AccountCommandHandler implements BaseRequestHandler {

    private final AccountService accountService;

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
            case "createAccount" -> handleCreateAccount(request);
            case "getAccount" -> handleGetAccount(request, parts);
            case "getUserAccounts" -> handleGetUserAccounts(request, parts);
            case "closeAccount" -> handleCloseAccount(request, parts);
            case "blockAccount" -> handleBlockAccount(request, parts);
            case "getBalanceAudit" -> handleGetBalanceAudit(request, parts);
            case "getBalance" -> handleGetBalance(request, parts);

            default -> Response.error("Unknown command: " + command, 404);
        };
    }

    public Response handleCreateAccount(Request request) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        AccountCreateDto dto = JsonUtil.convert(request.getPayload(), AccountCreateDto.class);

        SecurityUtil.checkAdminOrOwner(authContext, dto.userId());

        return Response.success(accountService.createAccount(dto));
    }

    public Response handleGetAccount(Request request, String[] parts) {
        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long accountId = getIdFromParts(parts);

        // Проверка прав внутри сервиса
        AccountReadDto dto = accountService.getAccount(accountId, authContext);

        return Response.success(dto);
    }

    public Response handleGetUserAccounts(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long userId = getIdFromParts(parts);

        SecurityUtil.checkAdminOrOwner(authContext, userId);

        return Response.success(accountService.getUserAccounts(userId)); // Проверить возврат, т.к возвращает List
    }

    public Response handleCloseAccount(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long accountId = getIdFromParts(parts);

        accountService.closeAccount(accountId, authContext);

        return Response.success(null);
    }

    public Response handleBlockAccount(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long accountId = getIdFromParts(parts);

        String reason = request.getHeaders().get("reason");

        if(reason == null) reason = "";

        accountService.blockAccount(accountId, reason, authContext);

        return Response.success(null);
    }

    public Response handleGetBalanceAudit(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        PageRequest pageRequest = JsonUtil.convert(request.getPayload(), PageRequest.class);

        Long accountId = getIdFromParts(parts);

         return Response.success(accountService.getBalanceAudit(accountId, pageRequest, authContext));
    }

    public Response handleGetBalance(Request request, String[] parts) {

        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        Long accountId = getIdFromParts(parts);

        return Response.success(accountService.getBalance(accountId, authContext));
    }

    private Long getIdFromParts(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("User id is required");
        }

        return Long.parseLong(parts[2]);
    }
}