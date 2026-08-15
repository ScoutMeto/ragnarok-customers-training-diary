package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogService;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingDifficulty;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseType;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin sekce pro šablony tréninků. Šablona = trénink s visibility=TEMPLATE.
 *
 * <ul>
 *     <li>{@code GET /admin/training-templates}        list všech šablon</li>
 *     <li>{@code GET /admin/training-templates/new}    formulář nový</li>
 *     <li>{@code POST /admin/training-templates}       create</li>
 *     <li>{@code GET /admin/training-templates/{id}/edit}  formulář edit</li>
 *     <li>{@code POST /admin/training-templates/{id}}  update</li>
 *     <li>{@code POST /admin/training-templates/{id}/delete}</li>
 *     <li>{@code GET /admin/training-templates/{id}/assign}  formulář přiřazení klientovi</li>
 *     <li>{@code POST /admin/training-templates/{id}/assign} POST přiřazení</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin/training-templates")
public class AdminTrainingTemplateController {

    private final TrainingService trainingService;
    private final ExerciseCatalogService catalogService;
    private final TrainingTagRepository tagRepository;
    private final AccountRepository accountRepository;
    private final ExerciseTypeConfigToInputMapper typeToInputMapper;
    private final com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService textPlanService;

    /** Velikost stránky admin výpisů (ScoutMeto kolo 7: „20 a 20"). */
    private static final int PAGE_SIZE = 20;

