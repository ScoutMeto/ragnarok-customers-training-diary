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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService textPlanService;
    private final com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository accountRepository;

    public AdminGroupTrainingController(
            TrainingService trainingService,
            ExerciseCatalogService catalogService,
            TrainingTagRepository tagRepository,
            com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper typeToInputMapper,
            com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService textPlanService,
            com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository accountRepository) {
        this.trainingService = trainingService;
        this.catalogService = catalogService;
        this.tagRepository = tagRepository;
        this.typeToInputMapper = typeToInputMapper;
        this.textPlanService = textPlanService;
        this.accountRepository = accountRepository;
    }

    /** Velikost stránky admin výpisů (ScoutMeto kolo 7: „20 a 20"). */
    private static final int PAGE_SIZE = 20;

    /** Bezpečné parsování stránkovacího parametru (ruční ?page=abc nesmí skončit 400). */
    static int parsePage(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    @GetMapping
    public String list(@RequestParam(value = "page", defaultValue = "0") String pageParam,
                       @RequestParam(value = "textPage", defaultValue = "0") String textPageParam,
                       Model model) {
        int page = parsePage(pageParam);
        int textPage = parsePage(textPageParam);
        // ScoutMeto kolo 7: stránkované výpisy — 20 naposled vytvořených, šipky vpřed/vzad
        var trainingsPage = trainingService.listGroupTrainingsPaged(page, PAGE_SIZE);
        model.addAttribute("trainings", trainingsPage.getContent());
        model.addAttribute("trainingsPage", trainingsPage);
        // ScoutMeto kolo 6: skupinové textové tréninky (nabídky pro všechny)
        var textOffersPage = textPlanService.listGroupOffersPaged(textPage, PAGE_SIZE);
        model.addAttribute("textOffers", textOffersPage.getContent());
        model.addAttribute("textOffersPage", textOffersPage);
        // review fix: hranice týdenního okna — nabídky publikované dřív jsou „mimo týden"
        model.addAttribute("weekMonday",
                com.ragnarok.ragnarok_customers_training_diary.coach.TextPlanService.currentWeekMonday());
        return "admin/group-trainings/list";
    }

    // -----------------------------------------------------------------------------
    // ScoutMeto kolo 6: skupinový trénink v TEXTOVÉM formátu (nabídka pro všechny)
    // -----------------------------------------------------------------------------

    @GetMapping("/text/new")
    public String newTextForm(Model model) {
        model.addAttribute("editingId", null);
        model.addAttribute("planTitle", "");
        model.addAttribute("planBody", "");
        return "admin/group-trainings/text-form";
    }

    @PostMapping("/text")
    public String createText(@AuthenticationPrincipal AccountEntity admin,
                             @org.springframework.web.bind.annotation.RequestParam("title") String title,
                             @org.springframework.web.bind.annotation.RequestParam("body") String body,
                             @org.springframework.web.bind.annotation.RequestParam(value = "action", defaultValue = "publish") String action,
                             RedirectAttributes flash) {
        try {
            boolean publish = !"draft".equals(action);
            textPlanService.createGroupOffer(admin, title, body, publish);
            flash.addFlashAttribute("flashSuccess", publish
                    ? "Skupinový textový trénink vytvořen — nabízí se všem uživatelům."
                    : "Skupinový textový trénink uložen jako nezveřejněný. Publikuj ho kliknutím na název v seznamu.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/group-trainings/text/new";
        }
        return "redirect:/admin/group-trainings";
    }

    @GetMapping("/text/{id}/edit")
    public String editTextForm(@PathVariable Long id, Model model) {
        var offer = textPlanService.getGroupOffer(id);
        model.addAttribute("editingId", id);
        model.addAttribute("planTitle", offer.getTitle());
        model.addAttribute("planBody", offer.getBody());
        return "admin/group-trainings/text-form";
    }

    @PostMapping("/text/{id}")
    public String updateText(@PathVariable Long id,
                             @org.springframework.web.bind.annotation.RequestParam("title") String title,
                             @org.springframework.web.bind.annotation.RequestParam("body") String body,
                             @org.springframework.web.bind.annotation.RequestParam(value = "action", defaultValue = "publish") String action,
                             RedirectAttributes flash) {
        try {
            boolean publish = !"draft".equals(action);
            textPlanService.updateGroupOffer(id, title, body, publish);
            flash.addFlashAttribute("flashSuccess", publish
                    ? "Skupinový textový trénink upraven."
                    : "Skupinový textový trénink uložen jako nezveřejněný.");
        } catch (IllegalArgumentException | NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    /** ScoutMeto kolo 7: publikace textové nabídky (datum publikace = dnes). */
    @PostMapping("/text/{id}/publish")
    public String publishText(@PathVariable Long id, RedirectAttributes flash) {
        try {
            textPlanService.publishGroupOffer(id);
            flash.addFlashAttribute("flashSuccess", "Textový trénink publikován — uživatelé ho uvidí tento týden.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    /** ScoutMeto kolo 7: přiřazení textového tréninku konkrétnímu uživateli (kopie do Můj plán). */
    @GetMapping("/text/{id}/assign")
    public String assignTextForm(@PathVariable Long id, Model model) {
        model.addAttribute("offer", textPlanService.getGroupOffer(id));
        model.addAttribute("clients", accountRepository
                .findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
                        com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER));
        return "admin/group-trainings/text-assign";
    }

    @PostMapping("/text/{id}/assign")
    public String assignText(@PathVariable Long id,
                             @org.springframework.web.bind.annotation.RequestParam Long clientId,
                             RedirectAttributes flash) {
        try {
            AccountEntity client = accountRepository.findById(clientId)
                    .orElseThrow(() -> new NotFoundException("Klient (id=" + clientId + ") nenalezen."));
            var copy = textPlanService.addGroupOfferToUser(id, client);
            flash.addFlashAttribute("flashSuccess", copy != null
                    ? "Textový trénink přiřazen klientovi " + client.getFirstName() + " " + client.getLastName()
                            + " (kopie v Mém plánu)."
                    : "Klient " + client.getFirstName() + " " + client.getLastName() + " už tento trénink má.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    /** ScoutMeto kolo 7: duplikace textového tréninku (kopie, publikace dneškem). */
    @PostMapping("/text/{id}/duplicate")
    public String duplicateText(@AuthenticationPrincipal AccountEntity admin,
                                @PathVariable Long id, RedirectAttributes flash) {
        try {
            textPlanService.duplicateGroupOffer(id, admin);
            flash.addFlashAttribute("flashSuccess", "Textový trénink zduplikován.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    @PostMapping("/text/{id}/delete")
    public String deleteText(@PathVariable Long id, RedirectAttributes flash) {
        try {
            textPlanService.deleteGroupOffer(id);
            flash.addFlashAttribute("flashSuccess", "Skupinový textový trénink smazán.");
        } catch (NotFoundException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
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
            @org.springframework.web.bind.annotation.RequestParam(value = "action", defaultValue = "publish") String action,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            prepareFormModel(model);
            return "admin/group-trainings/form";
        }

        try {
            // ScoutMeto kolo 7: „Uložit, zatím nezveřejňovat" → draft (klienti nevidí).
            // Publish flag jde do service v jedné transakci (review fix: žádné okno viditelnosti).
            boolean publish = !"draft".equals(action);
            TrainingEntity created = trainingService.createGroup(admin, form, publish);
            if (!publish) {
                flash.addFlashAttribute("flashSuccess",
                        "Skupinový trénink uložen jako nezveřejněný. Publikuj ho kliknutím na název v seznamu.");
                return "redirect:/admin/group-trainings";
            }
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
            @org.springframework.web.bind.annotation.RequestParam(value = "action", defaultValue = "publish") String action,
            Model model,
            RedirectAttributes flash) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editingId", id);
            prepareFormModel(model);
            return "admin/group-trainings/form";
        }

        try {
            // ScoutMeto kolo 7: draft flow i při editaci — publish flag v jedné transakci
            boolean publish = !"draft".equals(action);
            trainingService.updateGroup(id, form, publish);
            if (!publish) {
                flash.addFlashAttribute("flashSuccess", "Skupinový trénink uložen jako nezveřejněný.");
                return "redirect:/admin/group-trainings";
            }
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
    // ScoutMeto kolo 7: publikace / přiřazení / duplikace (klasické skupinové)
    // -----------------------------------------------------------------------------

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, RedirectAttributes flash) {
        try {
            trainingService.setGroupPublished(id, true);
            flash.addFlashAttribute("flashSuccess", "Skupinový trénink publikován — klienti ho uvidí.");
        } catch (NotFoundException | IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    @GetMapping("/{id}/assign")
    public String assignForm(@PathVariable Long id, Model model) {
        TrainingEntity training = trainingService.getAnyTraining(id);
        model.addAttribute("training", training);
        model.addAttribute("clients", accountRepository
                .findByRoleAndDeletedAtIsNullOrderByLastNameAscFirstNameAsc(
                        com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.USER));
        model.addAttribute("defaultDate", java.time.LocalDate.now());
        return "admin/group-trainings/assign";
    }

    @PostMapping("/{id}/assign")
    public String assign(@PathVariable Long id,
                         @org.springframework.web.bind.annotation.RequestParam Long clientId,
                         @org.springframework.web.bind.annotation.RequestParam
                         @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                         java.time.LocalDate trainingDate,
                         RedirectAttributes flash) {
        try {
            AccountEntity client = accountRepository.findById(clientId)
                    .orElseThrow(() -> new NotFoundException("Klient (id=" + clientId + ") nenalezen."));
            trainingService.assignGroupToClient(id, client, trainingDate, typeToInputMapper);
            flash.addFlashAttribute("flashSuccess", "Trénink přiřazen klientovi "
                    + client.getFirstName() + " " + client.getLastName() + " na " + trainingDate + ".");
        } catch (NotFoundException | IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
    }

    @PostMapping("/{id}/duplicate")
    public String duplicate(@AuthenticationPrincipal AccountEntity admin,
                            @PathVariable Long id, RedirectAttributes flash) {
        try {
            trainingService.duplicateGroup(id, admin, typeToInputMapper);
            flash.addFlashAttribute("flashSuccess", "Trénink zduplikován s dnešním datem.");
        } catch (NotFoundException | IllegalArgumentException ex) {
            flash.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/admin/group-trainings";
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
        model.addAttribute("difficultyGroups", TrainingDifficulty.groups());
        model.addAttribute("exerciseTypes", List.of(TrainingExerciseType.values()));
        model.addAttribute("catalogOptions", catalogService.allOptions());
        // Pro group trénink povolíme jen systémové tagy
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

        List<TrainingExerciseInput> exerciseInputs = entity.getExercises().stream().map(ex -> {
            TrainingExerciseInput ei = new TrainingExerciseInput();
            ei.setId(ex.getId());
            ei.setType(ex.getType());
            ei.setCatalogItemId(ex.getCatalogItem() != null ? ex.getCatalogItem().getId() : null);
            ei.setCustomName(ex.getCustomName());
            ei.setRpe(ex.getRpe());
            ei.setNotes(ex.getNotes());
            // kolo 8: dřív se při editaci group tréninku ztrácely tagy + náčiní cviku
            ei.setTagIds(ex.getTags().stream().map(t -> t.getId()).collect(java.util.stream.Collectors.toSet()));
            ei.setEquipmentName(ex.getEquipmentName());
            ei.setEquipmentWeightKg(ex.getEquipmentWeightKg());
            ei.setEquipmentCount(ex.getEquipmentCount());
            ei.setEquipmentSecondWeightKg(ex.getEquipmentSecondWeightKg());
            ei.setSetUnit(ex.getSetUnit());
            List<SetInput> setInputs = ex.getSets().stream().map(s -> {
                SetInput si = new SetInput();
                si.setId(s.getId());
                si.setWeightKg(s.getWeightKg());
                si.setReps(s.getReps());
                si.setRestSeconds(s.getRestSeconds());
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
