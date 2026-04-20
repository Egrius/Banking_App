package org.example.integration.service.business_logic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardReadDto;
import org.example.dto.card.CardUpdateDto;
import org.example.entity.Account;
import org.example.entity.Card;
import org.example.entity.User;
import org.example.entity.enums.AccountType;
import org.example.entity.enums.CardStatus;
import org.example.entity.enums.CardType;
import org.example.entity.enums.CurrencyCode;
import org.example.integration.service.config.AbstractCardServiceIntegrationTest;
import org.example.security.AuthContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CardServiceBusinessLogicIT extends AbstractCardServiceIntegrationTest {

    @BeforeEach
    void cleanUp() {
        deleteAllCards();
        deleteAllUsers();
        deleteAllAccounts();
        runSql("role_service/init_role_table.sql");
    }

    @Nested
    class CreateCardTests {

        @Test
        void createCard_shouldCreateNewCardSuccessfully() {
            User user = createUserInDB("cardtest@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardCreateDto createDto = new CardCreateDto(account.getId(), CurrencyCode.US, "CARDHOLDER NAME", CardType.DEBIT, "My Debit Card");

            CardReadDto createdCard = cardService.createCard(createDto, authContext);

            assertNotNull(createdCard);
            assertNotNull(createdCard.id());
            assertEquals(account.getId(), createdCard.accountId());
            assertEquals(CurrencyCode.US, createdCard.currencyCode());
            assertEquals("CARDHOLDER NAME", createdCard.cardholderName());
            assertEquals(CardType.DEBIT, createdCard.cardType());
            assertEquals(CardStatus.ACTIVE, createdCard.status());
            assertEquals("My Debit Card", createdCard.name());
            assertNotNull(createdCard.cardNumber());
            assertNotNull(createdCard.expiryDate());

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Card cardFromDb = cardDao.findById(em, createdCard.id()).orElseThrow();
                assertEquals(createdCard.cardNumber(), cardFromDb.getCardNumber());
                assertEquals(createdCard.cardholderName(), cardFromDb.getCardholderName());
            } finally {
                em.close();
            }
        }

        @Test
        void createCard_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("cardtest@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));
            CardCreateDto createDto = new CardCreateDto(999L, CurrencyCode.US, "HOLDER NAME", CardType.DEBIT, "My Card");

            assertThatThrownBy(() -> cardService.createCard(createDto, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }

        @Test
        void createCard_shouldThrowException_whenAccountHasMaxCardsLimit() {
            User user = createUserInDB("maxcards@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            for (int i = 0; i < 5; i++) {
                createCardInDB(account.getId(), generateCardNumber(), "Holder " + i, CardType.DEBIT, CardStatus.ACTIVE, "Test name");
            }

            CardCreateDto sixthCard = new CardCreateDto(account.getId(), CurrencyCode.US, "Sixth Holder", CardType.DEBIT, "Sixth Card");

            assertThatThrownBy(() -> cardService.createCard(sixthCard, authContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Нельзя выпустить более 5 карт");
        }
    }

    @Nested
    class GetCardTests {

        @Test
        void getCard_shouldReturnCard_whenCardExists() {
            User user = createUserInDB("getcard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), "Get Card Holder", CardType.DEBIT, CardStatus.ACTIVE, "Test name");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardReadDto foundCard = cardService.getCard(card.getId(), authContext);

            assertNotNull(foundCard);
            assertEquals(card.getId(), foundCard.id());
            assertEquals(card.getCardNumber(), foundCard.cardNumber());
            assertEquals(card.getCardholderName(), foundCard.cardholderName());
        }

        @Test
        void getCard_shouldThrowEntityNotFoundException_whenCardNotFound() {
            User user = createUserInDB("getcard@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            assertThatThrownBy(() -> cardService.getCard(999L, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class GetUserCardsTests {

        @Test
        void getUserCards_shouldReturnAllUserCards() {
            User user = createUserInDB("usercards@test.com", "password123", "Test", "User");
            Account account1 = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Account account2 = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.EUR, AccountType.CREDIT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            Card card1 = createCardInDB(account1.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Card One");
            Card card2 = createCardInDB(account1.getId(), generateCardNumber(), user.getEmail(), CardType.CREDIT, CardStatus.ACTIVE, "Card Two");
            Card card3 = createCardInDB(account2.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Card Three");

            List<CardReadDto> userCards = cardService.getUserCards(user.getId(), authContext);

            assertNotNull(userCards);
            assertEquals(3, userCards.size());
            assertTrue(userCards.stream().anyMatch(c -> c.id().equals(card1.getId())));
            assertTrue(userCards.stream().anyMatch(c -> c.id().equals(card2.getId())));
            assertTrue(userCards.stream().anyMatch(c -> c.id().equals(card3.getId())));
        }

        @Test
        void getUserCards_shouldReturnEmptyList_whenUserHasNoCards() {
            User user = createUserInDB("nocards@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            List<CardReadDto> userCards = cardService.getUserCards(user.getId(), authContext);

            assertNotNull(userCards);
            assertTrue(userCards.isEmpty());
        }
    }

    @Nested
    class GetAccountCardsTests {

        @Test
        void getAccountCards_shouldReturnAllCardsForAccount() {
            User user = createUserInDB("accountcards@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            Card card1 = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Account Card One");
            Card card2 = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.CREDIT, CardStatus.ACTIVE, "Account Card Two");
            Card card3 = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Account Card Three");

            List<CardReadDto> accountCards = cardService.getAccountCards(account.getId(), authContext);

            assertNotNull(accountCards);
            assertEquals(3, accountCards.size());
            assertTrue(accountCards.stream().anyMatch(c -> c.id().equals(card1.getId())));
            assertTrue(accountCards.stream().anyMatch(c -> c.id().equals(card2.getId())));
            assertTrue(accountCards.stream().anyMatch(c -> c.id().equals(card3.getId())));
        }

        @Test
        void getAccountCards_shouldThrowEntityNotFoundException_whenAccountNotFound() {
            User user = createUserInDB("accountcards@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            assertThatThrownBy(() -> cardService.getAccountCards(999L, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найден");
        }
    }

    @Nested
    class UpdateCardTests {

        @Test
        void updateCard_shouldUpdateCardNameSuccessfully() {
            User user = createUserInDB("updatecard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Old card name");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardUpdateDto updateDto = new CardUpdateDto("New Card Name");

            CardReadDto updatedCard = cardService.updateCard(card.getId(), updateDto, authContext);

            assertEquals("New Card Name", updatedCard.name());
            assertEquals(card.getCardNumber(), updatedCard.cardNumber());

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Card cardFromDb = cardDao.findById(em, card.getId()).orElseThrow();
                assertEquals("New Card Name", cardFromDb.getName());
            } finally {
                em.close();
            }
        }

        @Test
        void updateCard_shouldKeepOldName_whenUpdateDtoHasNullName() {
            User user = createUserInDB("keepname@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Original Name");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardUpdateDto updateDto = new CardUpdateDto(null);

            CardReadDto updatedCard = cardService.updateCard(card.getId(), updateDto, authContext);

            assertEquals("Original Name", updatedCard.name());
        }

        @Test
        void updateCard_shouldThrowEntityNotFoundException_whenCardNotFound() {
            User user = createUserInDB("updatecard@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));
            CardUpdateDto updateDto = new CardUpdateDto("New Name");

            assertThatThrownBy(() -> cardService.updateCard(999L, updateDto, authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class BlockCardTests {

        @Test
        void blockCard_shouldBlockActiveCardSuccessfully() {
            User user = createUserInDB("blockcard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "To Block");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            cardService.blockCard(card.getId(), "Lost card", authContext);

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Card cardFromDb = cardDao.findById(em, card.getId()).orElseThrow();
                assertEquals(CardStatus.BLOCKED, cardFromDb.getStatus());
            } finally {
                em.close();
            }
        }

        @Test
        void blockCard_shouldThrowException_whenCardAlreadyBlocked() {
            User user = createUserInDB("alreadyblocked@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.BLOCKED, "Already Blocked");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            assertThatThrownBy(() -> cardService.blockCard(card.getId(), "Second block", authContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("уже заблокирована");
        }

        @Test
        void blockCard_shouldThrowEntityNotFoundException_whenCardNotFound() {
            User user = createUserInDB("blockcard@test.com", "password123", "Test", "User");
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            assertThatThrownBy(() -> cardService.blockCard(999L, "Reason", authContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class UnblockCardTests {

        @Test
        void unblockCard_shouldUnblockBlockedCardSuccessfully() {
            User user = createUserInDB("unblockcard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.BLOCKED, "To Unblock");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            cardService.unblockCard(card.getId(), adminContext);

            EntityManager em = sessionFactory.createEntityManager();
            try {
                Card unblockedCard = cardDao.findById(em, card.getId()).orElseThrow();
                assertEquals(CardStatus.ACTIVE, unblockedCard.getStatus());
            } finally {
                em.close();
            }
        }

        @Test
        void unblockCard_shouldThrowException_whenCardNotBlocked() {
            User user = createUserInDB("notblocked@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Active Card");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> cardService.unblockCard(card.getId(), adminContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Разблокировать можно только заблокированную карту");
        }

        @Test
        void unblockCard_shouldThrowEntityNotFoundException_whenCardNotFound() {
            User user = createUserInDB("unblockcard@test.com", "password123", "Test", "User");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> cardService.unblockCard(999L, adminContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class DeleteCardTests {

        @Test
        void deleteCard_shouldDeleteCardSuccessfully() {
            User user = createUserInDB("deletecard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "To Delete");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            EntityManager em = sessionFactory.createEntityManager();
            try {
                assertTrue(cardDao.findById(em, card.getId()).isPresent());
            } finally {
                em.close();
            }

            cardService.deleteCard(card.getId(), adminContext);

            em = sessionFactory.createEntityManager();
            try {
                assertFalse(cardDao.findById(em, card.getId()).isPresent());
            } finally {
                em.close();
            }
        }

        @Test
        void deleteCard_shouldThrowEntityNotFoundException_whenCardNotFound() {
            User user = createUserInDB("deletecard@test.com", "password123", "Test", "User");
            AuthContext adminContext = new AuthContext(user.getId(), user.getEmail(), List.of("ADMIN"));

            assertThatThrownBy(() -> cardService.deleteCard(999L, adminContext))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("не найдена");
        }
    }

    @Nested
    class IsCardActiveTests {

        @Test
        void isCardActive_shouldReturnTrue_whenCardIsActive() {
            User user = createUserInDB("activecard@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.ACTIVE, "Active Card");

            boolean isActive = cardService.isCardActive(card.getId());

            assertTrue(isActive);
        }

        @Test
        void isCardActive_shouldReturnFalse_whenCardIsBlocked() {
            User user = createUserInDB("blockedactive@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            Card card = createCardInDB(account.getId(), generateCardNumber(), user.getEmail(), CardType.DEBIT, CardStatus.BLOCKED, "Blocked Card");

            boolean isActive = cardService.isCardActive(card.getId());

            assertFalse(isActive);
        }

        @Test
        void isCardActive_shouldReturnFalse_whenCardDoesNotExist() {
            boolean isActive = cardService.isCardActive(999L);

            assertFalse(isActive);
        }
    }

    @Nested
    class CardNumberGenerationTests {

        @Test
        void createCard_shouldGenerateUniqueCardNumbers() {
            User user = createUserInDB("uniquenumbers@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardCreateDto card1Dto = new CardCreateDto(account.getId(), CurrencyCode.US, "Card One", CardType.DEBIT, "Card 1");
            CardCreateDto card2Dto = new CardCreateDto(account.getId(), CurrencyCode.US, "Card Two", CardType.CREDIT, "Card 2");
            CardCreateDto card3Dto = new CardCreateDto(account.getId(), CurrencyCode.US, "Card Three", CardType.DEBIT, "Card 3");

            CardReadDto card1 = cardService.createCard(card1Dto, authContext);
            CardReadDto card2 = cardService.createCard(card2Dto, authContext);
            CardReadDto card3 = cardService.createCard(card3Dto, authContext);

            assertNotNull(card1.cardNumber());
            assertNotNull(card2.cardNumber());
            assertNotNull(card3.cardNumber());

            assertNotEquals(card1.cardNumber(), card2.cardNumber());
            assertNotEquals(card1.cardNumber(), card3.cardNumber());
            assertNotEquals(card2.cardNumber(), card3.cardNumber());
        }

        @Test
        void createCard_shouldGenerateCardNumberInCorrectFormat() {
            User user = createUserInDB("format@test.com", "password123", "Test", "User");
            Account account = createAccountInDB(user.getId(), generateAccountNumber(user.getId()), CurrencyCode.US, AccountType.CURRENT);
            AuthContext authContext = new AuthContext(user.getId(), user.getEmail(), List.of("USER"));

            CardCreateDto createDto = new CardCreateDto(account.getId(), CurrencyCode.US, "Format Card", CardType.DEBIT, "Format Card");
            CardReadDto card = cardService.createCard(createDto, authContext);

            String cardNumber = card.cardNumber();
            assertTrue(cardNumber.matches("\\*\\*\\*\\*-\\*\\*\\*\\*-\\*\\*\\*\\*-\\d{4}"),
                    "Card number should match format ****-****-****-XXXX");
        }
    }
}