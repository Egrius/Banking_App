package org.example.dto.user;

import org.example.annotation.AtLeastOneFieldNotBlank;

@AtLeastOneFieldNotBlank
public record UserUpdateDto(
        String firstNameUpdated,

        String lastNameUpdated
) { }
