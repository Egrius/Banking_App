package org.example.service;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dao.AccountDao;
import org.example.dao.UserDao;
import org.example.dto.account.AccountCreateDto;
import org.example.dto.account.AccountReadDto;
import org.example.entity.Account;
import org.example.entity.User;
import org.example.mapper.AccountReadMapper;
import org.example.util.ValidatorUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final UserDao userDao;
    private final AccountDao accountDao;
    private final AccountReadMapper accountReadMapper;
    private final EntityManagerFactory emf;

    public AccountReadDto createAccount(AccountCreateDto createDto) {

        ValidatorUtil.validate(createDto);

        User user = userDao.findById(createDto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if(accountDao.existsByUserIdAndType(user.getId(), createDto.accountType())) {
            throw new IllegalStateException("У пользователя уже есть счет такого типа");
        }

        Account account = new Account(user,
                generateAccountNumber(user.getId()),
                BigDecimal.ZERO,
                createDto.currencyCode(),
                createDto.accountType()
                );

        accountDao.save(account);

        log.info("Создан новый счет: {} для пользователя: {}",
                account.getAccountNumber(), user.getEmail());

        return accountReadMapper.map(account);
    }

    private String generateAccountNumber(Long userId) {
        return String.format("ACNT-%d-%d-%d",
                userId,
                System.currentTimeMillis(),
                (int)(Math.random() * 1000));
    }

}