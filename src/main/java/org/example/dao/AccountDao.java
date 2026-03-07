package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.example.entity.Account;
import org.example.entity.enums.AccountType;

public class AccountDao extends BaseDaoImpl<Account, Long> {
    public AccountDao() {
        super(Account.class);
    }

    public boolean existsByUserIdAndType(EntityManager em,
                                         Long userId, AccountType accountType) {

        try {
            em.createQuery("SELECT 1 FROM Account a JOIN a.user u WHERE u.id = :userId AND a.accountType = :accountType")
                    .setParameter("accountType", accountType)
                    .setParameter("userId", userId)
                    .setMaxResults(1)
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

}
