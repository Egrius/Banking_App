package org.example.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

public class HibernateConfig {
    public static SessionFactory createSessionFactory() {
        Configuration configuration = new Configuration();

        Properties properties = new Properties();

        properties.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        properties.setProperty("hibernate.connection.url", "jdbc:postgresql://localhost:5432/bankdb");
        properties.setProperty("hibernate.connection.username", "postgres");
        properties.setProperty("hibernate.connection.password", "2Pg8_06Egr");

        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        properties.setProperty("hibernate.hbm2ddl.auto", "create");

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

        return configuration.buildSessionFactory();

    }
}
