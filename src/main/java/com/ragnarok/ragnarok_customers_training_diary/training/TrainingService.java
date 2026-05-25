package com.ragnarok.ragnarok_customers_training_diary.training;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.ExerciseCatalogItemRepository;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.SetInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingExerciseInput;
import com.ragnarok.ragnarok_customers_training_diary.training.dto.TrainingInput;
import com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logika tréninkového deníku.
 *
 * <p>Všechny operace, které vrací nebo upravují trénink, validují vlastnictví —
 * uživatel může číst/editovat/mazat jen svoje vlastní tréninky. (Admin bypass
 * přijde ve Fázi 2.)
 */
@Service
@Transactional
public class TrainingService {

    private final TrainingRepository trainingRepository;
    private final ExerciseCatalogItemRepository catalogRepository;
    private final TrainingTagRepository tagRepository;
    private final ExerciseTypeConfigMapper typeConfigMapper;
    private final com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;

    public TrainingService(
            TrainingRepository trainingRepository,
            ExerciseCatalogItemRepository catalogRepository,
            TrainingTagRepository tagRepository,
            ExerciseTypeConfigMapper typeConfigMapper,
            com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService) {
        this.trainingRepository = trainingRepository;
        this.catalogRepository = catalogRepository;
        this.tagRepository = tagRepository;
        this.typeConfigMapper = typeConfigMapper;
        this.emailService = emailService;
    }

    // =============================================================================
    // Read
    // =============================================================================

