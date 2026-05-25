package com.ragnarok.ragnarok_customers_training_diary.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Vstupní DTO pro registraci nového klienta.
 *
 * <p>Pole povinná dle požadavku trenéra (potřebné pro pozdější předvyplnění rezervací):
 * {@code firstName}, {@code lastName}, {@code email}. Telefon je nepovinný.
 *
 * <p>Phase 6: 4 notifikační flagy jsou volitelné (null = default true v service).
 */
public record RegistrationRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100, message = "Heslo musí mít aspoň 8 znaků") String password,
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Size(max = 64) String firstName,
        @NotBlank @Size(max = 64) String lastName,
        @Size(max = 32) String phone,
        Boolean notifGroupTrainingReminder,
        Boolean notifNewPlanAssigned,
        Boolean notifNewComment,
        Boolean notifWelcome
) {
    /** Convenience constructor pro backward-compat (testy z fáze 0). */
    public RegistrationRequest(String email, String password, String nickname,
                                String firstName, String lastName, String phone) {
        this(email, password, nickname, firstName, lastName, phone, null, null, null, null);
    }
}
