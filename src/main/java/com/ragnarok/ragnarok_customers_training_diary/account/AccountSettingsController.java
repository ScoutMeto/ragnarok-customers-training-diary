package com.ragnarok.ragnarok_customers_training_diary.account;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Stránka /settings — uživatel si nastaví notifikační preference a může se odhlásit
 * z jednotlivých typů emailů. Vyžaduje přihlášení (security default).
 */
@Controller
public class AccountSettingsController {

    private final AccountRepository accountRepository;
    private final com.ragnarok.ragnarok_customers_training_diary.equipment.EquipmentOptionService equipmentService;

    public AccountSettingsController(AccountRepository accountRepository,
            com.ragnarok.ragnarok_customers_training_diary.equipment.EquipmentOptionService equipmentService) {
        this.accountRepository = accountRepository;
        this.equipmentService = equipmentService;
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal AccountEntity account, Model model) {
        // Načti čerstvý stav z DB (principal v session může být stale)
        AccountEntity fresh = accountRepository.findById(account.getId()).orElseThrow();
        model.addAttribute("account", fresh);
        // Phase 19e: jen vlastní (custom) pomůcky — systémové smazat nelze
        model.addAttribute("myEquipment", equipmentService.listVisibleTo(fresh).stream()
                .filter(e -> !e.isSystem()).toList());
        return "account/settings";
    }

    /** Phase 19e: smazání vlastní pomůcky z nastavení. */
    @PostMapping("/settings/equipment/{id}/delete")
    public String deleteEquipment(@AuthenticationPrincipal AccountEntity account,
            @org.springframework.web.bind.annotation.PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            equipmentService.deleteOwn(account, id);
            redirectAttributes.addFlashAttribute("flashSuccess", "Pomůcka smazána.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/notifications")
    @Transactional
    public String updateNotifications(
            @AuthenticationPrincipal AccountEntity account,
            @RequestParam(value = "notifNewPlanAssigned", required = false) Boolean planAssigned,
            @RequestParam(value = "notifNewComment", required = false) Boolean newComment,
            @RequestParam(value = "notifTrainingCreated", required = false) Boolean trainingCreated,
            @RequestParam(value = "notifAccountDeactivated", required = false) Boolean accountDeactivated,
            RedirectAttributes redirectAttributes) {

        // HTML checkbox nezaslal = false. Nullable mapping → defaultně false.
        AccountEntity fresh = accountRepository.findById(account.getId()).orElseThrow();
        fresh.setNotifNewPlanAssigned(Boolean.TRUE.equals(planAssigned));
        fresh.setNotifNewComment(Boolean.TRUE.equals(newComment));
        fresh.setNotifTrainingCreated(Boolean.TRUE.equals(trainingCreated));
        fresh.setNotifAccountDeactivated(Boolean.TRUE.equals(accountDeactivated));
        accountRepository.save(fresh);

        redirectAttributes.addFlashAttribute("flashSuccess", "Nastavení notifikací uloženo.");
        return "redirect:/settings";
    }
}
