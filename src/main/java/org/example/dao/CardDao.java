package org.example.dao;

import jakarta.persistence.EntityManager;
import org.example.entity.Card;

import java.util.List;

public class CardDao extends BaseDaoImpl<Card, Long> {
    public CardDao() {
        super(Card.class);
    }

    public Long countByAccountId(EntityManager em, Long accountId) {
        return em.createQuery("SELECT COUNT(c) FROM Card c " +
                                    "LEFT JOIN c.account a " +
                                    "WHERE a.id = :accountId", Long.class)
                .setParameter("accountId", accountId)
                .getSingleResult();

    }

    public List<Card> findByUserId(EntityManager em, Long userId) {
        return em.createQuery("SELECT c FROM Card c " +
                                    "INNER JOIN FETCH c.user u " +
                                    "INNER JOIN FETCH c.account a " +
                                    "WHERE u.id = :userId", Card.class)
                .setParameter("userId", userId)
                .getResultList();
    }


    public List<Card> findByAccountId(EntityManager em, Long accountId) {
        return em.createQuery("SELECT c FROM Card c " +
                        "LEFT JOIN c.account a " +
                        "WHERE a.id = :accountId", Card.class)
                .setParameter("accountId", accountId)
                .getResultList();
    }
}
