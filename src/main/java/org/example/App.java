package org.example;


import org.example.config.HibernateConfig;
import org.example.util.ValidatorUtil;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

// TODO: перенести AuthContext на слой выше сервисного
// TODO: Перенести валидацию DTO на слой выше сервисного
// TODO: Сделать связку AuthService с логин методом и сделать ThreadLocal айди сессии у клиента

public class App 
{
    public static void main( String[] args )
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ValidatorUtil.close();
        }));

        try (SessionFactory sessionFactory = HibernateConfig.createSessionFactory()) {

            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();

                System.out.println("Hibernate работает!");

                session.getTransaction().commit();

            } catch (HibernateException e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
