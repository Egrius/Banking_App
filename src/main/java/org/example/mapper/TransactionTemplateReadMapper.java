package org.example.mapper;

import lombok.RequiredArgsConstructor;
import org.example.dto.account.AccountSummaryDto;
import org.example.dto.transaction_template.TransactionTemplateReadDto;
import org.example.entity.TransactionTemplate;

@RequiredArgsConstructor
public class TransactionTemplateReadMapper implements BaseMapper<TransactionTemplate, TransactionTemplateReadDto>{

    @Override
    public TransactionTemplateReadDto map(TransactionTemplate object) {
        return new TransactionTemplateReadDto(
                object.getBeneficiaryName(),
                object.getBeneficiaryAccountNumber(),
                object.getBeneficiaryBank(),
                object.getBeneficiaryBic(),
                object.getBeneficiaryInn(),
                object.getAmount(),
                object.getCurrencyCode(),
                object.getDescription(),
                new AccountSummaryDto(
                        object.getAccount().getId(),
                        object.getAccount().getAccountNumber(),
                        object.getAccount().getCurrencyCode(),
                        object.getAccount().getAccountType()
                ),
                new AccountSummaryDto(
                        object.getDefaultAccount().getId(),
                        object.getDefaultAccount().getAccountNumber(),
                        object.getDefaultAccount().getCurrencyCode(),
                        object.getDefaultAccount().getAccountType()
                ),
                object.getCreatedAt(),
                object.getLastUsedAt(),
                object.getUsageCount()
        );
    }

}
