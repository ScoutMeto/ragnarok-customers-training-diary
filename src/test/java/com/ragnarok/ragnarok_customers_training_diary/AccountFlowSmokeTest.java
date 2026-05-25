package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smoke testy klíčových auth-flow scénářů: registrace přes REST, registrace přes form,
 * úspěšný i selhaný login, ochrana chráněných stránek.
 *
 * <p>Běží na H2 v profile {@code test}. Není to vyčerpávající test suite — jen rychlá
 * pojistka, že Fáze 0 funguje end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountFlowSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void registerNewClient_viaRestApi_returns201AndPersists() throws Exception {
        String body = """
                {
                  "email": "klient@example.com",
                  "password": "tajneheslo",
                  "nickname": "klient1",
                  "firstName": "Jan",
                  "lastName": "Novák",
                  "phone": "+420777111222"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("klient@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.firstName").value("Jan"));

        var saved = accountRepository.findByEmail("klient@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(AccountRole.USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("tajneheslo"); // hashováno
    }

    @Test
    void registerSameEmailTwice_returns409Conflict() throws Exception {
        String body = """
                {
                  "email": "dvojce@example.com",
                  "password": "tajneheslo",
                  "nickname": "dvojce",
                  "firstName": "Petr",
                  "lastName": "Dvojce"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    void registerWithInvalidEmail_returns400() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "password": "tajneheslo",
                  "nickname": "x",
                  "firstName": "X",
                  "lastName": "Y"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void formLogin_withValidCredentials_redirectsToDashboard() throws Exception {
        // 1. Zaregistruj klienta
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"login@example.com","password":"tajneheslo",
                                 "nickname":"login","firstName":"Login","lastName":"Test"}"""))
                .andExpect(status().isCreated());

        // Phase 6: simuluj potvrzení emailu (testujeme login confirmed accountu)
        var acc = accountRepository.findByEmail("login@example.com").orElseThrow();
        acc.setEmailConfirmed(true);
        acc.setEmailConfirmationCode(null);
        accountRepository.save(acc);

        // 2. Pošli form-login a ověř redirect + autentikovaný stav
        mockMvc.perform(formLogin("/login").user("login@example.com").password("tajneheslo"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void formLogin_withInvalidPassword_isUnauthenticated() throws Exception {
        mockMvc.perform(formLogin("/login").user("nonexistent@example.com").password("wrong"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void dashboardAnonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void registerForm_persistsViaThymeleafPost() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "form@example.com")
                        .param("password", "tajneheslo")
                        .param("nickname", "formuser")
                        .param("firstName", "Form")
                        .param("lastName", "User")
                        .param("phone", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        assertThat(accountRepository.existsByEmail("form@example.com")).isTrue();
    }
}
