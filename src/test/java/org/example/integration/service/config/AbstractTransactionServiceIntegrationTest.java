package org.example.integration.service.config;

import jakarta.persistence.EntityManager;
import org.example.dao.AccountDao;
import org.example.dao.AuditDao;
import org.example.dao.IdempotencyKeyDao;
import org.example.dao.TransactionDao;
import org.example.entity.Account;
import org.example.integration.AbstractIntegrationTest;
import org.example.mapper.*;
import org.example.service.AuditService;
import org.example.service.IdempotencyService;
import org.example.service.TransactionService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AbstractTransactionServiceIntegrationTest extends AbstractIntegrationTest {

    protected AccountDao accountDao;
    protected TransactionDao transactionDao;
    protected IdempotencyKeyDao keyDao;
    protected AuditDao auditDao;

    protected IdempotencyKeyReadMapper keyReadMapper;
    protected IdempotencyKeyCreateMapper keyCreateMapper;
    protected RoleReadMapper roleReadMapper;
    protected UserReadMapper userReadMapper;
    protected AccountReadMapper accountReadMapper;
    protected TransactionReadMapper transactionReadMapper;
    protected AuditLogReadMapper auditLogReadMapper;

    protected IdempotencyService idempotencyService;
    protected AuditService auditService;
    protected TransactionService transactionService;

    @BeforeAll
    void initServices() {
        // DAO
        accountDao = new AccountDao();
        transactionDao = new TransactionDao();
        keyDao = new IdempotencyKeyDao();
        auditDao = new AuditDao();

        // Мапперы
        roleReadMapper = new RoleReadMapper();
        userReadMapper = new UserReadMapper(roleReadMapper);
        accountReadMapper = new AccountReadMapper(userReadMapper);
        transactionReadMapper = new TransactionReadMapper(accountReadMapper);

        keyReadMapper = new IdempotencyKeyReadMapper();
        keyCreateMapper = new IdempotencyKeyCreateMapper();
        auditLogReadMapper = new AuditLogReadMapper(userReadMapper, transactionReadMapper);

        // Сервисы (sessionFactory от родителя)
        idempotencyService = IdempotencyService.getInstance(sessionFactory, keyDao, keyReadMapper, keyCreateMapper);
        auditService = new AuditService(sessionFactory, auditDao, auditLogReadMapper);
        transactionService = new TransactionService(sessionFactory, accountDao, transactionDao, transactionReadMapper, idempotencyService, auditService);
    }
    /*
    Здесь дальше хэлпер-методы
     */

    protected Optional<Account> findAccountById(Long id) {
        try(EntityManager em = sessionFactory.createEntityManager();) {
            return Optional.of(em.find(Account.class, id));
        }

    }
}