package com.ragnarok.ragnarok_customers_training_diary.web;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import com.ragnarok.ragnarok_customers_training_diary.account.EmailAlreadyTakenException;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Render Thymeleaf stránek (login, register, dashboard). Login a logout handler
 * provádí přímo Spring Security (form-login na {@code /login}, viz
 * {@link com.ragnarok.ragnarok_customers_training_diary.configuration.SecurityConfiguration}).
 */
@Controller
public class PageController {

    private final AccountService accountService;
    private final TrainingService trainingService;

    public PageController(AccountService accountService, TrainingService trainingService) {
        this.accountService = accountService;
        this.trainingService = trainingService;
    }

    @GetMapping("/")
    public String root(@AuthenticationPrincipal AccountEntity account) {
        return account != null ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@AuthenticationPrincipal AccountEntity account) {
        if (account != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(@AuthenticationPrincipal AccountEntity account, Model model) {
        if (account != null) {
            return "redirect:/dashboard";
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationForm());
        }
        return "register";
    }

    @PostMapping("/register")
    public String submitRegistration(
            @Valid @ModelAttribute("form") RegistrationForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            accountService.register(new RegistrationRequest(
                    form.getEmail(),
                    form.getPassword(),
                    form.getNickname(),
                    form.getFirstName(),
                    form.getLastName(),
                    form.getPhone()
            ));
        } catch (EmailAlreadyTakenException ex) {
            bindingResult.rejectValue("email", "email.taken", "Účet s tímto emailem už existuje.");
            return "register";
        }

        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AccountEntity account, Model model) {
        model.addAttribute("account", account);
        // Tři nejnovější vlastní tréninky pro rychlý přístup
        var recent = trainingService.listMyTrainings(account).stream().limit(3).toList();
        model.addAttribute("recentTrainings", recent);
        return "dashboard";
    }
}
