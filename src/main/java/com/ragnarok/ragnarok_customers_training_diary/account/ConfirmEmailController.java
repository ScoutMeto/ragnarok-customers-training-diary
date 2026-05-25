package com.ragnarok.ragnarok_customers_training_diary.account;

import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UI flow pro potvrzení emailu po registraci.
 *  <ul>
 *    <li>{@code GET /confirm-email} — zobrazí formulář (email + 6místný kód).
 *        Email se předvyplní z flash atributu po POST /register.</li>
 *    <li>{@code POST /confirm-email} — verify kód, při úspěchu redirect na
 *        {@code /login?confirmed}.</li>
 *    <li>{@code POST /confirm-email/resend} — vygeneruje nový kód a pošle mail.</li>
 *  </ul>
 *
 * Veřejně dostupné endpointy (anonymous OK) — viz {@code SecurityConfiguration}.
 */
@Controller
public class ConfirmEmailController {

    private final EmailConfirmationService emailConfirmationService;

    public ConfirmEmailController(EmailConfirmationService emailConfirmationService) {
        this.emailConfirmationService = emailConfirmationService;
    }

    @GetMapping("/confirm-email")
    public String showForm(@RequestParam(value = "email", required = false) String email,
                           Model model) {
        // Email může přijít z flash atributu (z POST /register) nebo z query parametru
        if (email != null && !email.isBlank() && !model.containsAttribute("email")) {
            model.addAttribute("email", email);
        }
        return "confirm-email";
    }

    @PostMapping("/confirm-email")
    public String verifyCode(@RequestParam("email") String email,
                             @RequestParam("code") String code,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        try {
            emailConfirmationService.verifyCode(email, code);
            return "redirect:/login?confirmed";
        } catch (EmailConfirmationException ex) {
            model.addAttribute("email", email);
            model.addAttribute("error", ex.getMessage());
            return "confirm-email";
        }
    }

    @PostMapping("/confirm-email/resend")
    public String resendCode(@RequestParam("email") String email,
                              RedirectAttributes redirectAttributes) {
        try {
            emailConfirmationService.resendCode(email);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Nový kód byl odeslán na " + email + ". Zkontroluj svůj inbox.");
        } catch (NotFoundException ex) {
            redirectAttributes.addFlashAttribute("error", "Účet s tímto emailem nenajdeme.");
        }
        redirectAttributes.addFlashAttribute("email", email);
        return "redirect:/confirm-email";
    }
}
