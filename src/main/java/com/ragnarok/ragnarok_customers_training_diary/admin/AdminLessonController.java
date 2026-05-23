package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.lesson.GroupLessonPlanEntity;
import com.ragnarok.ragnarok_customers_training_diary.lesson.GroupLessonPlanService;
import com.ragnarok.ragnarok_customers_training_diary.lesson.dto.GroupLessonPlanInput;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin sekce pro správu skupinových lekcí. ROLE_ADMIN vynuceno security configem.
 */
@Controller
@RequestMapping("/admin/lessons")
public class AdminLessonController {

    private final GroupLessonPlanService lessonService;
    private final AccountRepository accountRepository;

    public AdminLessonController(GroupLessonPlanService lessonService, AccountRepository accountRepository) {
        this.lessonService = lessonService;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("lessons", lessonService.listAll());
        return "admin/lessons/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AccountEntity currentAdmin, Model model) {
        if (!model.containsAttribute("form")) {
            GroupLessonPlanInput form = new GroupLessonPlanInput();
            form.setCoachId(currentAdmin.getId()); // default = aktuální admin
            model.addAttribute("form", form);
        }
        addCoaches(model);
        return "admin/lessons/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("form") GroupLessonPlanInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            addCoaches(model);
            return "admin/lessons/form";
        }

        try {
            GroupLessonPlanEntity created = lessonService.create(form);
            flash.addFlashAttribute("flashSuccess", "Lekce vytvořena.");
            return "redirect:/admin/lessons";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            addCoaches(model);
            return "admin/lessons/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        GroupLessonPlanEntity lesson = lessonService.getById(id);
        GroupLessonPlanInput form = new GroupLessonPlanInput();
        form.setId(lesson.getId());
        form.setLessonDate(lesson.getLessonDate());
        form.setStartTime(lesson.getStartTime());
        form.setEndTime(lesson.getEndTime());
        form.setLessonName(lesson.getLessonName());
        form.setCoachId(lesson.getCoach().getId());
        form.setDescription(lesson.getDescription());

        model.addAttribute("form", form);
        model.addAttribute("editing", true);
        addCoaches(model);
        return "admin/lessons/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") GroupLessonPlanInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true);
            addCoaches(model);
            return "admin/lessons/form";
        }

        try {
            lessonService.update(id, form);
            flash.addFlashAttribute("flashSuccess", "Lekce aktualizována.");
            return "redirect:/admin/lessons";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            model.addAttribute("editing", true);
            addCoaches(model);
            return "admin/lessons/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        lessonService.delete(id);
        flash.addFlashAttribute("flashSuccess", "Lekce smazána.");
        return "redirect:/admin/lessons";
    }

    private void addCoaches(Model model) {
        model.addAttribute("coaches",
                accountRepository.findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(AccountRole.ADMIN));
    }
}
