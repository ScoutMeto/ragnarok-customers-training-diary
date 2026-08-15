package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.tag.TagCategory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/catalog")
public class CatalogPageController {

    private final ExerciseCatalogService catalogService;
    private final CatalogAttributeOptionService attrOptionService;

    public CatalogPageController(ExerciseCatalogService catalogService,
            CatalogAttributeOptionService attrOptionService) {
        this.catalogService = catalogService;
        this.attrOptionService = attrOptionService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AccountEntity user, Model model) {
        model.addAttribute("items", catalogService.listVisibleTo(user));
        model.addAttribute("currentUserId", user.getId());
        addCatalogLabels(model);
        return "catalog/list";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id, Model model) {
        ExerciseCatalogItemEntity item = catalogService.getVisible(user, id);
        model.addAttribute("item", item);
        model.addAttribute("canEdit", canEdit(user, item));
        model.addAttribute("canDelete", canDelete(user, item));
        addCatalogLabels(model);
        return "catalog/detail";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AccountEntity user, Model model) {
        if (!model.containsAttribute("item")) {
            model.addAttribute("item", new ExerciseCatalogItemEntity());
        }
        prepareEnums(model, user);
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
        prepareEnums(model, user);
        return "catalog/form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id,
                         @ModelAttribute("item") ExerciseCatalogItemEntity item,
                         RedirectAttributes flash) {
        try {
            ExerciseCatalogItemEntity updated = catalogService.update(user, id, item);
            if (!updated.getId().equals(id)) {
                flash.addFlashAttribute("flashSuccess", "Systémový cvik uložen jako tvoje kopie.");
            } else {
                flash.addFlashAttribute("flashSuccess", "Cvik upraven.");
            }
            return "redirect:/catalog/" + updated.getId();
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

    private boolean canEdit(AccountEntity user, ExerciseCatalogItemEntity item) {
        if (user.getRole() == com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.ADMIN) {
            return true;
        }
        return item.isSystem() || isOwnCustom(user, item);
    }

    private boolean canDelete(AccountEntity user, ExerciseCatalogItemEntity item) {
        if (user.getRole() == com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.ADMIN) {
            return true;
        }
        return isOwnCustom(user, item);
    }

    private boolean isOwnCustom(AccountEntity user, ExerciseCatalogItemEntity item) {
        return !item.isSystem()
                && item.getCreatedBy() != null
                && item.getCreatedBy().getId().equals(user.getId());
    }

    private void addCatalogLabels(Model model) {
        model.addAttribute("bodyRegionLabels", CatalogLabels.bodyRegionLabels());
        model.addAttribute("movementPatternLabels", CatalogLabels.movementPatternLabels());
        model.addAttribute("tagCategories", TagCategory.values());
    }

    private void prepareEnums(Model model, AccountEntity user) {
        model.addAttribute("bodyRegions",
                attrOptionService.listVisibleTo(user, CatalogAttributeOptionEntity.Kind.BODY_REGION));
        model.addAttribute("movementPatterns",
                attrOptionService.listVisibleTo(user, CatalogAttributeOptionEntity.Kind.MOVEMENT_PATTERN));
        model.addAttribute("equipments",
                attrOptionService.listVisibleTo(user, CatalogAttributeOptionEntity.Kind.EQUIPMENT));
        addCatalogLabels(model);
    }
}