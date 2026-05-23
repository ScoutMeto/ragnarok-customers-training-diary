package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.account.dto.AccountResponse;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Veřejné REST endpointy pro účty.
 *
 * <ul>
 *     <li>{@code POST /api/auth/register} — registrace klienta (anonymní)</li>
 *     <li>{@code GET  /api/auth/me} — info o přihlášeném klientovi</li>
 * </ul>
 *
 * Login a logout řeší Spring Security form-login na {@code /login} a {@code /logout}
 * (viz {@link com.ragnarok.ragnarok_customers_training_diary.configuration.SecurityConfiguration}).
 */
@RestController
@RequestMapping("/api/auth")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegistrationRequest request) {
        AccountEntity created = accountService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(created));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> me(@AuthenticationPrincipal AccountEntity account) {
        if (account == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<String> handleEmailTaken(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}
