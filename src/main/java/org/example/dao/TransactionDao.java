package org.example.dao;

import jakarta.persistence.EntityManager;
import org.example.entity.BankTransaction;
import org.example.entity.enums.Status;
import org.example.entity.enums.TransactionStatus;

import java.util.List;

public class TransactionDao extends BaseDaoImpl<BankTransaction, Long> {
    public TransactionDao() {
        super(BankTransaction.class);
    }

    public List<BankTransaction> findByToAccountId(EntityManager em, Long accountId, int pageNum, int pageSize) {
        return em.createQuery(
                "SELECT t FROM BankTransaction t " +
                "JOIN FETCH t.toAccount " +
                "JOIN FETCH t.fromAccount " +
                "WHERE toAccount.id = :accountId " +
                "ORDER BY t.createdAt DESC ", BankTransaction.class)
                .setFirstResult(pageNum * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    public long countByToAccountId(EntityManager em, Long accountId) {
        return em.createQuery(
                        "SELECT COUNT(t) FROM BankTransaction t " +
                                "WHERE t.toAccount.id = :accountId", Long.class)
                .setParameter("accountId", accountId)
                .getSingleResult();
    }

    public TransactionStatus getTransactionStatus(EntityManager em, Long transactionId) {
        return em.createQuery("SELECT t.status FROM BankTransaction t WHERE t.id = :transactionId", TransactionStatus.class)
                .setParameter("transactionId", transactionId)
                .getSingleResult();
    }

}
