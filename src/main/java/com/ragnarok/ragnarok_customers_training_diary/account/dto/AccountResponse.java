package com.ragnarok.ragnarok_customers_training_diary.account.dto;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;

/**
 * Veřejné DTO pro odpověď API — bez password_hash, bez interních flagů.
 */
public record AccountResponse(
        Long id,
        String email,
        String nickname,
        String firstName,
        String lastName,
        String phone,
        AccountRole role
) {
    public static AccountResponse from(AccountEntity entity) {
        return new AccountResponse(
                entity.getId(),
                entity.getEmail(),
                entity.getNickname(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getRole()
        );
    }
}
