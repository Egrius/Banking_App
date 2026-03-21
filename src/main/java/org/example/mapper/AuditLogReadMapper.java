package org.example.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.audit_log.AuditLogReadDto;
import org.example.entity.AuditLog;

@RequiredArgsConstructor
public class AuditLogReadMapper implements BaseMapper<AuditLog, AuditLogReadDto> {
    private final UserReadMapper userReadMapper;
    private final TransactionReadMapper transactionReadMapper;

    @Override
    public AuditLogReadDto map(AuditLog object) {
        return new AuditLogReadDto(
                userReadMapper.map(object.getUser()),
                object.getEmail(),
                object.getUserIp(),
                object.getUserAgent(),
                object.getActionType(),
                object.getEntityType(),
                object.getEntityId(),
                object.getOldValue(),
                object.getNewValue(),
                object.getStatus(),
                object.getErrorMessage(),
                object.getCreatedAt(),
                object.getThreadName(),
                transactionReadMapper.map(object.getTransaction()),
                object.getExecutionTimeMs()
        );
    }
}
