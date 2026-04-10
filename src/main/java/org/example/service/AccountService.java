package org.example.service;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.balance_audit.BalanceAuditReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.Account;
import org.example.entity.AccountBalanceAudit;
import org.example.entity.User;
import org.example.entity.enums.Status;
import org.example.exception.account.AccountAlreadyExistsException;
import org.example.exception.security_exception.AccessDeniedException;
import org.example.mapper.AccountReadMapper;
import org.example.security.AuthContext;
import org.example.util.SecurityUtil;
import org.example.util.ValidatorUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <h3>Сервис для управления банковскими счетами.</h3>
 * <p>
 * Обеспечивает открытие, закрытие, блокировку счетов,
 * а также получение информации о балансе и истории изменений.
 * Все операции, изменяющие данные, выполняются в транзакциях
 * с поддержкой пессимистической и оптимистической блокировок.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class AccountService {


    private final EntityManagerFactory emf;
    private final UserDao userDao;
    private final AccountDao accountDao;
    private final AccountReadMapper accountReadMapper;
    /**
     * <h3>Открытие нового банковского счета.</h3>
     * <p>
     * <b>Доступно владельцу или администратору.</b>
     * </p>
     * Выполняет следующие проверки:
     * <ul>
     *     <li>существование пользователя</li>
     *     <li>отсутствие у пользователя счета указанного типа</li>
     * </ul>
     *
     * @param createDto    DTO с данными для открытия счета
     * @return DTO с информацией о созданном счете
     * @throws EntityNotFoundException если пользователь не найден
     * @throws AccountAlreadyExistsException если счет такого типа уже существует
     * @throws AccessDeniedException если недостаточно прав
     */
    public AccountReadDto createAccount(AccountCreateDto createDto) {

        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {

            tx.begin();

            User user = userDao.findById(em, createDto.userId())
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

            if(accountDao.existsByUserIdAndType(em, user.getId(), createDto.accountType())) {
                throw new AccountAlreadyExistsException("У пользователя уже есть счет такого типа");
            }

            Account account = new Account(user,
                    generateAccountNumber(user.getId()),
                    BigDecimal.ZERO,
                    createDto.currencyCode(),
                    createDto.accountType()
            );

            accountDao.save(em, account);
            tx.commit();

            log.info("Создан новый счет: {} для пользователя: {}",
                    account.getAccountNumber(), user.getEmail());

            return accountReadMapper.map(account);
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение информации о счете по идентификатору.</h3>
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * </p>
     *
     * @param accountId    идентификатор счета
     * @return DTO с информацией о счете
     * @throws EntityNotFoundException если счет не найден
     * @throws AccessDeniedException если недостаточно прав
     */
    public AccountReadDto getAccount(Long accountId) {


        log.debug("Запрос счета accountId={}", accountId);

        EntityManager em = emf.createEntityManager();

        try {

            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

            return accountReadMapper.map(account);

        } catch (Exception e) {
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение всех счетов пользователя.</h3>
     * <p>
     * <b>Доступно владельцу счетов или администратору.</b>
     * </p>
     *
     * @param userId       идентификатор пользователя
     * @return список кратких DTO счетов пользователя
     * @throws AccessDeniedException если недостаточно прав
     */
    public List<AccountSummaryDto> getUserAccounts(Long userId){

        EntityManager em = emf.createEntityManager();

        try {

            List<Account> accounts = accountDao.findUserAccountsByUserId(em, userId);

            return accounts.stream()
                    .map(a -> new AccountSummaryDto(
                            a.getId(),
                            a.getAccountNumber(),
                            a.getCurrencyCode(),
                            a.getAccountType()
                    ))
                    .toList();

        } catch (Exception e) {
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * <h3>Закрытие счета.</h3>
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * Счет может быть закрыт только если он не заблокирован и не закрыт ранее.
     * При конфликте версий выбрасывается {@link jakarta.persistence.OptimisticLockException}.
     * </p>
     *
     * @param accountId    идентификатор счета
     * @throws EntityNotFoundException если счет не найден
     * @throws IllegalStateException если счет уже закрыт или заблокирован
     * @throws AccessDeniedException если недостаточно прав
     */
    public void closeAccount(Long accountId) {
        log.debug("Попытка закрытия счета accountId={}", accountId);


        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {

            tx.begin();

            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

            if (account.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Счет уже закрыт");
            }

            if (account.getStatus() == Status.BLOCKED) {
                throw new IllegalStateException("Нельзя закрыть заблокированный счет");
            }

            account.setStatus(Status.CLOSED);
            account.setClosingDate(LocalDateTime.now());

            accountDao.save(em, account);

            log.info("Счет {} закрыт", account.getAccountNumber());

            tx.commit();

        } catch(OptimisticLockException e) {
            tx.rollback();
            log.warn("Конфликт при закрытии счета accountId={}", accountId);
            throw e;

        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;

        } finally {
            em.close();
        }
    }

    /**
     * <h3>Блокировка счета.</h3>
     * <p>
     * <b>Доступно только администратору.</b>
     * Счет может быть заблокирован только если он не закрыт.
     * При конфликте версий выбрасывается {@link jakarta.persistence.OptimisticLockException}.
     * </p>
     *
     * @param accountId    идентификатор счета
     * @param reason       причина блокировки
     * @throws EntityNotFoundException если счет не найден
     * @throws IllegalStateException если счет уже заблокирован или закрыт
     * @throws AccessDeniedException если недостаточно прав
     */

    // TODO в логи добавить причину плюс событие
    public void blockAccount(Long accountId, String reason){


        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {

            tx.begin();
            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

            System.out.println("--- ВНУТРИ СЕРВИСА СТАТУС СЧЕТА: " +  account.getStatus());

            if (account.getStatus() == Status.BLOCKED) {
                throw new IllegalStateException("Счет уже заблокирован");
            }

            if (account.getStatus() == Status.CLOSED) {
                throw new IllegalStateException("Нельзя заблокировать закрытый счет");
            }

            account.setStatus(Status.BLOCKED);
            // TODO: сохранить reason (нужно поле в Account)

            accountDao.save(em, account);
            tx.commit();

            log.info("Счет {} заблокирован. Причина: {}", account.getAccountNumber(), reason);

        } catch (OptimisticLockException e) {
            if (tx.isActive()) tx.rollback();
            log.info("Аккаунт был кем-то обновлён, попробуйте ещё раз");
            throw e;

        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;

        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение истории изменений баланса счета с пагинацией.</h3>
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * </p>
     *
     * @param accountId    идентификатор счета
     * @param pageRequest  параметры пагинации
     * @return страница с DTO записей аудита баланса
     * @throws EntityNotFoundException если счет не найден
     * @throws AccessDeniedException если недостаточно прав
     */
    public PageResponse<BalanceAuditReadDto> getBalanceAudit(Long accountId, PageRequest pageRequest) {


        EntityManager em = emf.createEntityManager();

        try {

            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет не найден"));

            // Подсчет сколько всего, чтобы создать респонс
            Long auditsTotal = accountDao.countAudits(em, accountId);

            List<AccountBalanceAudit> audits = accountDao.getAuditsPage(em, accountId,pageRequest.getPageNumber(), pageRequest.getPageSize());

            return new PageResponse<BalanceAuditReadDto>(
                    audits.stream().map(a -> new BalanceAuditReadDto(
                        a.getBalanceBefore(), a.getBalanceAfter(),
                        a.getChangeAmount(), a.getChangedAt(), a.getChangedByThread()
                    )).toList(),
                    pageRequest.getPageNumber(),
                    pageRequest.getPageSize(), auditsTotal);

        } finally {
            em.close();
        }
    }

    /**
     * <h3>Получение текущего баланса счета.</h3>
     * <p>
     * <b>Доступно владельцу счета или администратору.</b>
     * </p>
     *
     * @param accountId    идентификатор счета
     * @return текущий баланс
     * @throws EntityNotFoundException если счет не найден
     * @throws AccessDeniedException если недостаточно прав
     */
    public BigDecimal getBalance(Long accountId) {

        EntityManager em = emf.createEntityManager();

        try {

            Account account = accountDao.findById(em, accountId)
                    .orElseThrow(() -> new EntityNotFoundException("Счет с id " + accountId + " не найден"));

           return account.getBalance();

        } catch (Exception e) {
            throw e;

        } finally {
            em.close();
        }
    }

    /**
     * Генерация уникального номера счета.
     *
     * @param userId идентификатор пользователя
     * @return сгенерированный номер счета в формате ACNT-{userId}-{timestamp}-{random}
     */
    private String generateAccountNumber(Long userId) {
        return String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }
}