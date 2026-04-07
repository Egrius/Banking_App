package org.example.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.example.dto.audit_log.AuditLogReadDto;
import org.example.dto.filter.AuditFilter;
import org.example.dto.request.PageRequest;
import org.example.entity.AuditLog;

import java.util.ArrayList;
import java.util.List;

public class AuditDao extends BaseDaoImpl<AuditLog, Long>{
    public AuditDao() {
        super(AuditLog.class);
    }

    public List<AuditLog> findByUserId(EntityManager em, Long userId, PageRequest pageRequest) {
        return em.createQuery("SELECT a FROM AuditLog a WHERE a.userId = :userId", AuditLog.class)
                .setParameter("userId", userId)
                .setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize())
                .setMaxResults(pageRequest.getPageSize())
                .getResultList();
    }

    // TODO: добавить пагинацию
    public List<AuditLogReadDto> findByFilter(EntityManager em, AuditFilter filter, PageRequest pageRequest) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<AuditLogReadDto> auditQuery = cb.createQuery(AuditLogReadDto.class);
        Root<AuditLog> root = auditQuery.from(AuditLog.class);

        List<Predicate> predicates = new ArrayList<>();

        if(filter.email() != null && !filter.email().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("email")), "%" + filter.email().toLowerCase() + "%"));
        }

        if(filter.actionType() != null) {
            predicates.add(cb.equal(root.get("actionType"), filter.actionType().name()));
        }

        if(filter.entityType() != null) {
            predicates.add(cb.equal(root.get("entityType"), filter.entityType().name()));
        }

        if(filter.entityId() != null) {
            predicates.add(cb.equal(root.get("entityId"), filter.entityId()));
        }

        if(filter.status() != null) {
            predicates.add(cb.equal(root.get("status"), filter.status().name()));
        }

        if(filter.fromDate() != null) {
            if(filter.toDate() != null) {
                predicates.add(cb.between(root.get("createdAt"), filter.fromDate(), filter.toDate()));
            } else {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.fromDate()));
            }
        }
        auditQuery.where(cb.and(predicates.toArray(Predicate[]::new)));

        auditQuery.select(cb.construct(
                AuditLogReadDto.class,
                root.get("user"),
                root.get("email"),
                root.get("userIp"),
                root.get("userAgent"),
                root.get("actionType"),
                root.get("entityType"),
                root.get("entityId"),
                root.get("oldValue"),
                root.get("newValue"),
                root.get("status"),
                root.get("errorMessage"),
                root.get("createdAt"),
                root.get("threadName"),
                root.get("transaction"),
                root.get("executionTimeMs")
        ));

        List<AuditLogReadDto> result = em.createQuery(auditQuery).getResultList();
        return result;
    }

    public Long countTotalWithUserId(EntityManager em, Long userId) {
        return em.createQuery("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }


}
