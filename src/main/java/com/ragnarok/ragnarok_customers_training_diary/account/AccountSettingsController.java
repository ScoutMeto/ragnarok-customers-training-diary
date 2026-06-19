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
        // ScoutMeto kolo 5: profil (pohlaví/věk) + zóny MTF
        model.addAttribute("genders", Gender.values());
        Integer mtf = fresh.getMaxHeartRate();            // efektivní (vlastní ?: vypočtená)
        model.addAttribute("maxHeartRate", mtf);
        model.addAttribute("computedMaxHeartRate", fresh.getComputedMaxHeartRate());  // ScoutMeto kolo 6
        model.addAttribute("cardioZones", mtf != null ? CardioZone.forMaxHeartRate(mtf) : null);
        return "account/settings";
    }

    /** ScoutMeto kolo 5: uložení profilu (pohlaví + datum narození). */
    @PostMapping("/settings/profile")
    @Transactional
    public String updateProfile(
            @AuthenticationPrincipal AccountEntity account,
            @RequestParam(value = "gender", required = false) Gender gender,
            @RequestParam(value = "birthDate", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso =
                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate birthDate,
            @RequestParam(value = "customMaxHr", required = false) Short customMaxHr,
            RedirectAttributes redirectAttributes) {
        AccountEntity fresh = accountRepository.findById(account.getId()).orElseThrow();
        if (birthDate != null && birthDate.isAfter(java.time.LocalDate.now())) {
            redirectAttributes.addFlashAttribute("flashError", "Datum narození nemůže být v budoucnosti.");
            return "redirect:/settings";
        }
        if (customMaxHr != null && (customMaxHr < 100 || customMaxHr > 250)) {
            redirectAttributes.addFlashAttribute("flashError",
                    "Vlastní maximální tepová frekvence musí být v rozmezí 100–250.");
            return "redirect:/settings";
        }
        fresh.setGender(gender);
        fresh.setBirthDate(birthDate);
        fresh.setCustomMaxHr(customMaxHr);  // ScoutMeto kolo 6: vlastní změřená MTF (přebíjí výpočet)
        accountRepository.save(fresh);
        redirectAttributes.addFlashAttribute("flashSuccess", "Profil uložen.");
        return "redirect:/settings";
    }

    /** ScoutMeto kolo 4: přidání vlastní pomůcky přímo z nastavení (jako u tagů). */
    @PostMapping("/settings/equipment")
    public String addEquipment(@AuthenticationPrincipal AccountEntity account,
            @RequestParam("name") String name,
            RedirectAttributes redirectAttributes) {
        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("flashError", "Zadej název náčiní.");
        } else {
            equipmentService.ensureExistsForUser(account, name);
            redirectAttributes.addFlashAttribute("flashSuccess", "Náčiní '" + name.trim() + "' přidáno.");
        }
        return "redirect:/settings";
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
