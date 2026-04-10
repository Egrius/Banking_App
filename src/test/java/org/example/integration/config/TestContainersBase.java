package org.example.integration.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Properties;

@Testcontainers
public class TestContainersBase {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("bank_db_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    public static SessionFactory sessionFactory;

    private static SessionFactory createSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration();

            Properties properties = new Properties();

            properties.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            properties.setProperty("hibernate.connection.url", postgres.getJdbcUrl());
            properties.setProperty("hibernate.connection.username", postgres.getUsername());
            properties.setProperty("hibernate.connection.password", postgres.getPassword());

            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

            properties.setProperty("hibernate.hbm2ddl.auto", "drop-create");

            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");

            properties.setProperty("hibernate.connection.pool_size", "5");

            properties.setProperty("hibernate.jdbc.lock.use_none_for_update", "false");

            configuration.setProperties(properties);

            configuration.addAnnotatedClass(org.example.entity.User.class);
            configuration.addAnnotatedClass(org.example.entity.Role.class);
            configuration.addAnnotatedClass(org.example.entity.TransactionTemplate.class);
            configuration.addAnnotatedClass(org.example.entity.BankTransaction.class);
            configuration.addAnnotatedClass(org.example.entity.Card.class);
            configuration.addAnnotatedClass(org.example.entity.AuditLog.class);
            configuration.addAnnotatedClass(org.example.entity.AccountBalanceAudit.class);
            configuration.addAnnotatedClass(org.example.entity.Account.class);
            configuration.addAnnotatedClass(org.example.entity.IdempotencyKey.class);

            sessionFactory = configuration.buildSessionFactory();
        }
        return sessionFactory;
    }

    @AfterAll
    static void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }
}