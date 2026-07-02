package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.benefit.BenefitItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.benefit.BenefitService;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin gym-wide overview — souhrn aktivit napříč všemi klienty.
 * Data se načítají v UI přes /api/analysis/admin/overview.
 *
 * <p>ScoutMeto kolo 7: navíc editor tabulky „Výhody" (společná pro všechny USER).
 */
@Controller
public class AdminOverviewController {

    private final BenefitService benefitService;

    public AdminOverviewController(BenefitService benefitService) {
        this.benefitService = benefitService;
    }

    @GetMapping("/admin/overview")
    public String overview(Model model) {
        model.addAttribute("benefits", benefitService.listAll());
        return "admin/overview";
    }

    // -----------------------------------------------------------------------------
    // ScoutMeto kolo 7: CRUD výhod
    // -----------------------------------------------------------------------------

    @GetMapping("/admin/overview/benefits/new")
    public String newBenefit(Model model) {
        model.addAttribute("benefit", new BenefitItemEntity());
        model.addAttribute("editing", false);
        return "admin/benefit-form";
    }

    @GetMapping("/admin/overview/benefits/{id}/edit")
    public String editBenefit(@PathVariable Long id, Model model) {
        model.addAttribute("benefit", benefitService.getById(id));
        model.addAttribute("editing", true);
        return "admin/benefit-form";
    }

    @PostMapping("/admin/overview/benefits")
    public String createBenefit(@RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(defaultValue = "TEXT") BenefitItemEntity.HelpType helpType,
                                @RequestParam(required = false) String helpText,
                                @RequestParam(required = false) String buttonLabel,
                                @RequestParam(required = false) String buttonEmail,
                                @RequestParam(required = false) String buttonSubject,
                                @RequestParam(required = false) String buttonPresetText,
                                @RequestParam(defaultValue = "0") int position,
                                RedirectAttributes flash) {
        try {
            BenefitItemEntity item = new BenefitItemEntity();
            applyFields(item, name, description, helpType, helpText,
                    buttonLabel, buttonEmail, buttonSubject, buttonPresetText, position);
            benefitService.save(item);
            flash.addFlashAttribute("flashSuccess", "Výhoda přidána.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/overview";
    }

    @PostMapping("/admin/overview/benefits/{id}")
    public String updateBenefit(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam(required = false) String description,
                                @RequestParam(defaultValue = "TEXT") BenefitItemEntity.HelpType helpType,
                                @RequestParam(required = false) String helpText,
                                @RequestParam(required = false) String buttonLabel,
                                @RequestParam(required = false) String buttonEmail,
                                @RequestParam(required = false) String buttonSubject,
                                @RequestParam(required = false) String buttonPresetText,
                                @RequestParam(defaultValue = "0") int position,
                                RedirectAttributes flash) {
        try {
            BenefitItemEntity item = benefitService.getById(id);
            applyFields(item, name, description, helpType, helpText,
                    buttonLabel, buttonEmail, buttonSubject, buttonPresetText, position);
            benefitService.save(item);
            flash.addFlashAttribute("flashSuccess", "Výhoda upravena.");
        } catch (IllegalArgumentException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/overview";
    }

    @PostMapping("/admin/overview/benefits/{id}/delete")
    public String deleteBenefit(@PathVariable Long id, RedirectAttributes flash) {
        try {
            benefitService.delete(id);
            flash.addFlashAttribute("flashSuccess", "Výhoda smazána.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/overview";
    }

    private void applyFields(BenefitItemEntity item, String name, String description,
                             BenefitItemEntity.HelpType helpType, String helpText,
                             String buttonLabel, String buttonEmail, String buttonSubject,
                             String buttonPresetText, int position) {
        item.setName(name != null ? name.trim() : null);
        item.setDescription(description);
        item.setHelpType(helpType);
        item.setHelpText(helpText);
        item.setButtonLabel(buttonLabel);
        item.setButtonEmail(buttonEmail);
        item.setButtonSubject(buttonSubject);
        item.setButtonPresetText(buttonPresetText);
        item.setPosition(position);
    }
}
