package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.entity.Account;
import org.example.entity.AccountBalanceAudit;
import org.example.entity.enums.AccountType;
import java.util.List;

public class AccountDao extends BaseDaoImpl<Account, Long> {
    public AccountDao() {
        super(Account.class);
    }

    public boolean existsById(EntityManager em, Long accountId) {
        return !em.createQuery("SELECT 1 FROM Account a WHERE a.id = :accountId")
                .setParameter("accountId", accountId)
                .setMaxResults(1)
                .getResultList()
                .isEmpty();
    }

    public boolean existsByUserIdAndType(EntityManager em,
                                         Long userId, AccountType accountType) {
            return !em.createQuery("SELECT 1 FROM Account a JOIN a.user u WHERE u.id = :userId AND a.accountType = :accountType")
                    .setParameter("accountType", accountType.getAbbreviation().toUpperCase())
                    .setParameter("userId", userId)
                    .setMaxResults(1)
                    .getResultList()
                    .isEmpty();
    }

    public List<Account> findUserAccountsByUserId(EntityManager em, Long userId) {
        return em.createQuery("SELECT a FROM Account a JOIN a.user u WHERE u.id = :userId", Account.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<AccountBalanceAudit> getAuditsPage(EntityManager em, Long accountId, Integer pageNum, Integer pageSize) {
        return em.createQuery(
                "SELECT aud FROM AccountBalanceAudit aud " +
                        "WHERE aud.account.id = :accountId " +
                        "ORDER BY aud.changedAt DESC", AccountBalanceAudit.class)
                .setFirstResult(pageNum * pageSize)
                .setMaxResults(pageSize)
                .setParameter("accountId", accountId)
                .getResultList();
    }

    public Long countAudits(EntityManager em, Long accountId) {
        return em.createQuery(
                "SELECT COUNT(aud) FROM AccountBalanceAudit aud " +
                        "WHERE aud.account.id = :accountId", Long.class)
                .setParameter("accountId", accountId)
                .getSingleResult();
    }
}