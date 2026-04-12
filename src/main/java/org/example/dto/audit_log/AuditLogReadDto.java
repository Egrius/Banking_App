package org.example.dto.audit_log;

import org.example.dto.user.UserReadDto;
import org.example.entity.enums.ActionType;
import org.example.entity.enums.AuditStatus;
import org.example.entity.enums.EntityType;

import java.time.LocalDateTime;

public record AuditLogReadDto(
        UserReadDto userReadDto,
        String email,
        String userIp,
        String userAgent,
        ActionType actionType,
        EntityType entityType,
        Long entityId,
        String oldValue,
        String newValue,
        AuditStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        String threadName,
        Long executionTimeMs
) { }
