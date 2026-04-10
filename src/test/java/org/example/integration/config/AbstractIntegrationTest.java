package org.example.integration.config;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIntegrationTest {

    private static final String PATH_TO_SQL_FOLDER = "src/test/java/org/example/integration/sql/";
    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("bank_db_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    protected SessionFactory sessionFactory;

    @BeforeAll
    void initSessionFactory() {
        createSessionFactory();
    }

    private SessionFactory createSessionFactory() {
        if (sessionFactory == null) {
            Configuration configuration = new Configuration();

            Properties properties = new Properties();

            properties.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
            properties.setProperty("hibernate.connection.url", postgres.getJdbcUrl());
            properties.setProperty("hibernate.connection.username", postgres.getUsername());
            properties.setProperty("hibernate.connection.password", postgres.getPassword());

            properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");

            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");

            properties.setProperty("hibernate.connection.pool_size", "5");

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
    void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    protected void runSql(String pathToSqlFile) {
        if(!pathToSqlFile.endsWith(".sql")) throw new IllegalArgumentException("Передан файл с некорректным расширением, ожидался .sql файл!");

        Path path = Path.of(PATH_TO_SQL_FOLDER + pathToSqlFile);

        try {
            String sql = new String(Files.readAllBytes(path));

            try(Session session = sessionFactory.openSession()) {
                session.beginTransaction();
                for(String statemnt : sql.split(";")) {
                    if(!statemnt.trim().isEmpty()) {
                        session.createNativeQuery(statemnt).executeUpdate();
                    }
                }
                session.getTransaction().commit();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}