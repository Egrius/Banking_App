package org.example;


import org.example.config.HibernateConfig;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        try (SessionFactory sessionFactory = HibernateConfig.createSessionFactory()) {

            try (Session session = sessionFactory.openSession()) {
                session.beginTransaction();

                System.out.println("Hibernate работает!");

                session.getTransaction().commit();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