    // -----------------------------------------------------------------------------
    // PRIVATE — klient si vede vlastní deník
    // -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TrainingEntity> listMyTrainings(AccountEntity owner) {
        return trainingRepository.findByOwner_IdAndVisibilityOrderByTrainingDateDescIdDesc(
                owner.getId(), TrainingVisibility.PRIVATE);
    }

    @Transactional(readOnly = true)
    public TrainingEntity getMyTraining(AccountEntity owner, Long trainingId) {
        return trainingRepository.findByIdAndOwner_Id(trainingId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
    }

    /**
     * Admin bypass — vrátí trénink bez ohledu na majitele/visibility. Použij <b>pouze</b>
     * v admin sekci, kontrola role musí být zařízena v controlleru/security.
     */
    @Transactional(readOnly = true)
    public TrainingEntity getAnyTraining(Long trainingId) {
        return trainingRepository.findById(trainingId)
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
    }

    /** Admin bypass pro výpis cizích PRIVATE tréninků (admin sekce: detail klienta). */
    @Transactional(readOnly = true)
    public List<TrainingEntity> listTrainingsOf(Long ownerId) {
        return trainingRepository.findByOwner_IdAndVisibilityOrderByTrainingDateDescIdDesc(
                ownerId, TrainingVisibility.PRIVATE);
    }

    public TrainingEntity create(AccountEntity owner, TrainingInput input) {
        validateExerciseNaming(input);

        TrainingEntity training = new TrainingEntity();
        training.setVisibility(TrainingVisibility.PRIVATE);
        training.setOwner(owner);
        training.setCreatedBy(owner);
        applyTrainingFields(training, input);
        applyExercises(training, input.getExercises());
        applyTags(training, input.getTagIds(), owner);

        return trainingRepository.save(training);
    }

    public TrainingEntity update(AccountEntity owner, Long trainingId, TrainingInput input) {
        validateExerciseNaming(input);

        TrainingEntity training = trainingRepository.findByIdAndOwner_Id(trainingId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));

        applyTrainingFields(training, input);
        // Cviky a sety přepíšeme od základu — jednodušší než inkrementální merge.
        training.getExercises().clear();
        applyExercises(training, input.getExercises());
        applyTags(training, input.getTagIds(), owner);

        return trainingRepository.save(training);
    }

    public void delete(AccountEntity owner, Long trainingId) {
        TrainingEntity training = trainingRepository.findByIdAndOwner_Id(trainingId, owner.getId())
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
        trainingRepository.delete(training);
    }

    // -----------------------------------------------------------------------------
    // GROUP — admin tvoří, všichni klienti vidí pro daný den
    // -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TrainingEntity> listGroupTrainingsForDay(LocalDate date) {
        return trainingRepository.findByVisibilityAndTrainingDateOrderByStartTimeAsc(
                TrainingVisibility.GROUP, date);
    }

    @Transactional(readOnly = true)
    public List<TrainingEntity> listAllGroupTrainings() {
        return trainingRepository.findByVisibilityOrderByTrainingDateDescIdDesc(TrainingVisibility.GROUP);
    }

    /**
     * Vytvoří skupinový trénink. {@code creator} musí být admin — vynucení role
     * v controlleru/security.
     */
    public TrainingEntity createGroup(AccountEntity creator, TrainingInput input) {
        validateExerciseNaming(input);

        TrainingEntity training = new TrainingEntity();
        training.setVisibility(TrainingVisibility.GROUP);
        training.setOwner(null);
        training.setCreatedBy(creator);
        applyTrainingFields(training, input);
        applyExercises(training, input.getExercises());
        // Pro group tréninky používáme stejné tagy — viditelné napříč uživateli
        // (omezíme to na systémové tagy v UI, custom tagy patří uživatelům).
        applyTagsForGroup(training, input.getTagIds());

        return trainingRepository.save(training);
    }

    public TrainingEntity updateGroup(Long trainingId, TrainingInput input) {
        validateExerciseNaming(input);

        TrainingEntity training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
        if (training.getVisibility() != TrainingVisibility.GROUP) {
            throw new IllegalArgumentException("Trénink není skupinový.");
        }

        applyTrainingFields(training, input);
        training.getExercises().clear();
        applyExercises(training, input.getExercises());
        applyTagsForGroup(training, input.getTagIds());

        return trainingRepository.save(training);
    }

    public void deleteGroup(Long trainingId) {
        TrainingEntity training = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + trainingId + ") nenalezen."));
        if (training.getVisibility() != TrainingVisibility.GROUP) {
            throw new IllegalArgumentException("Trénink není skupinový.");
        }
        trainingRepository.delete(training);
    }

    // -----------------------------------------------------------------------------
    // TEMPLATE — šablony (Phase 8)
    // -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TrainingEntity> listAllTemplates() {
        return trainingRepository.findByVisibilityOrderByCreatedAtDesc(TrainingVisibility.TEMPLATE);
    }

    @Transactional(readOnly = true)
    public TrainingEntity getTemplate(Long id) {
        TrainingEntity t = trainingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Šablona (id=" + id + ") nenalezena."));
        if (t.getVisibility() != TrainingVisibility.TEMPLATE) {
            throw new IllegalArgumentException("Trénink není šablona.");
        }
        return t;
    }

    public TrainingEntity createTemplate(AccountEntity creator, TrainingInput input) {
        validateExerciseNaming(input);

        TrainingEntity training = new TrainingEntity();
        training.setVisibility(TrainingVisibility.TEMPLATE);
        training.setOwner(null);
        training.setCreatedBy(creator);
        applyTrainingFields(training, input);
        applyExercises(training, input.getExercises());
        applyTagsForGroup(training, input.getTagIds());  // jen systémové tagy

        return trainingRepository.save(training);
    }

    public TrainingEntity updateTemplate(Long templateId, TrainingInput input) {
        validateExerciseNaming(input);
        TrainingEntity training = getTemplate(templateId);

        applyTrainingFields(training, input);
        training.getExercises().clear();
        applyExercises(training, input.getExercises());
        applyTagsForGroup(training, input.getTagIds());

        return trainingRepository.save(training);
    }

    public void deleteTemplate(Long templateId) {
        TrainingEntity training = getTemplate(templateId);
        trainingRepository.delete(training);
    }

    /**
     * Přiřadí šablonu konkrétnímu klientovi na konkrétní datum. Vytvoří se nový
     * PRIVATE trénink jako kopie šablony s {@link TrainingEntity#sourceTemplate}
     * odkazem. Klient si pak může vyplnit svoje výkony do setů.
     *
     * @return nově vytvořený PRIVATE trénink
     */
    public TrainingEntity assignTemplateToClient(Long templateId, AccountEntity client,
                                                  java.time.LocalDate date,
                                                  com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper toInputMapper) {
        TrainingEntity template = getTemplate(templateId);

        TrainingInput input = new TrainingInput();
        input.setTrainingDate(date);
        input.setStartTime(template.getStartTime());
        input.setEndTime(template.getEndTime());
        input.setName(template.getName() != null ? template.getName() : "Trénink od trenéra");
        input.setDifficulty(template.getDifficulty());
        input.setRpe(null); // klient si vyplní vlastní
        input.setNotes(template.getNotes());

        // Tagy: jen systémové (custom tagy patří někomu jinému)
        java.util.Set<Long> systemTagIds = new java.util.HashSet<>();
        for (TrainingTagEntity t : template.getTags()) {
            if (t.isSystem()) systemTagIds.add(t.getId());
        }
        input.setTagIds(systemTagIds);

        // Cviky + per-type configs jako šablona (klient může přepsat při vyplňování)
        for (TrainingExerciseEntity sourceEx : template.getExercises()) {
            TrainingExerciseInput exInput = new TrainingExerciseInput();
            exInput.setType(sourceEx.getType());
            exInput.setCatalogItemId(sourceEx.getCatalogItem() != null ? sourceEx.getCatalogItem().getId() : null);
            exInput.setCustomName(sourceEx.getCustomName());
            exInput.setNotes(sourceEx.getNotes());
            // RPE klient vyplní sám
            for (var s : sourceEx.getSets()) {
                SetInput si = new SetInput();
                si.setWeightKg(s.getWeightKg());
                si.setReps(s.getReps());
                // RPE necháme prázdné — klient si je vyplní
                exInput.getSets().add(si);
            }
            toInputMapper.fillInput(exInput, sourceEx);
            input.getExercises().add(exInput);
        }

        TrainingEntity created = create(client, input);
        created.setSourceTemplate(template);
        TrainingEntity saved = trainingRepository.save(created);

        // Phase 6: notifikace klientovi že má nový tréninkový plán
        String trainerName = template.getCreatedBy() != null
                ? template.getCreatedBy().getFirstName() + " " + template.getCreatedBy().getLastName()
                : null;
        emailService.sendNewPlanAssignedNotification(client, "šablonu tréninku",
                template.getName(), trainerName);

        return saved;
    }

    /**
     * Tréninky daného klienta, které byly přiřazeny z šablony (admin view).
     */
    @Transactional(readOnly = true)
    public List<TrainingEntity> listTrainingsFromTemplate(Long templateId) {
        // JPA derived query: findBy SourceTemplate_Id
        return trainingRepository.findAll().stream()
                .filter(t -> t.getSourceTemplate() != null && t.getSourceTemplate().getId().equals(templateId))
                .toList();
    }

    /**
     * Zkopíruje GROUP trénink do klientova osobního deníku jako nový PRIVATE záznam.
     * Nový trénink dostane stejné cviky, sety, tagy (jen systémové) a per-type configs,
     * ale s owner=klient a visibility=PRIVATE. Klient pak může vyplnit svoje výkony.
     *
     * <p>Implementace: použijeme {@link ExerciseTypeConfigToInputMapper} pro převod
     * entity → input, a pak normální {@code create()} s vlastnickou logikou.
     *
     * @return nově vytvořený PRIVATE trénink
     */
    public TrainingEntity copyGroupToPrivate(AccountEntity owner, Long groupTrainingId,
                                              com.ragnarok.ragnarok_customers_training_diary.training.types.ExerciseTypeConfigToInputMapper toInputMapper) {
        TrainingEntity source = trainingRepository.findById(groupTrainingId)
                .orElseThrow(() -> new NotFoundException("Trénink (id=" + groupTrainingId + ") nenalezen."));
        if (source.getVisibility() != TrainingVisibility.GROUP) {
            throw new IllegalArgumentException("Lze zkopírovat jen skupinový trénink.");
        }

        TrainingInput input = new TrainingInput();
        input.setTrainingDate(source.getTrainingDate());
        input.setStartTime(source.getStartTime());
        input.setEndTime(source.getEndTime());
        input.setName(source.getName() != null ? source.getName() + " (kopie)" : "Kopie skupiny");
        input.setDifficulty(source.getDifficulty());
        input.setRpe(source.getRpe());
        input.setNotes(source.getNotes());
        // Tagy: jen systémové (custom tag patří jinému uživateli)
        java.util.Set<Long> systemTagIds = new java.util.HashSet<>();
        for (TrainingTagEntity t : source.getTags()) {
            if (t.isSystem()) systemTagIds.add(t.getId());
        }
        input.setTagIds(systemTagIds);

        // Cviky + per-type config
        for (TrainingExerciseEntity sourceEx : source.getExercises()) {
            TrainingExerciseInput exInput = new TrainingExerciseInput();
            exInput.setType(sourceEx.getType());
            exInput.setCatalogItemId(sourceEx.getCatalogItem() != null ? sourceEx.getCatalogItem().getId() : null);
            exInput.setCustomName(sourceEx.getCustomName());
            exInput.setRpe(sourceEx.getRpe());
            exInput.setNotes(sourceEx.getNotes());
            // Sety (přenes prázdné jako šablona - klient si je doplní)
            for (var s : sourceEx.getSets()) {
                SetInput si = new SetInput();
                si.setWeightKg(s.getWeightKg());
                si.setReps(s.getReps());
                si.setRpe(s.getRpe());
                si.setNote(s.getNote());
                exInput.getSets().add(si);
            }
            // Per-type config
            toInputMapper.fillInput(exInput, sourceEx);
            input.getExercises().add(exInput);
        }

        return create(owner, input);
    }

    // =============================================================================
    // Privátní helpery
    // =============================================================================

    private void applyTrainingFields(TrainingEntity training, TrainingInput input) {
        training.setTrainingDate(input.getTrainingDate());
        training.setStartTime(input.getStartTime());
        training.setEndTime(input.getEndTime());
        training.setName(input.getName());
        training.setDifficulty(input.getDifficulty());
        training.setRpe(input.getRpe());
        training.setNotes(input.getNotes());
    }

    private void applyExercises(TrainingEntity training, List<TrainingExerciseInput> exerciseInputs) {
        if (exerciseInputs == null) return;

        int orderIdx = 0;
        for (TrainingExerciseInput exInput : exerciseInputs) {
            TrainingExerciseEntity exercise = new TrainingExerciseEntity();
            exercise.setOrderIndex(orderIdx++);
            exercise.setType(exInput.getType());
            exercise.setRpe(exInput.getRpe());
            exercise.setNotes(exInput.getNotes());

            if (exInput.getCatalogItemId() != null) {
                ExerciseCatalogItemEntity catalogItem = catalogRepository.findById(exInput.getCatalogItemId())
                        .orElseThrow(() -> new NotFoundException(
                                "Cvik z katalogu (id=" + exInput.getCatalogItemId() + ") nenalezen."));
                exercise.setCatalogItem(catalogItem);
                exercise.setCustomName(null);
            } else {
                exercise.setCatalogItem(null);
                exercise.setCustomName(exInput.getCustomName());
            }

            int setIdx = 0;
            for (SetInput setInput : exInput.getSets()) {
                if (isSetEmpty(setInput)) continue;
                ExerciseSetEntity set = new ExerciseSetEntity();
                set.setSetIndex(setIdx++);
                set.setWeightKg(setInput.getWeightKg());
                set.setReps(setInput.getReps());
                set.setRpe(setInput.getRpe());
                set.setNote(setInput.getNote());
                exercise.addSet(set);
            }

            // Per-type config (EMOM, Tabata, AMRAP, Circuit, ...). Bezpečné NO-OP pro FREEFORM.
            typeConfigMapper.apply(exercise, exInput);

            training.addExercise(exercise);
        }
    }

    private void applyTags(TrainingEntity training, Set<Long> tagIds, AccountEntity owner) {
        Set<TrainingTagEntity> resolved = new HashSet<>();
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                TrainingTagEntity tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new NotFoundException("Tag (id=" + tagId + ") nenalezen."));
                // Custom tag musí patřit uživateli; system tag je OK pro všechny
                if (!tag.isSystem() && (tag.getOwner() == null
                        || !tag.getOwner().getId().equals(owner.getId()))) {
                    throw new ForbiddenException("Cizí custom tag (id=" + tagId + ").");
                }
                resolved.add(tag);
            }
        }
        training.setTags(resolved);
    }

    /**
     * Tagy pro group trénink — povolíme jen systémové (custom tagy patří uživateli,
     * a group trénink je sdílený přes všechny uživatele).
     */
    private void applyTagsForGroup(TrainingEntity training, Set<Long> tagIds) {
        Set<TrainingTagEntity> resolved = new HashSet<>();
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                TrainingTagEntity tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new NotFoundException("Tag (id=" + tagId + ") nenalezen."));
                if (!tag.isSystem()) {
                    throw new IllegalArgumentException(
                            "Group trénink může mít jen systémové tagy (tag id=" + tagId + " je custom).");
                }
                resolved.add(tag);
            }
        }
        training.setTags(resolved);
    }

    private boolean isSetEmpty(SetInput s) {
        return s.getWeightKg() == null
                && s.getReps() == null
                && s.getRpe() == null
                && (s.getNote() == null || s.getNote().isBlank());
    }

    private void validateExerciseNaming(TrainingInput input) {
        if (input.getExercises() == null) return;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < input.getExercises().size(); i++) {
            TrainingExerciseInput ex = input.getExercises().get(i);
            if (!ex.isNamingValid()) {
                errors.add("Cvik #" + (i + 1) + ": vyber buď cvik z katalogu, nebo zadej vlastní název.");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors));
        }
    }
}
