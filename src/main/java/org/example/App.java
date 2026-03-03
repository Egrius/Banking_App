package org.example;


import org.example.config.HibernateConfig;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class App 
{
    public static void main( String[] args )
    {

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
