package org.example.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.account.AccountReadDto;
import org.example.entity.Account;

@RequiredArgsConstructor
public class AccountReadMapper implements BaseMapper<Account, AccountReadDto>{

    private final UserReadMapper userReadMapper;

    @Override
    public AccountReadDto map(Account object) {
        return new AccountReadDto(
                object.getId(),
                userReadMapper.map(object.getUser()),
                object.getAccountNumber(),
                object.getBalance(),
                object.getCurrencyCode(),
                object.getAccountType(),
                object.getStatus(),
                object.getOpeningDate(),
                object.getClosingDate()
        );
    }
}
