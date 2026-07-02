package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import com.ragnarok.ragnarok_customers_training_diary.account.EmailAlreadyTakenException;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.AdminAccountUpdateRequest;
import com.ragnarok.ragnarok_customers_training_diary.account.dto.RegistrationRequest;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin sekce pro správu klientů a dalších adminů (trenérů).
 *
 * <ul>
 *     <li>{@code GET /admin/accounts} — seznam aktivních účtů</li>
 *     <li>{@code GET /admin/accounts/new} — formulář nový</li>
 *     <li>{@code POST /admin/accounts} — vytvořit</li>
 *     <li>{@code GET /admin/accounts/{id}} — detail + deníky klienta</li>
 *     <li>{@code GET /admin/accounts/{id}/edit} — formulář edit</li>
 *     <li>{@code POST /admin/accounts/{id}} — uložit edit</li>
 *     <li>{@code POST /admin/accounts/{id}/delete} — soft delete</li>
 * </ul>
 *
 * Autorizace ROLE_ADMIN je vynucena přes URL pattern v
 * {@link com.ragnarok.ragnarok_customers_training_diary.configuration.SecurityConfiguration}.
 */
@Controller
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AccountService accountService;
    private final TrainingService trainingService;
    private final com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;

    public AdminAccountController(AccountService accountService, TrainingService trainingService,
            com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService) {
        this.accountService = accountService;
        this.trainingService = trainingService;
        this.emailService = emailService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("accounts", accountService.listActiveAccounts());
        return "admin/accounts/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        AccountEntity account = accountService.getById(id);
        model.addAttribute("account", account);
        model.addAttribute("trainings", trainingService.listTrainingsOf(id));
        return "admin/accounts/detail";
    }

    // -----------------------------------------------------------------------------
    // Vytvoření nového účtu
    // -----------------------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new AdminAccountForm());
        }
        model.addAttribute("roles", AccountRole.values());
        return "admin/accounts/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") AdminAccountForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (form.getPassword() == null || form.getPassword().isBlank()) {
            bindingResult.rejectValue("password", "password.required", "Heslo je při vytvoření povinné.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", AccountRole.values());
            return "admin/accounts/form";
        }

        try {
            RegistrationRequest req = new RegistrationRequest(
                    form.getEmail(),
                    form.getPassword(),
                    form.getNickname(),
                    form.getFirstName(),
                    form.getLastName(),
                    form.getPhone()
            );
            AccountEntity created = accountService.createByAdmin(req, form.getRole());
            flash.addFlashAttribute("flashSuccess", "Účet vytvořen.");
            return "redirect:/admin/accounts/" + created.getId();
        } catch (EmailAlreadyTakenException ex) {
            bindingResult.rejectValue("email", "email.taken", "Účet s tímto emailem už existuje.");
            model.addAttribute("roles", AccountRole.values());
            return "admin/accounts/form";
        }
    }

    // -----------------------------------------------------------------------------
    // Editace
    // -----------------------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AccountEntity account = accountService.getById(id);
        AdminAccountForm form = new AdminAccountForm();
        form.setId(account.getId());
        form.setEmail(account.getEmail());
        form.setNickname(account.getNickname());
        form.setFirstName(account.getFirstName());
        form.setLastName(account.getLastName());
        form.setPhone(account.getPhone());
        form.setRole(account.getRole());

        model.addAttribute("form", form);
        model.addAttribute("editing", true);
        model.addAttribute("roles", AccountRole.values());
        return "admin/accounts/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") AdminAccountForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true);
            model.addAttribute("roles", AccountRole.values());
            return "admin/accounts/form";
        }

        AdminAccountUpdateRequest req = new AdminAccountUpdateRequest(
                form.getNickname(),
                form.getFirstName(),
                form.getLastName(),
                form.getPhone(),
                form.getRole(),
                form.getPassword() // může být null/blank — service to ignoruje
        );
        accountService.updateByAdmin(id, req);
        flash.addFlashAttribute("flashSuccess", "Účet aktualizován.");
        return "redirect:/admin/accounts/" + id;
    }

    // -----------------------------------------------------------------------------
    // ScoutMeto kolo 7: členství (vstupy / datum konce)
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/membership")
    public String updateMembership(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(value = "membershipEntries", required = false) Short membershipEntries,
            @org.springframework.web.bind.annotation.RequestParam(value = "membershipUntil", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate membershipUntil,
            RedirectAttributes flash) {
        AccountEntity account = accountService.getById(id);
        if (account.getRole() == AccountRole.ADMIN) {
            flash.addFlashAttribute("flashError", "Admin účty členství neevidují.");
            return "redirect:/admin/accounts/" + id;
        }
        account.setMembershipEntries(membershipEntries);
        account.setMembershipUntil(membershipUntil);
        accountService.save(account);
        flash.addFlashAttribute("flashSuccess", "Členství uloženo.");
        return "redirect:/admin/accounts/" + id;
    }

    /** ScoutMeto kolo 7: zapnutí/vypnutí sekce Výhody pro konkrétního uživatele. */
    @PostMapping("/{id}/benefits-toggle")
    public String toggleBenefits(@PathVariable Long id, RedirectAttributes flash) {
        AccountEntity account = accountService.getById(id);
        account.setBenefitsVisible(!account.isBenefitsVisible());
        accountService.save(account);
        flash.addFlashAttribute("flashSuccess", account.isBenefitsVisible()
                ? "Sekce Výhody je pro uživatele viditelná."
                : "Sekce Výhody je pro uživatele skrytá.");
        return "redirect:/admin/accounts/" + id;
    }

    // -----------------------------------------------------------------------------
    // Soft delete
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/delete")
    public String softDelete(@PathVariable Long id, RedirectAttributes flash) {
        accountService.softDelete(id);
        flash.addFlashAttribute("flashSuccess", "Účet anonymizován.");
        return "redirect:/admin/accounts";
    }

    // -----------------------------------------------------------------------------
    // Phase 10: deaktivace / reaktivace (read-only mód pro neplatiče)
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes flash) {
        accountService.deactivate(id);
        // Phase 19b: upozornění klientovi na deaktivaci (respektuje jeho preferenci)
        emailService.sendAccountDeactivatedNotification(accountService.getById(id));
        flash.addFlashAttribute("flashSuccess",
                "Účet deaktivován — klient má teď jen náhled (read-only).");
        return "redirect:/admin/accounts/" + id;
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, RedirectAttributes flash) {
        accountService.reactivate(id);
        flash.addFlashAttribute("flashSuccess", "Účet reaktivován — všechny funkce zpřístupněny.");
        return "redirect:/admin/accounts/" + id;
    }
}
