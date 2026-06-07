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

    public MyPlanPageController(CoachPlanService coachPlanService, MarkdownRenderer markdown,
            TextPlanService textPlanService) {
        this.coachPlanService = coachPlanService;
        this.markdown = markdown;
        this.textPlanService = textPlanService;
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
}
