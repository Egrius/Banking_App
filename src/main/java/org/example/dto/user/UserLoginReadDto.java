package org.example.dto.user;

public record UserLoginReadDto (
    UserReadDto userReadDto,
    String jwtToken
){}
