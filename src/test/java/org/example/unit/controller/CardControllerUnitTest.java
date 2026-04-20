package org.example.unit.controller;

import org.example.dto.Request;
import org.example.dto.Response;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardReadDto;
import org.example.dto.card.CardUpdateDto;
import org.example.entity.enums.CardStatus;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;
import org.example.security.AuthContext;
import org.example.server.controller.CardController;
import org.example.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardControllerUnitTest {

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController controller;

    private AuthContext adminAuth;
    private AuthContext userAuth;
    private final Long USER_ID = 1L;
    private final Long ACCOUNT_ID = 100L;
    private final Long CARD_ID = 200L;

    @BeforeEach
    void setUp() {
        adminAuth = new AuthContext(999L, "admin@test.com", List.of("ADMIN"));
        userAuth = new AuthContext(USER_ID, "user@test.com", List.of("USER"));
    }

    @Nested
    class CreateCardTests {

        private final String COMMAND = "card.createCard";

        @Test
        void createCardShouldCallServiceAndReturnSuccess() {
            CardCreateDto dto = new CardCreateDto(ACCOUNT_ID, CurrencyCode.US, "John Doe", CardType.DEBIT, "My Card");
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            CardReadDto expected = new CardReadDto(CARD_ID, USER_ID, "John Doe", ACCOUNT_ID, "ACNT-001",
                    "****-****-****-1234", CurrencyCode.US, "John Doe", "12/28", CardType.DEBIT, CardStatus.ACTIVE, "My Card", false);
            when(cardService.createCard(any(CardCreateDto.class), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).createCard(dto, userAuth);
        }
    }

    @Nested
    class GetCardTests {

        private final String COMMAND = "card.getCard." + CARD_ID;

        @Test
        void getCardShouldReturnCard() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            CardReadDto expected = mock(CardReadDto.class);
            when(cardService.getCard(eq(CARD_ID), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).getCard(CARD_ID, userAuth);
        }
    }

    @Nested
    class GetUserCardsTests {

        private final String COMMAND = "card.getUserCards." + USER_ID;

        @Test
        void getUserCardsShouldReturnList() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            List<CardReadDto> expected = List.of(mock(CardReadDto.class), mock(CardReadDto.class));
            when(cardService.getUserCards(eq(USER_ID), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).getUserCards(USER_ID, userAuth);
        }
    }

    @Nested
    class GetAccountCardsTests {

        private final String COMMAND = "card.getAccountCards." + ACCOUNT_ID;

        @Test
        void getAccountCardsShouldReturnList() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            List<CardReadDto> expected = List.of(mock(CardReadDto.class));
            when(cardService.getAccountCards(eq(ACCOUNT_ID), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).getAccountCards(ACCOUNT_ID, userAuth);
        }
    }

    @Nested
    class UpdateCardTests {

        private final String COMMAND = "card.updateCard." + CARD_ID;

        @Test
        void updateCardShouldCallService() {
            CardUpdateDto dto = new CardUpdateDto("New Card Name");
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);
            request.setPayload(dto);

            CardReadDto expected = mock(CardReadDto.class);
            when(cardService.updateCard(eq(CARD_ID), any(CardUpdateDto.class), eq(userAuth))).thenReturn(expected);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).updateCard(CARD_ID, dto, userAuth);
        }
    }

    @Nested
    class BlockCardTests {

        private final String COMMAND = "card.blockCard." + CARD_ID;

        @Test
        void blockCardShouldCallServiceWithReason() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setHeaders(Map.of("reason", "Lost card"));

            doNothing().when(cardService).blockCard(eq(CARD_ID), eq("Lost card"), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals("Card blocked successfully", response.getMessage());
            verify(cardService).blockCard(CARD_ID, "Lost card", adminAuth);
        }

        @Test
        void blockCardShouldCallServiceWithEmptyReason_whenReasonHeaderMissing() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);
            request.setHeaders(Map.of());

            doNothing().when(cardService).blockCard(CARD_ID, null, adminAuth);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            verify(cardService).blockCard(CARD_ID, null, adminAuth);
        }
    }

    @Nested
    class UnblockCardTests {

        private final String COMMAND = "card.unblockCard." + CARD_ID;

        @Test
        void unblockCardShouldCallService() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            doNothing().when(cardService).unblockCard(eq(CARD_ID), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals("Card unblocked successfully", response.getMessage());
            verify(cardService).unblockCard(CARD_ID, adminAuth);
        }
    }

    @Nested
    class DeleteCardTests {

        private final String COMMAND = "card.deleteCard." + CARD_ID;

        @Test
        void deleteCardShouldCallService() {
            Request request = new Request(COMMAND);
            request.setAuthContext(adminAuth);

            doNothing().when(cardService).deleteCard(eq(CARD_ID), eq(adminAuth));

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals("Card deleted successfully", response.getMessage());
            verify(cardService).deleteCard(CARD_ID, adminAuth);
        }
    }

    @Nested
    class IsActiveCardTests {

        private final String COMMAND = "card.isActiveCard." + CARD_ID;

        @Test
        void isActiveCardShouldReturnTrue() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            when(cardService.isCardActive(eq(CARD_ID))).thenReturn(true);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(true, response.getData());
            verify(cardService).isCardActive(CARD_ID);
        }

        @Test
        void isActiveCardShouldReturnFalse() {
            Request request = new Request(COMMAND);
            request.setAuthContext(userAuth);

            when(cardService.isCardActive(eq(CARD_ID))).thenReturn(false);

            Response response = controller.handle(request);

            assertTrue(response.isSuccess());
            assertEquals(false, response.getData());
            verify(cardService).isCardActive(CARD_ID);
        }
    }
}