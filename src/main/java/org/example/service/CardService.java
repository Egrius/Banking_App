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
 * <p>
 * Обеспечивает выпуск, просмотр, блокировку и удаление карт.
 * Все операции доступны только владельцу карты или администратору.
 * </p>
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
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * </p>
     * <p>Генерирует номер карты в формате: XXXX-XXXX-XXXX-YYYY, (полный номер не создается в целях демонстрации)
     * где YYYY — последние 4 цифры случайного числа.</p>
     *
     * @param createDto    DTO с данными для выпуска карты
     * @return DTO с информацией о выпущенной карте
     * @throws EntityNotFoundException если счет не найден
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public CardReadDto createCard(CardCreateDto createDto) {

        ValidatorUtil.validate(createDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Account account = accountDao.findById(em, createDto.accountId())
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + createDto.accountId() + " не найден"));

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
     * <p>
     * <b>Доступно владельцу карты или администратору.</b>
     * </p>
     *
     * @param cardId       идентификатор карты
     * @return DTO с информацией о карте
     * @throws EntityNotFoundException если карта не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public CardReadDto getCard(Long cardId) {

        EntityManager em = emf.createEntityManager();

        try {
            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            return mapToReadDto(card);

        } finally {
            em.close();
        }
    }

    /**
     * Получение всех карт пользователя.
     * <p>
     * <b>Доступно владельцу карт или администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @return список DTO карт пользователя
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public List<CardReadDto> getUserCards(Long userId) {

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
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * </p>
     *
     * @param accountId    идентификатор счета
     * @return список DTO карт счета
     * @throws EntityNotFoundException если счет не найден
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public List<CardReadDto> getAccountCards(Long accountId) {

        EntityManager em = emf.createEntityManager();

        try {
            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

            return cardDao.findByAccountId(em, accountId).stream()
                    .map(this::mapToReadDto)
                    .collect(Collectors.toList());

        } finally {
            em.close();
        }
    }

    /**
     * Обновление информации о карте (название).
     * <p>
     * <b>Доступно владельцу карты или администратору.</b>
     * </p>
     *
     * @param cardId       идентификатор карты
     * @param updateDto    DTO с обновленными данными
     * @return DTO с обновленной информацией о карте
     * @throws EntityNotFoundException если карта не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public CardReadDto updateCard(Long cardId, CardUpdateDto updateDto) {

        ValidatorUtil.validate(updateDto);

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));


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

    // TODO: навесить оптимистичную блокировку
    /**
     * Блокировка карты.
     * <p>
     * <b>Доступно владельцу карты или администратору.</b>
     * Заблокированную карту нельзя использовать для операций.
     * </p>
     *
     * @param cardId       идентификатор карты
     * @param reason       причина блокировки
     * @throws EntityNotFoundException если карта не найдена
     * @throws IllegalStateException если карта уже заблокирована или истек срок
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public void blockCard(Long cardId, String reason) {

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            Card card = cardDao.findById(em, cardId)
                    .orElseThrow(() -> new EntityNotFoundException("Карта с id " + cardId + " не найдена"));

            if (card.getStatus() == CardStatus.BLOCKED) {
                throw new IllegalStateException("Карта уже заблокирована");
            }

            if (card.getStatus() == CardStatus.EXPIRED) {
                throw new IllegalStateException("Нельзя заблокировать карту с истекшим сроком действия");
            }

            card.setStatus(CardStatus.BLOCKED);
            cardDao.update(em, card);
            tx.commit();

            log.info("Карта {} заблокирована. Причина: {}",
                    card.getCardNumber(), reason);

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }


    // TODO: навесить оптимистичную блокировку
    /**
     * Разблокировка карты.
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param cardId       идентификатор карты
     * @throws EntityNotFoundException если карта не найдена
     * @throws IllegalStateException если карта не заблокирована
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public void unblockCard(Long cardId) {

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
     * Удаление карты (физическое удаление из БД).
     * <p>
     * <b>Доступно только администратору.</b>
     * </p>
     *
     * @param cardId       идентификатор карты
     * @throws EntityNotFoundException если карта не найдена
     * @throws org.example.exception.security_exception.AccessDeniedException если недостаточно прав
     */
    public void deleteCard(Long cardId) {

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
     *
     * @param cardId идентификатор карты
     * @param em     EntityManager
     * @return {@code true} если карта активна и не истек срок
     */
    public boolean isCardActive(Long cardId, EntityManager em) {
        return cardDao.findById(em, cardId)
                .map(card -> card.getStatus() == CardStatus.ACTIVE &&
                        card.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // TODO: пересмотреть метод
    /**
     * Генерация номера карты.
     * Формат: XXXX-XXXX-XXXX-YYYY, где YYYY — последние 4 цифры.
     *
     * @return маскированный номер карты
     */
    private String generateCardNumber() {
        long random = (long) (Math.random() * 10_000_000_000L);
        String last4 = String.format("%04d", random % 10000);
        return String.format("****-****-****-%s", last4);
    }

    /**
     * Расчет срока действия карты (5 лет от текущей даты).
     *
     * @return дата истечения срока действия
     */
    private LocalDateTime calculateExpiryDate() {
        return LocalDateTime.now().plusYears(5);
    }

    /**
     * Преобразование сущности в DTO.
     *
     * @param card сущность карты
     * @return DTO карты
     */
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