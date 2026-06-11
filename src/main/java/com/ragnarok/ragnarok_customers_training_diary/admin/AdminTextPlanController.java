package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Phase 21: admin správa textových šablon plánu (prostý text). Vedle běžných šablon tréninku.
 * ROLE_ADMIN vynuceno security configem na /admin/**.
 */
@Controller
@RequestMapping("/admin/text-plans")
public class AdminTextPlanController {

    private final TextPlanService textPlanService;
    private final AccountRepository accountRepository;

    public AdminTextPlanController(TextPlanService textPlanService, AccountRepository accountRepository) {
        this.textPlanService = textPlanService;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("templates", textPlanService.listTemplates());
        return "admin/text-plans/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("editingId", null);
        model.addAttribute("planTitle", "");
        model.addAttribute("planBody", "");
        return "admin/text-plans/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AccountEntity admin,
                         @RequestParam("title") String title,
                         @RequestParam("body") String body,
                         RedirectAttributes flash) {
        try {
            var created = textPlanService.createTemplate(admin, title, body);
            flash.addFlashAttribute("flashSuccess",
                    "Textová šablona vytvořena. Přiřaď ji klientovi, nebo ji nech nepřiřazenou.");
            return "redirect:/admin/text-plans/" + created.getId() + "/assign";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/text-plans/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var tpl = textPlanService.getTemplate(id);
        model.addAttribute("editingId", id);
        model.addAttribute("planTitle", tpl.getTitle());
        model.addAttribute("planBody", tpl.getBody());
        return "admin/text-plans/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam("title") String title,
                         @RequestParam("body") String body,
                         RedirectAttributes flash) {
        try {
            textPlanService.updateTemplate(id, title, body);
            flash.addFlashAttribute("flashSuccess", "Textová šablona upravena.");
        } catch (IllegalArgumentException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/text-plans";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            textPlanService.deleteTemplate(id);
            flash.addFlashAttribute("flashSuccess", "Textová šablona smazána.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/text-plans";
    }

    @GetMapping("/{id}/assign")
    public String assignForm(@PathVariable Long id, Model model) {
        model.addAttribute("template", textPlanService.getTemplate(id));
        model.addAttribute("clients",
                accountRepository.findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(AccountRole.USER));
        return "admin/text-plans/assign";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id, @RequestParam Long clientId, RedirectAttributes flash) {
        AccountEntity client = accountRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Klient (id=" + clientId + ") nenalezen."));
        textPlanService.assignToUser(id, client.getId());
        flash.addFlashAttribute("flashSuccess",
                "Textový plán přiřazen klientovi " + client.getFirstName() + " " + client.getLastName() + ".");
        return "redirect:/admin/text-plans";
    }
}
