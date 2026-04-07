package org.example.interceptor;

import org.example.entity.AuditLog;
import org.example.entity.BankTransaction;
import org.example.entity.enums.ActionType;
import org.example.entity.enums.AuditStatus;
import org.example.entity.enums.EntityType;
import org.example.service.AuditService;
import org.hibernate.Interceptor;
import org.hibernate.event.spi.*;
import org.hibernate.type.Type;

import java.time.LocalDateTime;
import java.util.Iterator;

public class AuditLogTransactionInterceptor implements PostInsertEventListener, PostUpdateEventListener {

    private final AuditService auditService;

    public AuditLogTransactionInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Object entity = event.getEntity();
        if(entity instanceof BankTransaction) {
            BankTransaction transaction = (BankTransaction) entity;

            AuditLog auditLog = AuditLog.builder()
                    .user(transaction.getFromAccount().getUser())
                    .email(transaction.getFromAccount().getUser().getEmail())
                    .actionType(ActionType.TRANSFER_INITIATE)
                    .entityType(EntityType.TRANSACTION)
                    .entityId(transaction.getId())
                    .status(AuditStatus.SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .build();

            auditService.logIndependent(auditLog);

        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        if(entity instanceof BankTransaction) {
            BankTransaction transaction = (BankTransaction) entity;

            AuditLog auditLog = AuditLog.builder()
                    .user(transaction.getFromAccount().getUser())
                    .email(transaction.getFromAccount().getUser().getEmail())
                    .actionType(ActionType.TRANSFER_COMPLETE)
                    .entityType(EntityType.TRANSACTION)
                    .entityId(transaction.getId())
                    .status(AuditStatus.SUCCESS)
                    .createdAt(LocalDateTime.now())
                    .threadName(Thread.currentThread().getName())
                    .build();

            auditService.logIndependent(auditLog);
        }

    }
}
