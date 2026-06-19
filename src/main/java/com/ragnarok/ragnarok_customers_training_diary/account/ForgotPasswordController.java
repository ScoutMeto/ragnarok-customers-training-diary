package com.ragnarok.ragnarok_customers_training_diary.account;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * UI flow pro zapomenuté heslo (ScoutMeto kolo 6):
 *  <ul>
 *    <li>{@code GET /forgot-password} — formulář (email).</li>
 *    <li>{@code POST /forgot-password} — pošle 6místný kód na email, redirect na reset.</li>
 *    <li>{@code GET /reset-password} — formulář (email + kód + nové heslo).</li>
 *    <li>{@code POST /reset-password} — ověří kód a nastaví nové heslo → redirect login.</li>
 *  </ul>
 *
 * Veřejně dostupné (anonymous) — viz {@code SecurityConfiguration}.
 */
@Controller
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String sendCode(@RequestParam("email") String email, RedirectAttributes flash) {
        passwordResetService.startReset(email);
        // Neutrální hláška — neúnikáme, jestli účet existuje.
        flash.addFlashAttribute("flashSuccess",
                "Pokud účet s tímto emailem existuje, poslali jsme na něj kód pro obnovu hesla.");
        flash.addFlashAttribute("email", email);
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email != null && !email.isBlank() && !model.containsAttribute("email")) {
            model.addAttribute("email", email);
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("code") String code,
                                @RequestParam("password") String password,
                                @RequestParam("passwordConfirm") String passwordConfirm,
                                Model model) {
        if (!password.equals(passwordConfirm)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Hesla se neshodují.");
            return "reset-password";
        }
        try {
            passwordResetService.resetPassword(email, code, password);
            return "redirect:/login?reset";
        } catch (EmailConfirmationException ex) {
            model.addAttribute("email", email);
            model.addAttribute("error", ex.getMessage());
            return "reset-password";
        }
    }
}
