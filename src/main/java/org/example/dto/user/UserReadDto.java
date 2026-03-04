package org.example.dto.user;

import java.time.LocalDateTime;

public record UserReadDto(
        String firstName,
        String lastName,
        String email,
        LocalDateTime createdAt
) { }
