package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Phase 17: správa katalogu cviků (/catalog). Každý klient vidí systémové cviky
 * + svoje vlastní a může si vytvářet/upravovat/mazat vlastní. Admin spravuje
 * systémové cviky (vidí všichni klienti).
 */
@Controller
@RequestMapping("/catalog")
public class CatalogPageController {

    private final ExerciseCatalogService catalogService;

    public CatalogPageController(ExerciseCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AccountEntity user, Model model) {
        model.addAttribute("items", catalogService.listVisibleTo(user));
        model.addAttribute("currentUserId", user.getId());
        return "catalog/list";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id, Model model) {
        ExerciseCatalogItemEntity item = catalogService.getVisible(user, id);
        model.addAttribute("item", item);
        model.addAttribute("canEdit", canEdit(user, item));
        return "catalog/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("item")) {
            model.addAttribute("item", new ExerciseCatalogItemEntity());
        }
        prepareEnums(model);
        return "catalog/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AccountEntity user,
                         @ModelAttribute("item") ExerciseCatalogItemEntity item,
                         RedirectAttributes flash) {
        if (item.getName() == null || item.getName().isBlank()) {
            flash.addFlashAttribute("flashError", "Název cviku je povinný.");
            return "redirect:/catalog/new";
        }
        ExerciseCatalogItemEntity created = catalogService.create(user, item);
        flash.addFlashAttribute("flashSuccess", "Cvik přidán do katalogu.");
        return "redirect:/catalog/" + created.getId();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id,
                           Model model, RedirectAttributes flash) {
        ExerciseCatalogItemEntity item = catalogService.getVisible(user, id);
        if (!canEdit(user, item)) {
            flash.addFlashAttribute("flashError", "Tento cvik nemůžeš upravovat.");
            return "redirect:/catalog/" + id;
        }
        model.addAttribute("item", item);
        model.addAttribute("editingId", id);
        prepareEnums(model);
        return "catalog/form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id,
                         @ModelAttribute("item") ExerciseCatalogItemEntity item,
                         RedirectAttributes flash) {
        try {
            catalogService.update(user, id, item);
            flash.addFlashAttribute("flashSuccess", "Cvik upraven.");
            return "redirect:/catalog/" + id;
        } catch (ForbiddenException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/catalog/" + id;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id,
                         RedirectAttributes flash) {
        try {
            catalogService.delete(user, id);
            flash.addFlashAttribute("flashSuccess", "Cvik odebrán z katalogu.");
            return "redirect:/catalog";
        } catch (ForbiddenException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/catalog/" + id;
        }
    }

    // -----------------------------------------------------------------------------

    private boolean canEdit(AccountEntity user, ExerciseCatalogItemEntity item) {
        if (user.getRole() == com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.ADMIN) {
            return true;
        }
        return !item.isSystem()
                && item.getCreatedBy() != null
                && item.getCreatedBy().getId().equals(user.getId());
    }

    private void prepareEnums(Model model) {
        model.addAttribute("bodyRegions", BodyRegion.values());
        model.addAttribute("movementPatterns", MovementPattern.values());
        model.addAttribute("equipments", Equipment.values());
    }
}
