package com.ragnarok.ragnarok_customers_training_diary.benefit;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * ScoutMeto kolo 7: uživatelské akce nad sekcí Výhody na přehledu.
 */
@Controller
public class BenefitPageController {

    private final BenefitService benefitService;
    private final AccountService accountService;

    public BenefitPageController(BenefitService benefitService, AccountService accountService) {
        this.benefitService = benefitService;
        this.accountService = accountService;
    }

    /** Využití výhody s tlačítkem — odešle e-mail (text uživatele je nepovinný). */
    @PostMapping("/benefits/{id}/use")
    public String useBenefit(@AuthenticationPrincipal AccountEntity user,
                             @PathVariable Long id,
                             @RequestParam(value = "userText", required = false) String userText,
                             RedirectAttributes flash) {
        try {
            AccountEntity fresh = accountService.getById(user.getId());
            benefitService.useBenefit(fresh, id, userText);
            flash.addFlashAttribute("flashSuccess",
                    "Požadavek odeslán — brzy se ti ozveme (kopie jde na kontakt výhody).");
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    /**
     * Admin při náhledu uživatele (impersonace) znovu zapne skrytou tabulku Výhod.
     * Dostupné JEN se stopou {@code ROLE_PREVIOUS_ADMINISTRATOR} — běžný USER
     * si viditelnost sám zapnout nemůže.
     */
    @PostMapping("/benefits/restore-visibility")
    public String restoreVisibility(@AuthenticationPrincipal AccountEntity user,
                                    RedirectAttributes flash) {
        boolean impersonatingAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> "ROLE_PREVIOUS_ADMINISTRATOR".equals(a.getAuthority()));
        if (!impersonatingAdmin) {
            flash.addFlashAttribute("flashError", "Tuto akci může provést jen admin.");
            return "redirect:/dashboard";
        }
        AccountEntity fresh = accountService.getById(user.getId());
        fresh.setBenefitsVisible(true);
        accountService.save(fresh);
        flash.addFlashAttribute("flashSuccess", "Tabulka Výhody je pro uživatele znovu viditelná.");
        return "redirect:/dashboard";
    }
}
