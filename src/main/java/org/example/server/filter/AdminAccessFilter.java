package org.example.server.filter;

import org.example.dto.Request;
import org.example.exception.security_exception.AccessDeniedException;

import java.util.Set;

// Обязательно должен идти после фильтра AuthFilter
public class AdminAccessFilter extends BaseRequestFilter {

    private static final Set<String> ADMIN_COMMANDS_PREFIXES = Set.of("role");

    @Override
    public void doFilterInternal(Request request) {

        // Возможна проблема из-за дубляжа split, т.к выше в диспетчере уже такое было

        if (ADMIN_COMMANDS_PREFIXES.contains(request.getCommand().split("\\.")[0])) {
            if (!request.getAuthContext().isAdmin()) {
                throw new AccessDeniedException("Нет доступа для функций администратора");
            }
        }
    }
}
