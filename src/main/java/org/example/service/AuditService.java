package org.example.service;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AuditDao;
import org.example.dto.audit_log.AuditLogReadDto;
import org.example.dto.request.PageRequest;
import org.example.dto.response.PageResponse;
import org.example.entity.AuditLog;
import org.example.mapper.AuditLogReadMapper;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class AuditService {

    private final AuditDao auditDao;
    private final EntityManagerFactory emf;

    private final AuditLogReadMapper auditLogReadMapper;

    private static final ExecutorService asyncLogPool = Executors.newSingleThreadExecutor();

    // TODO асинхронный аудит для некритических, со своим собственным EM, а для транзакций брать тот же и делить один контекст

    /**
     * Сохранить запись аудита
     */
    public void logInTransaction(EntityManager em, AuditLog auditLog) {
        EntityTransaction tx = em.getTransaction();
        try {
            if(!tx.isActive()) {
                throw new IllegalStateException("logInTransaction() требует наличия активной транзакции!");
            }
            auditDao.save(em, auditLog);

        } catch (Exception e) {
            throw e;
        }
    }

    public void logIndependent(AuditLog auditLog) {
        EntityManager independentEm = emf.createEntityManager();
        EntityTransaction tx = independentEm.getTransaction();
        try {
            tx.begin();
            auditDao.save(independentEm, auditLog);
            tx.commit();
        } catch (Exception e) {
            if(tx.isActive()) tx.rollback();
            throw e;
        } finally {
            independentEm.close();
        }
    }

    public void logAsync(AuditLog auditLog) {
        asyncLogPool.submit(() -> {
            EntityManager em = emf.createEntityManager();
            EntityTransaction tx = em.getTransaction();
            try {
                tx.begin();
                auditDao.save(em, auditLog);
                tx.commit();
            } catch (Exception e) {
                if(tx.isActive()) tx.rollback();
                throw e;
            } finally {
                em.close();
            }
        });
    }

    /**
     * Получить записи аудита по пользователю
     */
    public PageResponse<AuditLogReadDto> getUserAuditLogs(Long userId, PageRequest pageRequest) {
        EntityManager em = emf.createEntityManager();
        try {
            Long auditsTotal = auditDao.countTotalWithUserId(em, userId);
            List<AuditLogReadDto> result = auditDao.findByUserId(em, userId, pageRequest)
                    .stream()
                    .map(auditLogReadMapper::map)
                    .toList();

            return new PageResponse<AuditLogReadDto>(result, pageRequest.getPageNumber(), pageRequest.getPageSize(), auditsTotal);
        } finally {
            em.close();
        }
    }

    //public PageResponse<AuditLogReadDto> getByFilter(AuditFilter filter) {

    //}

    /**
     * Очистить старые записи
     */
    // public void cleanOldLogs(LocalDateTime beforeDate) {
    //     // TODO: реализовать удаление старых записей
    // }

    public void shutdown() {
        asyncLogPool.shutdown();
        try {
            if(!asyncLogPool.awaitTermination(3, TimeUnit.SECONDS)) asyncLogPool.shutdownNow();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
