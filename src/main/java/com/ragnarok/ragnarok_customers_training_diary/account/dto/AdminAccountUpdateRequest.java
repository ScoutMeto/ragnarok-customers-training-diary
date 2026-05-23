package com.ragnarok.ragnarok_customers_training_diary.account.dto;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin update účtu. Email se NEMĚNÍ (kvůli historickým komentářům a ownership).
 * Heslo je volitelné — vyplň jen když ho chceš změnit.
 */
public record AdminAccountUpdateRequest(
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Size(max = 64) String firstName,
        @NotBlank @Size(max = 64) String lastName,
        @Size(max = 32) String phone,
        AccountRole role,
        @Size(min = 8, max = 100, message = "Nové heslo musí mít aspoň 8 znaků") String newPassword
) {}
