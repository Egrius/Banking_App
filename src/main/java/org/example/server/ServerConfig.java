package org.example.server;

import org.example.config.HibernateConfig;
import org.example.dao.*;
import org.example.mapper.*;
import org.example.server.dispatcher.RequestDispatcher;
import org.example.server.filter.AdminAccessFilter;
import org.example.server.filter.AuthFilter;
import org.example.server.filter_chain.FilterChain;
import org.example.service.*;
import org.example.util.ValidatorUtil;
import org.hibernate.SessionFactory;

public class ServerConfig {
    // Hibernate
    private final SessionFactory sessionFactory;

    // DAO
    private final AccountDao accountDao;
    private final UserDao userDao;
    private final TransactionDao transactionDao;
    private final CardDao cardDao;
    private final RoleDao roleDao;
    private final IdempotencyKeyDao keyDao;
    private final AuditDao auditDao;

    // Мапперы
    private final TransactionReadMapper transactionReadMapper;
    private final AccountReadMapper accountReadMapper;
    private final UserReadMapper userReadMapper;
    private final UserUpdateMapper userUpdateMapper;
    private final RoleReadMapper roleReadMapper;
    private final IdempotencyKeyReadMapper keyReadMapper;
    private final IdempotencyKeyCreateMapper keyCreateMapper;
    private final AuditLogReadMapper auditLogReadMapper;

    // Сервисы
    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final CardService cardService;
    private final RoleService roleService;
    private final AuthenticationService authenticationService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    // Компоненты
    private final FilterChain filterChain;
    private final RequestDispatcher dispatcher;
    private final Server server;

    public static ServerConfig defaultServerConfig(int port) {
        return new ServerConfig(port, HibernateConfig.createSessionFactory());
    }

    public static ServerConfig customHibernateSessionFactoryServerConfig(int port, SessionFactory sessionFactory) {
        return new ServerConfig(port, sessionFactory);
    }

    private ServerConfig(int serverPort, SessionFactory sessionFactory) {
        // 1. Hibernate
        this.sessionFactory = sessionFactory;

        // 2. DAO
        this.accountDao = new AccountDao();
        this.userDao = new UserDao();
        this.roleDao = new RoleDao();
        this.transactionDao = new TransactionDao();
        this.cardDao = new CardDao();
        this.keyDao = new IdempotencyKeyDao();
        this.auditDao = new AuditDao();

        // 3. Мапперы
        this.roleReadMapper = new RoleReadMapper();

        this.userReadMapper = new UserReadMapper(roleReadMapper);
        this.userUpdateMapper = new UserUpdateMapper();

        this.accountReadMapper = new AccountReadMapper(userReadMapper);

        this.transactionReadMapper = new TransactionReadMapper(accountReadMapper);

        this.keyReadMapper = new IdempotencyKeyReadMapper();
        this.keyCreateMapper = new IdempotencyKeyCreateMapper();

        this.auditLogReadMapper = new AuditLogReadMapper(userReadMapper, transactionReadMapper);

        // 4. Сервисы
        this.idempotencyService = IdempotencyService.getInstance(sessionFactory, keyDao, keyReadMapper, keyCreateMapper);
        this.authenticationService = new AuthenticationService();
        this.userService = new UserService(sessionFactory, authenticationService, userDao, roleDao, userReadMapper, userUpdateMapper);
        this.accountService = new AccountService(sessionFactory, userDao, accountDao, accountReadMapper);
        this.cardService = new CardService(sessionFactory, cardDao, accountDao);
        this.auditService = new AuditService(sessionFactory, auditDao, auditLogReadMapper);
        this.transactionService = new TransactionService(sessionFactory, accountDao, transactionDao, transactionReadMapper, idempotencyService, auditService);
        this.roleService = new RoleService(sessionFactory, roleDao, userDao, userReadMapper);

        // 5. Фильтры
        this.filterChain = new FilterChain()
                .addFilter(new AuthFilter(authenticationService))
                .addFilter(new AdminAccessFilter());

        // 6. Диспатчер
        this.dispatcher = new RequestDispatcher(userService, accountService, cardService, roleService, transactionService, filterChain);

        // 7. Сервер
        this.server = new Server(dispatcher, serverPort);
    }

    public Server getServer() {
        return server;
    }

    public void shutdown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        ValidatorUtil.close();
    }
}
