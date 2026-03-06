package org.example.mapper;

import org.example.dto.user.UserUpdateDto;
import org.example.entity.User;
import org.example.util.PasswordUtil;

public class UserUpdateMapper implements BaseMapper<UserUpdateDto, User>{
    @Override
    public User map(UserUpdateDto object) {
        throw new UnsupportedOperationException("Используйте map(UserUpdateDto fromObject, User toObject)");
    }

    @Override
    public User map(UserUpdateDto fromObject, User toObject) {
       copy(fromObject, toObject);
       return toObject;
    }

    private void copy(UserUpdateDto fromObject, User toObject) {
        if(!fromObject.firstNameUpdated().isBlank()) toObject.setFirstName(fromObject.firstNameUpdated());
        if(!fromObject.lastNameUpdated().isBlank()) toObject.setLastName(fromObject.lastNameUpdated());
    }
}
