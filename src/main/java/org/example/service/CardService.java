package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.CardDao;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardReadDto;
import org.example.dto.card.CardUpdateDto;
import org.example.entity.Account;
import org.example.entity.Card;
import org.example.entity.enums.CardStatus;
import org.example.security.AuthContext;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами.
 */
@Slf4j
@RequiredArgsConstructor
public class CardService {

    private final EntityManagerFactory emf;
    private final CardDao cardDao;
    private final AccountDao accountDao;

    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("MM/yy");

    /**
     * Выпуск новой карты.
     */
    public CardReadDto createCard(CardCreateDto createDto, AuthContext authContext) {
        ValidatorUtil.validate(createDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Account account = accountDao.findById(em, createDto.accountId())
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + createDto.accountId() + " не найден"));

            // Проверка прав: владелец счета или админ
            SecurityUtil.checkAdminOrOwner(authContext, account.getUser().getId());

            // Проверка лимита карт на счете (не более 5)
            long cardCount = cardDao.countByAccountId(em, account.getId());
            if (cardCount >= 5) {
                throw new IllegalStateException("Нельзя выпустить более 5 карт на один счет");
            }

            Card card = Card.builder()
                    .user(account.getUser())
                    .account(account)
                    .cardNumber(generateCardNumber())
                    .currencyCode(createDto.currencyCode())
                    .cardholderName(createDto.cardholderName())
                    .expiryDate(calculateExpiryDate())
                    .cardType(createDto.cardType())
                    .status(CardStatus.ACTIVE)
                    .name(createDto.name())
                    .build();

            cardDao.save(em, card);
            tx.commit();

            log.info("Выпущена новая карта {} для счета {}",
                    card.getCardNumber(), account.getAccountNumber());

            return mapToReadDto(card);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Получение карты по идентификатору.
     */
    public CardReadDto getCard(Long cardId, AuthContext authContext) {
        EntityManager em = emf.createEntityManager();

        try {
            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            SecurityUtil.checkAdminOrOwner(authContext, card.getUser().getId());

            return mapToReadDto(card);

        } finally {
            em.close();
        }
    }

    /**
     * Получение всех карт пользователя.
     */
    public List<CardReadDto> getUserCards(Long userId, AuthContext authContext) {
        SecurityUtil.checkAdminOrOwner(authContext, userId);

        EntityManager em = emf.createEntityManager();

        try {
            return cardDao.findByUserId(em, userId).stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toList());

        } finally {
            em.close();
        }
    }

    /**
     * Получение всех карт счета.
     */
    public List<CardReadDto> getAccountCards(Long accountId, AuthContext authContext) {
        EntityManager em = emf.createEntityManager();

        try {
            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

            SecurityUtil.checkAdminOrOwner(authContext, account.getUser().getId());

            return cardDao.findByAccountId(em, accountId).stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toList());

        } finally {
            em.close();
        }
    }

    /**
     * Обновление информации о карте (название).
     */
    public CardReadDto updateCard(Long cardId, CardUpdateDto updateDto, AuthContext authContext) {
        ValidatorUtil.validate(updateDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            SecurityUtil.checkAdminOrOwner(authContext, card.getUser().getId());

            if (updateDto.name() != null) {
                card.setName(updateDto.name());
            }

            cardDao.update(em, card);
            tx.commit();

            log.info("Обновлена карта {}", card.getCardNumber());

            return mapToReadDto(card);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Блокировка карты.
     */
    public void blockCard(Long cardId, String reason, AuthContext authContext) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            SecurityUtil.checkAdminOrOwner(authContext, card.getUser().getId());

            if (card.getStatus() == CardStatus.BLOCKED) {
                throw new IllegalStateException("Карта уже заблокирована");
            }

            if (card.getStatus() == CardStatus.EXPIRED) {
                throw new IllegalStateException("Нельзя заблокировать карту с истекшим сроком действия");
            }

            card.setStatus(CardStatus.BLOCKED);
            cardDao.update(em, card);
            tx.commit();

            log.info("Карта {} заблокирована. Причина: {}", card.getCardNumber(), reason);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Разблокировка карты (только для администратора).
     */
    public void unblockCard(Long cardId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            if (card.getStatus() != CardStatus.BLOCKED) {
                throw new IllegalStateException("Разблокировать можно только заблокированную карту");
            }

            card.setStatus(CardStatus.ACTIVE);
            cardDao.update(em, card);
            tx.commit();

            log.info("Карта {} разблокирована", card.getCardNumber());

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Удаление карты (только для администратора).
     */
    public void deleteCard(Long cardId, AuthContext authContext) {
        SecurityUtil.checkAdmin(authContext);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            cardDao.delete(em, card);
            tx.commit();

            log.info("Карта {} удалена", card.getCardNumber());

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Проверка активности карты.
     */
    public boolean isCardActive(Long cardId) {

        try(EntityManager em = emf.createEntityManager();) {
            return cardDao.findById(em, cardId)
                    .map(card -> card.getStatus() == CardStatus.ACTIVE &&
                            card.getExpiryDate().isAfter(LocalDateTime.now()))
                    .orElse(false);
        }

    }

    private String generateCardNumber() {
        long random = (long) (Math.random() * 10_000_000_000L);
        String last4 = String.format("%04d", random % 10000);
        return String.format("****-****-****-%s", last4);
    }

    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusYears(5);
    }

    private CardReadDto mapToReadDto(Card card) {
        return new CardReadDto(
                card.getId(),
                card.getUser().getId(),
                card.getUser().getFirstName() + " " + card.getUser().getLastName(),
                card.getAccount().getId(),
                card.getAccount().getAccountNumber(),
                card.getCardNumber(),
                card.getCurrencyCode(),
                card.getCardholderName(),
                card.getExpiryDate().format(EXPIRY_FORMAT),
                card.getCardType(),
                card.getStatus(),
                card.getName(),
                card.getExpiryDate().isBefore(LocalDateTime.now())
        );
    }
}