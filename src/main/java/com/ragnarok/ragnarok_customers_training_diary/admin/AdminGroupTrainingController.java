package com.ragnarok.ragnarok_customers_training_diary.admin;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
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
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin sekce pro skupinové tréninky. Admin vytvoří trénink (cviky + sety) na konkrétní
 * den; všichni klienti ho uvidí v deníku / na dashboardu pro daný den.
 *
 * <p>Formulář je sdílen s klientským tvořením (viz {@code diary/form.html} v
 * {@link com.ragnarok.ragnarok_customers_training_diary.training.DiaryPageController})
 * — to admin jen uloží jako visibility=GROUP.
 *
 * <p>ROLE_ADMIN vynuceno security configem na URL pattern {@code /admin/**}.
 */
@Controller
@RequestMapping("/admin/group-trainings")
public class AdminGroupTrainingController {

    private final TrainingService trainingService;
    private final ExerciseCatalogService catalogService;
    private final TrainingTagRepository tagRepository;
    private final com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper typeToInputMapper;

    public AdminGroupTrainingController(
            TrainingService trainingService,
            ExerciseCatalogService catalogService,
            TrainingTagRepository tagRepository,
            com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper typeToInputMapper) {
        this.trainingService = trainingService;
        this.catalogService = catalogService;
        this.tagRepository = tagRepository;
        this.typeToInputMapper = typeToInputMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("trainings", trainingService.listAllGroupTrainings());
        return "admin/group-trainings/list";
    }

    // -----------------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            TrainingInput form = new TrainingInput();
            // Defaultně jeden cvik + 3 sety pro pohodlí
            TrainingExerciseInput firstExercise = new TrainingExerciseInput();
            firstExercise.getSets().add(new SetInput());
            firstExercise.getSets().add(new SetInput());
            firstExercise.getSets().add(new SetInput());
            form.getExercises().add(firstExercise);
            model.addAttribute("form", form);
        }
        prepareFormModel(model);
        return "admin/group-trainings/form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal AccountEntity admin,
            @Valid @ModelAttribute("form") TrainingInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model);
            return "admin/group-trainings/form";
        }

        try {
            TrainingEntity created = trainingService.createGroup(admin, form);
            flash.addFlashAttribute("flashSuccess", "Skupinový trénink vytvořen.");
            return "redirect:/diary/" + created.getId();  // detail je sdílený s běžným diary view
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            prepareFormModel(model);
            return "admin/group-trainings/form";
        }
    }

    // -----------------------------------------------------------------------------
    // Edit
    // -----------------------------------------------------------------------------

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TrainingEntity training = trainingService.getAnyTraining(id);
        TrainingInput form = toInput(training);
        model.addAttribute("form", form);
        model.addAttribute("editingId", id);
        prepareFormModel(model);
        return "admin/group-trainings/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("form") TrainingInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingId", id);
            prepareFormModel(model);
            return "admin/group-trainings/form";
        }

        try {
            trainingService.updateGroup(id, form);
            flash.addFlashAttribute("flashSuccess", "Skupinový trénink aktualizován.");
            return "redirect:/diary/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            model.addAttribute("editingId", id);
            prepareFormModel(model);
            return "admin/group-trainings/form";
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/group-trainings";
        }
    }

    // -----------------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        try {
            trainingService.deleteGroup(id);
            flash.addFlashAttribute("flashSuccess", "Skupinový trénink smazán.");
        } catch (NotFoundException | IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    // -----------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------

    private void prepareFormModel(Model model) {
        model.addAttribute("difficulties", TrainingDifficulty.values());
        model.addAttribute("exerciseTypes", List.of(TrainingExerciseType.values()));
        model.addAttribute("catalogItems", catalogService.listAll());
        // Pro group trénink povolíme jen systémové tagy
        model.addAttribute("tags", tagRepository.findByIsSystemTrueOrderByNameAsc());
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

        List<TrainingExerciseInput> exerciseInputs = entity.getExercises().stream().map(ex -> {
            TrainingExerciseInput ei = new TrainingExerciseInput();
            ei.setId(ex.getId());
            ei.setType(ex.getType());
            ei.setCatalogItemId(ex.getCatalogItem() != null ? ex.getCatalogItem().getId() : null);
            ei.setCustomName(ex.getCustomName());
            ei.setRpe(ex.getRpe());
            ei.setNotes(ex.getNotes());
            List<SetInput> setInputs = ex.getSets().stream().map(s -> {
                SetInput si = new SetInput();
                si.setId(s.getId());
                si.setWeightKg(s.getWeightKg());
                si.setReps(s.getReps());
                si.setRpe(s.getRpe());
                si.setNote(s.getNote());
                return si;
            }).collect(Collectors.toList());
            ei.setSets(setInputs);
            typeToInputMapper.fillInput(ei, ex);
            return ei;
        }).collect(Collectors.toList());
        input.setExercises(exerciseInputs);

        return input;
    }
}
