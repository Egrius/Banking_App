package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardUpdateDto;
import org.example.security.AuthContext;
import org.example.service.CardService;

@RequiredArgsConstructor
public class CardController implements BaseController {

    private final CardService cardService;

    @Override
    public Response handle(Request request) {
        String[] parts = request.getCommand().split("\\.");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid command: " + request.getCommand());
        }

        String action = parts[1];

        return switch (action) {
            case "createCard" -> handleCreateCard(request);
            case "getCard" -> handleGetCard(request, parts);
            case "getUserCards" -> handleGetUserCards(request, parts);
            case "getAccountCards" -> handleGetAccountCards(request, parts);
            case "updateCard" -> handleUpdateCard(request, parts);
            case "blockCard" -> handleBlockCard(request, parts);
            case "unblockCard" -> handleUnblockCard(request, parts);
            case "deleteCard" -> handleDeleteCard(request, parts);
            case "isActiveCard" -> handleIsActive(request, parts);
            default -> Response.error("Unknown card action: " + action, 404);
        };
    }

    private Response handleCreateCard(Request request) {
        AuthContext auth = request.getAuthContext();
        CardCreateDto dto = (CardCreateDto) request.getPayload();

        return Response.success(cardService.createCard(dto, auth));
    }

    private Response handleGetCard(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long cardId = extractId(parts);

        return Response.success(cardService.getCard(cardId, auth));
    }

    private Response handleGetUserCards(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long userId = extractId(parts);

        return Response.success(cardService.getUserCards(userId, auth));
    }

    private Response handleGetAccountCards(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long accountId = extractId(parts);

        return Response.success(cardService.getAccountCards(accountId, auth));
    }

    private Response handleUpdateCard(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long cardId = extractId(parts);
        CardUpdateDto dto = (CardUpdateDto) request.getPayload();

        return Response.success(cardService.updateCard(cardId, dto, auth));
    }

    private Response handleBlockCard(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long cardId = extractId(parts);
        String reason = request.getHeaders() != null ? request.getHeaders().get("reason") : "";

        cardService.blockCard(cardId, reason, auth);
        return Response.success("Card blocked successfully");
    }

    private Response handleUnblockCard(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long cardId = extractId(parts);

        cardService.unblockCard(cardId, auth);
        return Response.success("Card unblocked successfully");
    }

    private Response handleDeleteCard(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
        Long cardId = extractId(parts);

        cardService.deleteCard(cardId, auth);
        return Response.success("Card deleted successfully");
    }

    private Response handleIsActive(Request request, String[] parts) {
        AuthContext auth = request.getAuthContext();
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