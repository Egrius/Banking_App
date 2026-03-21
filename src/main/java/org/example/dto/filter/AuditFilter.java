package org.example.dto.filter;

import org.example.entity.enums.ActionType;
import org.example.entity.enums.AuditStatus;
import org.example.entity.enums.EntityType;

import java.time.LocalDateTime;

public record AuditFilter(
        String email,
        ActionType actionType,
        EntityType entityType,
        Long entityId,
        AuditStatus status,
        LocalDateTime fromDate,
        LocalDateTime toDate
) { }
