package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import com.ragnarok.ragnarok_customers_training_diary.coach.CoachPlanEntity;
import com.ragnarok.ragnarok_customers_training_diary.coach.CoachPlanService;
import com.ragnarok.ragnarok_customers_training_diary.coach.MarkdownRenderer;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Admin sekce coach plans (markdown plány pro konkrétního klienta).
 * URL pattern /admin/accounts/{clientId}/coach-plans/** — vždy v kontextu klienta.
 */
@Controller
@RequestMapping("/admin/accounts/{clientId}/coach-plans")
public class AdminCoachPlanController {

    private final CoachPlanService coachPlanService;
    private final AccountService accountService;
    private final MarkdownRenderer markdown;

    public AdminCoachPlanController(CoachPlanService coachPlanService,
                                     AccountService accountService,
                                     MarkdownRenderer markdown) {
        this.coachPlanService = coachPlanService;
        this.accountService = accountService;
        this.markdown = markdown;
    }

    @GetMapping
    public String list(@PathVariable Long clientId, Model model) {
        AccountEntity client = accountService.getById(clientId);
        model.addAttribute("client", client);
        model.addAttribute("plans", coachPlanService.listForClient(clientId));
        return "admin/coach-plans/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long clientId, Model model) {
        AccountEntity client = accountService.getById(clientId);
        model.addAttribute("client", client);
        model.addAttribute("today", LocalDate.now());
        return "admin/coach-plans/form";
    }

    @PostMapping
    public String create(@PathVariable Long clientId,
                        @AuthenticationPrincipal AccountEntity author,
                        @RequestParam String title,
                        @RequestParam String bodyMarkdown,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
                        RedirectAttributes flash) {
        coachPlanService.create(author, clientId, title, bodyMarkdown, validFrom, validTo);
        flash.addFlashAttribute("flashSuccess", "Plán vytvořen.");
        return "redirect:/admin/accounts/" + clientId + "/coach-plans";
    }

    @GetMapping("/{planId}/edit")
    public String editForm(@PathVariable Long clientId,
                          @PathVariable Long planId,
                          Model model) {
        AccountEntity client = accountService.getById(clientId);
        CoachPlanEntity plan = coachPlanService.getById(planId);
        if (!plan.getClient().getId().equals(clientId)) {
            throw new NotFoundException("Plán nepatří tomuto klientovi.");
        }
        model.addAttribute("client", client);
        model.addAttribute("plan", plan);
        return "admin/coach-plans/form";
    }

    @PostMapping("/{planId}")
    public String update(@PathVariable Long clientId,
                        @PathVariable Long planId,
                        @RequestParam String title,
                        @RequestParam String bodyMarkdown,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
                        RedirectAttributes flash) {
        coachPlanService.update(planId, title, bodyMarkdown, validFrom, validTo);
        flash.addFlashAttribute("flashSuccess", "Plán aktualizován.");
        return "redirect:/admin/accounts/" + clientId + "/coach-plans";
    }

    @PostMapping("/{planId}/delete")
    public String delete(@PathVariable Long clientId,
                        @PathVariable Long planId,
                        RedirectAttributes flash) {
        coachPlanService.delete(planId);
        flash.addFlashAttribute("flashSuccess", "Plán smazán.");
        return "redirect:/admin/accounts/" + clientId + "/coach-plans";
    }

    @GetMapping("/{planId}")
    public String show(@PathVariable Long clientId,
                      @PathVariable Long planId,
                      Model model) {
        AccountEntity client = accountService.getById(clientId);
        CoachPlanEntity plan = coachPlanService.getById(planId);
        if (!plan.getClient().getId().equals(clientId)) {
            throw new NotFoundException("Plán nepatří tomuto klientovi.");
        }
        model.addAttribute("client", client);
        model.addAttribute("plan", plan);
        model.addAttribute("bodyHtml", markdown.renderToHtml(plan.getBodyMarkdown()));
        return "admin/coach-plans/show";
    }
}
