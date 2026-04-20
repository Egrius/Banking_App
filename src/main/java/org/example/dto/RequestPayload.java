package org.example.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.card.CardCreateDto;
import org.example.dto.card.CardUpdateDto;
import org.example.dto.request.PageRequest;
import org.example.dto.role.*;
import org.example.dto.transation.TransactionCreateDto;
import org.example.dto.user.PasswordChangeDto;
import org.example.dto.user.UserCreateDto;
import org.example.dto.user.UserLoginDto;
import org.example.dto.user.UserUpdateDto;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = false)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AccountCreateDto.class, name = "account.createAccount"),
        @JsonSubTypes.Type(value = CardCreateDto.class, name = "card.createCard"),
        @JsonSubTypes.Type(value = CardUpdateDto.class, name = "card.updateCard"),
        @JsonSubTypes.Type(value = TransactionCreateDto.class, name = "transaction.transfer"),
        @JsonSubTypes.Type(value = PasswordChangeDto.class, name = "user.changePassword"),
        @JsonSubTypes.Type(value = UserCreateDto.class, name = "user.register"),
        @JsonSubTypes.Type(value = UserLoginDto.class, name = "user.login"),
        @JsonSubTypes.Type(value = UserUpdateDto.class, name = "user.updateUser"),
        @JsonSubTypes.Type(value = CreateRoleDto.class, name = "role.create"),
        @JsonSubTypes.Type(value = AssignRoleDto.class, name = "role.assign"),
        @JsonSubTypes.Type(value = AssignManyRolesDto.class, name = "role.assignMany"),
        @JsonSubTypes.Type(value = RemoveRoleDto.class, name = "role.remove"),
        @JsonSubTypes.Type(value = RemoveAllRolesDto.class, name = "role.removeAll"),
        @JsonSubTypes.Type(value = HasRoleDto.class, name = "role.hasRole"),
        @JsonSubTypes.Type(value = HasAnyRoleDto.class, name = "role.hasAnyRole"),
        @JsonSubTypes.Type(value = HasAllRolesDto.class, name = "role.hasAllRoles"),
        @JsonSubTypes.Type(value = PageRequest.class, name = "pageRequest")
})
public interface RequestPayload {

}
