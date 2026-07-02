package com.ragnarok.ragnarok_customers_training_diary.coach;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Klientův pohled na svoje coach plány.
 *
 * <ul>
 *     <li>{@code GET /my-plan} — aktuálně platný plán + odkaz na historii</li>
 *     <li>{@code GET /my-plan/history} — všechny moje plány</li>
 *     <li>{@code GET /my-plan/{id}} — konkrétní plán (s validací ownership)</li>
 * </ul>
 */
@Controller
public class MyPlanPageController {

    private final CoachPlanService coachPlanService;
    private final MarkdownRenderer markdown;
    private final TextPlanService textPlanService;
    private final com.ragnarok.ragnarok_customers_training_diary.account.AccountService accountService;

    public MyPlanPageController(CoachPlanService coachPlanService, MarkdownRenderer markdown,
            TextPlanService textPlanService,
            com.ragnarok.ragnarok_customers_training_diary.account.AccountService accountService) {
        this.coachPlanService = coachPlanService;
        this.markdown = markdown;
        this.textPlanService = textPlanService;
        this.accountService = accountService;
    }

    @GetMapping("/my-plan")
    public String myActivePlan(@AuthenticationPrincipal AccountEntity user, Model model) {
        var active = coachPlanService.findActiveForClient(user.getId(), LocalDate.now());
        var all = coachPlanService.listForClient(user.getId());
        model.addAttribute("activePlan", active.orElse(null));
        model.addAttribute("allPlans", all);
        if (active.isPresent()) {
            model.addAttribute("activeBodyHtml", markdown.renderToHtml(active.get().getBodyMarkdown()));
        }
        // Phase 21: textové plány (editovatelné kopie od trenéra)
        model.addAttribute("textPlans", textPlanService.listForUser(user.getId()));
        return "coach/my-plan";
    }

    @GetMapping("/my-plan/{id}")
    public String showPlan(@AuthenticationPrincipal AccountEntity user,
                          @PathVariable Long id,
                          Model model) {
        CoachPlanEntity plan = coachPlanService.getForClientOrAdmin(user, id);
        model.addAttribute("plan", plan);
        model.addAttribute("bodyHtml", markdown.renderToHtml(plan.getBodyMarkdown()));
        return "coach/plan-detail";
    }

    // ----- Phase 21: textový plán (view + edit vlastní kopie) -----

    @GetMapping("/my-plan/text/{id}")
    public String showTextPlan(@AuthenticationPrincipal AccountEntity user,
                               @PathVariable Long id, Model model) {
        model.addAttribute("plan", textPlanService.getForUser(user, id));
        return "coach/text-plan-detail";
    }

    @org.springframework.web.bind.annotation.PostMapping("/my-plan/text/{id}")
    public String saveTextPlan(@AuthenticationPrincipal AccountEntity user,
                               @PathVariable Long id,
                               @org.springframework.web.bind.annotation.RequestParam("title") String title,
                               @org.springframework.web.bind.annotation.RequestParam("body") String body,
                               org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            textPlanService.updateOwnCopy(user, id, title, body);
            flash.addFlashAttribute("flashSuccess", "Plán uložen.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/my-plan/text/" + id;
    }

    /** ScoutMeto kolo 7: smazání vlastní kopie textového plánu. */
    @org.springframework.web.bind.annotation.PostMapping("/my-plan/text/{id}/delete")
    public String deleteTextPlan(@AuthenticationPrincipal AccountEntity user,
                                 @PathVariable Long id,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        try {
            textPlanService.deleteOwnCopy(user, id);
            flash.addFlashAttribute("flashSuccess", "Textový plán smazán.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/my-plan";
    }

    // ----- ScoutMeto kolo 6: přidání skupinové textové nabídky k sobě (→ kopie v Můj plán) -----

    @org.springframework.web.bind.annotation.PostMapping("/my-plan/text-offers/{id}/add")
    public String addGroupOffer(@AuthenticationPrincipal AccountEntity user,
                                @PathVariable Long id,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        // Read-only (deaktivovaný) účet nesmí nic přidávat — konzistentní s gating v deníku (E1).
        if (accountService.isDeactivated(user.getId())) {
            flash.addFlashAttribute("flashError",
                    "Tvůj účet je neaktivní (jen náhled). Pro obnovení funkcí kontaktuj trenéra.");
            return "redirect:/my-plan";
        }
        // ScoutMeto kolo 7: uživatel smí přidat jen nabídku aktuálně viditelnou
        // (publikovaná v tomto týdnu) — drafty a minulé týdny jen přes admin přiřazení.
        boolean visible = textPlanService.listVisibleGroupOffers().stream()
                .anyMatch(o -> o.getId().equals(id));
        if (!visible) {
            flash.addFlashAttribute("flashError", "Tato nabídka už není aktuální.");
            return "redirect:/diary";
        }
        try {
            var copy = textPlanService.addGroupOfferToUser(id, user);
            flash.addFlashAttribute("flashSuccess", copy != null
                    ? "Trénink přidán do Mého plánu — můžeš si ho upravit."
                    : "Tento trénink už v Mém plánu máš.");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/my-plan";
    }
}
