package com.ragnarok.ragnarok_customers_training_diary.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form-backing object pro {@code register.html}. Thymeleaf vyžaduje mutable POJO
 * (ne record), aby šel data-binding přes {@code th:object} a {@code th:field}.
 */
@Getter
@Setter
@NoArgsConstructor
public class RegistrationForm {

    @NotBlank(message = "Email je povinný")
    @Email(message = "Neplatný formát emailu")
    private String email;

    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, max = 100, message = "Heslo musí mít aspoň 8 znaků")
    private String password;

    @NotBlank(message = "Přezdívka je povinná")
    @Size(max = 64)
    private String nickname;

    @NotBlank(message = "Jméno je povinné")
    @Size(max = 64)
    private String firstName;

    @NotBlank(message = "Příjmení je povinné")
    @Size(max = 64)
    private String lastName;

    @Size(max = 32, message = "Telefon je moc dlouhý")
    private String phone;

    // Phase 6: notifikační preference (default true)
    private boolean notifGroupTrainingReminder = true;
    private boolean notifNewPlanAssigned = true;
    private boolean notifNewComment = true;
    private boolean notifWelcome = true;
}
