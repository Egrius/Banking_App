package org.example.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.transation.TransactionReadDto;
import org.example.entity.BankTransaction;

@RequiredArgsConstructor
public class TransactionReadMapper implements BaseMapper<BankTransaction, TransactionReadDto> {
    private final AccountReadMapper accountReadMapper;

    @Override
    public TransactionReadDto map(BankTransaction object) {
        return new TransactionReadDto(
                object.getId(),
                accountReadMapper.map(object.getFromAccount()),
                accountReadMapper.map(object.getToAccount()),
                object.getAmount(),
                object.getStatus(),
                object.getFailureReason(),
                object.getRetryCount(),
                object.getFromBalanceBefore(),
                object.getFromBalanceAfter(),
                object.getToBalanceBefore(),
                object.getToBalanceAfter(),
                object.getCreatedAt(),
                object.getProcessedAt(),
                object.getDescription()
        );
    }
}
