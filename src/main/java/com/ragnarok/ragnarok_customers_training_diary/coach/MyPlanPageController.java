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

    public MyPlanPageController(CoachPlanService coachPlanService, MarkdownRenderer markdown) {
        this.coachPlanService = coachPlanService;
        this.markdown = markdown;
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
}
