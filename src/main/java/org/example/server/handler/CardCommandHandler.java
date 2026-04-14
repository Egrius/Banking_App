package org.example.server.handler;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardUpdateDto;
import org.example.security.AuthContext;
import org.example.service.CardService;
import org.example.util.JsonUtil;

@RequiredArgsConstructor
public class CardCommandHandler implements BaseRequestHandler {

    private final CardService cardService;

    @Override
    public Response handle(Request request) {
        String[] parts = request.getCommand().split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid command: " + request.getCommand());
        }

        String action = parts[1];

        return switch (action) {
            case "create" -> handleCreateCard(request);
            case "get" -> handleGetCard(request, parts);
            case "getUserCards" -> handleGetUserCards(request, parts);
            case "getAccountCards" -> handleGetAccountCards(request, parts);
            case "update" -> handleUpdateCard(request, parts);
            case "block" -> handleBlockCard(request, parts);
            case "unblock" -> handleUnblockCard(request, parts);
            case "delete" -> handleDeleteCard(request, parts);
            case "isActive" -> handleIsActive(request, parts);
            default -> Response.error("Unknown card action: " + action, 404);
        };
    }

    private Response handleCreateCard(Request request) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        CardCreateDto dto = JsonUtil.convert(request.getPayload(), CardCreateDto.class);

        return Response.success(cardService.createCard(dto, auth));
    }

    private Response handleGetCard(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);

        return Response.success(cardService.getCard(cardId, auth));
    }

    private Response handleGetUserCards(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long userId = extractId(parts);

        return Response.success(cardService.getUserCards(userId, auth));
    }

    private Response handleGetAccountCards(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long accountId = extractId(parts);

        return Response.success(cardService.getAccountCards(accountId, auth));
    }

    private Response handleUpdateCard(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);
        CardUpdateDto dto = JsonUtil.convert(request.getPayload(), CardUpdateDto.class);

        return Response.success(cardService.updateCard(cardId, dto, auth));
    }

    private Response handleBlockCard(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);
        String reason = request.getHeaders() != null ? request.getHeaders().get("reason") : "";

        cardService.blockCard(cardId, reason, auth);
        return Response.success("Card blocked successfully");
    }

    private Response handleUnblockCard(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);

        cardService.unblockCard(cardId, auth);
        return Response.success("Card unblocked successfully");
    }

    private Response handleDeleteCard(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);

        cardService.deleteCard(cardId, auth);
        return Response.success("Card deleted successfully");
    }

    private Response handleIsActive(Request request, String[] parts) {
        AuthContext auth = JsonUtil.convert(request.getAuthContext(), AuthContext.class);
        Long cardId = extractId(parts);
        return Response.success(cardService.isCardActive(cardId));
    }

    private Long extractId(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("Missing ID parameter");
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid ID format: " + parts[2]);
        }
    }
}