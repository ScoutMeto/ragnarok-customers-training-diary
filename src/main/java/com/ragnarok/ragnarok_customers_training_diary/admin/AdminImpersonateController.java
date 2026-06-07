package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Phase 20c: výběr uživatele pro „Deník uživatele" / „Statistiky uživatele".
 * Po výběru se admin přepne (impersonace přes SwitchUserFilter na /admin/impersonate)
 * a uvidí deník/statistiky daného uživatele s plnými právy.
 */
@Controller
public class AdminImpersonateController {

    private final AccountRepository accountRepository;

    public AdminImpersonateController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/admin/impersonate-select")
    public String selectUser(@RequestParam(value = "next", defaultValue = "diary") String next, Model model) {
        model.addAttribute("clients",
                accountRepository.findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(AccountRole.USER));
        model.addAttribute("next", "analysis".equals(next) ? "analysis" : "diary");
        return "admin/impersonate-select";
    }
}
