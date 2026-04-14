package org.example.server.handler;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.transation.TransactionReadDto;
import org.example.security.AuthContext;
import org.example.service.TransactionService;
import org.example.util.JsonUtil;
import org.example.util.SecurityUtil;

@RequiredArgsConstructor
public class TransactionCommandHandler implements BaseRequestHandler {

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
        AuthContext authContext = JsonUtil.convert(request.getAuthContext(), AuthContext.class);

        TransactionCreateDto dto = JsonUtil.convert(request.getPayload(), TransactionCreateDto.class);

        SecurityUtil.checkOwner(authContext, dto.fromAccountId());

        TransactionReadDto result = transactionService.transfer(dto);
        return Response.success(result);
    }
}