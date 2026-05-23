package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form-backing pro vytvoření (a editaci) účtu adminem. Při editaci je heslo nepovinné
 * a email se nemění (read-only v UI).
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminAccountForm {

    private Long id;

    @NotBlank @Email
    private String email;

    /** Při create povinné, při edit volitelné (prázdný = nezměnit). */
    @Size(min = 8, max = 100, message = "Heslo musí mít aspoň 8 znaků")
    private String password;

    @NotBlank @Size(max = 64)
    private String nickname;

    @NotBlank @Size(max = 64)
    private String firstName;

    @NotBlank @Size(max = 64)
    private String lastName;

    @Size(max = 32)
    private String phone;

    @NotNull
    private AccountRole role = AccountRole.USER;
}
