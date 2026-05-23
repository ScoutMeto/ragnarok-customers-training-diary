package com.ragnarok.ragnarok_customers_training_diary.training;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogService;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf controller pro tréninkový deník (/diary/**).
 *
 * <p>Stránky:
 * <ul>
 *     <li>{@code GET /diary} — seznam mých tréninků</li>
 *     <li>{@code GET /diary/new} — formulář nový</li>
 *     <li>{@code POST /diary} — uložit nový</li>
 *     <li>{@code GET /diary/{id}} — detail (read-only zobrazení)</li>
 *     <li>{@code GET /diary/{id}/edit} — formulář editace</li>
 *     <li>{@code POST /diary/{id}} — uložit edit</li>
 *     <li>{@code POST /diary/{id}/delete} — smazat</li>
 * </ul>
 */
@Controller
@RequestMapping("/diary")
public class DiaryPageController {

    private final TrainingService trainingService;
    private final ExerciseCatalogService catalogService;
    private final TrainingTagService tagService;
    private final TrainingCommentService commentService;
    private final com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper typeToInputMapper;

    public DiaryPageController(
            TrainingService trainingService,
            ExerciseCatalogService catalogService,
            TrainingTagService tagService,
            TrainingCommentService commentService,
            com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper typeToInputMapper) {
        this.trainingService = trainingService;
        this.catalogService = catalogService;
        this.tagService = tagService;
        this.commentService = commentService;
        this.typeToInputMapper = typeToInputMapper;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal AccountEntity user, Model model) {
        List<TrainingEntity> trainings = trainingService.listMyTrainings(user);
        model.addAttribute("trainings", trainings);
        // Dnešní skupinové tréninky od trenéra (viditelné všem klientům)
        model.addAttribute("todayGroupTrainings",
                trainingService.listGroupTrainingsForDay(java.time.LocalDate.now()));
        return "diary/list";
    }

    @GetMapping("/{id}")
    public String detail(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            Model model) {
        TrainingEntity training = loadTrainingForViewer(user, id);

        boolean isGroup = training.getVisibility() == TrainingVisibility.GROUP;
        boolean isOwner = !isGroup
                && training.getOwner() != null
                && training.getOwner().getId().equals(user.getId());

        model.addAttribute("training", training);
        model.addAttribute("comments", commentService.listForTraining(id));
        model.addAttribute("currentUser", user);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isGroup", isGroup);
        return "diary/detail";
    }

    /**
     * Načte trénink podle role + typu:
     *  - GROUP → kdokoliv (read-only pro klienta, admin může přes /admin/group-trainings editovat)
     *  - PRIVATE + user je ADMIN → getAnyTraining (admin bypass)
     *  - PRIVATE + user je USER → getMyTraining (ownership enforced)
     */
    private TrainingEntity loadTrainingForViewer(AccountEntity user, Long trainingId) {
        TrainingEntity training = trainingService.getAnyTraining(trainingId);

        if (training.getVisibility() == TrainingVisibility.GROUP) {
            return training; // viditelný všem
        }
        // PRIVATE
        if (user.getRole() == AccountRole.ADMIN) {
            return training;
        }
        if (training.getOwner() == null || !training.getOwner().getId().equals(user.getId())) {
            throw new NotFoundException("Trénink (id=" + trainingId + ") nenalezen.");
        }
        return training;
    }

    // -----------------------------------------------------------------------------
    // Create / Edit form rendering + handlers — implementuje commit 6
    // (placeholder metody, ať list/detail funguje hned)
    // -----------------------------------------------------------------------------

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal AccountEntity user, Model model) {
        TrainingInput form = new TrainingInput();
        // Rovnou jeden prázdný cvik a tři prázdné sety — jednodušší UX
        TrainingExerciseInput firstExercise = new TrainingExerciseInput();
        firstExercise.getSets().add(new SetInput());
        firstExercise.getSets().add(new SetInput());
        firstExercise.getSets().add(new SetInput());
        form.getExercises().add(firstExercise);

        prepareFormModel(model, form, user);
        return "diary/form";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal AccountEntity user,
            @Valid @ModelAttribute("form") TrainingInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, user);
            return "diary/form";
        }

        try {
            TrainingEntity saved = trainingService.create(user, form);
            flash.addFlashAttribute("flashSuccess", "Trénink uložen.");
            return "redirect:/diary/" + saved.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            prepareFormModel(model, form, user);
            return "diary/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            Model model) {
        TrainingEntity training = trainingService.getMyTraining(user, id);
        TrainingInput form = toInput(training);
        prepareFormModel(model, form, user);
        model.addAttribute("editingId", id);
        return "diary/form";
    }

    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            @Valid @ModelAttribute("form") TrainingInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, user);
            model.addAttribute("editingId", id);
            return "diary/form";
        }

        try {
            trainingService.update(user, id, form);
            flash.addFlashAttribute("flashSuccess", "Trénink aktualizován.");
            return "redirect:/diary/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("form", ex.getMessage());
            prepareFormModel(model, form, user);
            model.addAttribute("editingId", id);
            return "diary/form";
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/diary";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            RedirectAttributes flash) {
        try {
            trainingService.delete(user, id);
            flash.addFlashAttribute("flashSuccess", "Trénink smazán.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/diary";
    }

    // -----------------------------------------------------------------------------
    // Komentáře
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/comments")
    public String addComment(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            @RequestParam("text") String text,
            RedirectAttributes flash) {
        try {
            commentService.addComment(user, id, text);
            flash.addFlashAttribute("flashSuccess", "Komentář přidán.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        } catch (ForbiddenException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/diary/" + id;
    }

    @PostMapping("/{id}/comments/{commentId}/delete")
    public String deleteComment(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id,
            @PathVariable Long commentId,
            RedirectAttributes flash) {
        try {
            commentService.deleteComment(user, commentId);
            flash.addFlashAttribute("flashSuccess", "Komentář smazán.");
        } catch (ForbiddenException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/diary/" + id;
    }

    // -----------------------------------------------------------------------------
    // Privátní helpery
    // -----------------------------------------------------------------------------

    private void prepareFormModel(Model model, TrainingInput form, AccountEntity user) {
        model.addAttribute("form", form);
        model.addAttribute("difficulties", TrainingDifficulty.values());
        model.addAttribute("exerciseTypes", java.util.List.of(TrainingExerciseType.values()));
        model.addAttribute("catalogItems", catalogService.listAll());
        model.addAttribute("tags", tagService.findVisibleTo(user));
    }

    /**
     * Naplní form z existující entity (pro edit).
     */
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
            // Per-type config (EMOM, Tabata, AMRAP, ...) z entity zpět do DTO
            typeToInputMapper.fillInput(ei, ex);
            return ei;
        }).collect(Collectors.toList());
        input.setExercises(exerciseInputs);

        return input;
    }
}