    public AdminTrainingTemplateController(TrainingService trainingService,
                                            ExerciseCatalogService catalogService,
                                            TrainingTagRepository tagRepository,
                                            AccountRepository accountRepository,
                                            ExerciseTypeConfigToInputMapper typeToInputMapper,
                                            com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService textPlanService) {
        this.trainingService = trainingService;
        this.catalogService = catalogService;
        this.tagRepository = tagRepository;
        this.accountRepository = accountRepository;
        this.typeToInputMapper = typeToInputMapper;
        this.textPlanService = textPlanService;
    }

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") String pageParam,
                       @RequestParam(value = "textPage", defaultValue = "0") String textPageParam,
                       Model model) {
        // review fix: bezpečné parsování (?page=abc nesmí skončit 400)
        int page = AdminGroupTrainingController.parsePage(pageParam);
        int textPage = AdminGroupTrainingController.parsePage(textPageParam);
        // ScoutMeto kolo 7: stránkované výpisy (20 naposled vytvořených) + textové šablony inline
        var templatesPage = trainingService.listTemplatesPaged(page, PAGE_SIZE);
        model.addAttribute("templates", templatesPage.getContent());
        model.addAttribute("templatesPage", templatesPage);
        var textTemplatesPage = textPlanService.listTemplatesPaged(textPage, PAGE_SIZE);
        model.addAttribute("textTemplates", textTemplatesPage.getContent());
        model.addAttribute("textTemplatesPage", textTemplatesPage);
        return "admin/templates/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            TrainingInput form = new TrainingInput();
            form.setTrainingDate(LocalDate.now());  // jen formální (templ se nezobrazuje v deníku)
            TrainingExerciseInput first = new TrainingExerciseInput();
            first.getSets().add(new SetInput());
            first.getSets().add(new SetInput());
            first.getSets().add(new SetInput());
            form.getExercises().add(first);
            model.addAttribute("form", form);
        }
        prepareFormModel(model);
        return "admin/templates/form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal AccountEntity admin,
                        @Valid @ModelAttribute("form") TrainingInput form,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model);
            return "admin/templates/form";
        }
        try {
            TrainingEntity created = trainingService.createTemplate(admin, form);
            flash.addFlashAttribute("flashSuccess", "Šablona vytvořena.");
            return "redirect:/admin/training-templates/" + created.getId() + "/assign";
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            prepareFormModel(model);
            return "admin/templates/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TrainingEntity tpl = trainingService.getTemplate(id);
        TrainingInput form = toInput(tpl);
        model.addAttribute("form", form);
        model.addAttribute("editingId", id);
        prepareFormModel(model);
        return "admin/templates/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("form") TrainingInput form,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes flash) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editingId", id);
            prepareFormModel(model);
            return "admin/templates/form";
        }
        try {
            trainingService.updateTemplate(id, form);
            flash.addFlashAttribute("flashSuccess", "Šablona aktualizována.");
            return "redirect:/admin/training-templates";
        } catch (IllegalArgumentException | NotFoundException ex) {
            bindingResult.reject("form", ex.getMessage());
            model.addAttribute("editingId", id);
            prepareFormModel(model);
            return "admin/templates/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            trainingService.deleteTemplate(id);
            flash.addFlashAttribute("flashSuccess", "Šablona smazána.");
        } catch (NotFoundException | IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/training-templates";
    }

    // -----------------------------------------------------------------------------
    // Assign
    // -----------------------------------------------------------------------------

    @GetMapping("/{id}/assign")
    public String assignForm(@PathVariable Long id, Model model) {
        TrainingEntity tpl = trainingService.getTemplate(id);
        model.addAttribute("template", tpl);
        model.addAttribute("clients",
                accountRepository.findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(AccountRole.USER));
        model.addAttribute("defaultDate", LocalDate.now());
        return "admin/templates/assign";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id,
                         @RequestParam Long clientId,
                         @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate trainingDate,
                         RedirectAttributes flash) {
        AccountEntity client = accountRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Klient (id=" + clientId + ") nenalezen."));

        TrainingEntity created = trainingService.assignTemplateToClient(id, client, trainingDate, typeToInputMapper);
        flash.addFlashAttribute("flashSuccess",
                "Šablona přiřazena klientovi " + client.getFirstName() + " " + client.getLastName()
                        + " na " + trainingDate + ".");
        return "redirect:/diary/" + created.getId();
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------

    private void prepareFormModel(Model model) {
        model.addAttribute("difficultyGroups", TrainingDifficulty.groups());
        model.addAttribute("exerciseTypes", List.of(TrainingExerciseType.values()));
        model.addAttribute("catalogOptions", catalogService.allOptions());
        model.addAttribute("tags", tagRepository.findByIsSystemTrueOrderByNameAsc());
        model.addAttribute("tagCategories", com.ragnarok.ragnarok_customers_training_diary.tag.TagCategory.values());
    }

    private TrainingInput toInput(TrainingEntity entity) {
        TrainingInput input = new TrainingInput();
        input.setId(entity.getId());
        input.setTrainingDate(entity.getTrainingDate());
        input.setStartTime(entity.getStartTime());
        input.setEndTime(entity.getEndTime());
        input.setName(entity.getName());
        input.setDifficulty(entity.getDifficulty());
        input.setRpe(entity.getRpe());
        input.setNotes(entity.getNotes());
        input.setTagIds(entity.getTags().stream().map(t -> t.getId()).collect(Collectors.toSet()));

        List<TrainingExerciseInput> exercises = entity.getExercises().stream().map(ex -> {
            TrainingExerciseInput ei = new TrainingExerciseInput();
            ei.setId(ex.getId());
            ei.setType(ex.getType());
            ei.setCatalogItemId(ex.getCatalogItem() != null ? ex.getCatalogItem().getId() : null);
            ei.setCustomName(ex.getCustomName());
            ei.setRpe(ex.getRpe());
            ei.setNotes(ex.getNotes());
            ei.setTagIds(ex.getTags().stream().map(t -> t.getId()).collect(Collectors.toSet()));
            ei.setEquipmentName(ex.getEquipmentName());
            ei.setEquipmentWeightKg(ex.getEquipmentWeightKg());
            ei.setEquipmentCount(ex.getEquipmentCount());
            ei.setEquipmentSecondWeightKg(ex.getEquipmentSecondWeightKg());
            // review fix (kolo 8): bez toho by edit šablony ztratil jednotku Carry a odpočinky setů
            ei.setSetUnit(ex.getSetUnit());
            List<SetInput> sets = ex.getSets().stream().map(s -> {
                SetInput si = new SetInput();
                si.setId(s.getId());
                si.setWeightKg(s.getWeightKg());
                si.setReps(s.getReps());
                si.setRestSeconds(s.getRestSeconds());
                si.setRpe(s.getRpe());
                si.setNote(s.getNote());
                return si;
            }).collect(Collectors.toList());
            ei.setSets(sets);
            typeToInputMapper.fillInput(ei, ex);
            return ei;
        }).collect(Collectors.toList());
        input.setExercises(exercises);
        return input;
    }
}
