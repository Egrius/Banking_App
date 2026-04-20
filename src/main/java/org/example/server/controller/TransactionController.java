package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.security.AuthContext;
import org.example.service.TransactionService;
import org.example.util.SecurityUtil;

@RequiredArgsConstructor
public class TransactionController implements BaseController {

    private final TransactionService transactionService;

    @Override
    public Response handle(Request request) {
        String[] parts = request.getCommand().split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid command: " + request.getCommand());
        }

        String action = parts[1];

        return switch (action) {
            case "transfer" -> handleTransfer(request);

            default -> Response.error("Unknown transaction action: " + action, 404);
        };
    }

    private Response handleTransfer(Request request) {
        AuthContext authContext = request.getAuthContext();

        TransactionCreateDto dto = (TransactionCreateDto) request.getPayload();

        SecurityUtil.checkOwner(authContext, dto.fromAccountId());

        TransactionReadDto result = transactionService.transfer(dto);
        return Response.success(result);
    }
}