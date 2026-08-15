package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionEntity.Kind;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.common.TextNormalizer;
import com.ragnarok.ragnarok_customers_training_diary.tag.TagCategory;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogAttributeOptionService {

    private final CatalogAttributeOptionRepository repository;
    private final TrainingTagRepository tagRepository;

    public CatalogAttributeOptionService(CatalogAttributeOptionRepository repository,
            TrainingTagRepository tagRepository) {
        this.repository = repository;
        this.tagRepository = tagRepository;
    }

    public List<CatalogAttributeOptionEntity> list(Kind kind) {
        return repository.findByKindOrderByIsSystemDescNameAsc(kind).stream()
                .sorted(optionComparator())
                .toList();
    }

    public List<CatalogAttributeOptionEntity> listVisibleTo(AccountEntity user, Kind kind) {
        return repository.findVisibleTo(kind, user.getId()).stream()
                .sorted(optionComparator())
                .toList();
    }

    @Transactional
    public OptionCreateResult addCustom(AccountEntity owner, Kind kind, String name, Boolean createTag) {
        if (kind == null) {
            throw new IllegalArgumentException("Typ hodnoty je povinný.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Název nesmí být prázdný.");
        }
        String trimmed = name.trim();
        Optional<CatalogAttributeOptionEntity> duplicate = findVisibleDuplicate(owner, kind, trimmed);
        if (duplicate.isPresent()) {
            return new OptionCreateResult(duplicate.get(), false, true, false, false);
        }

        CatalogAttributeOptionEntity option = new CatalogAttributeOptionEntity(kind, trimmed, false);
        option.setOwner(owner);
        CatalogAttributeOptionEntity saved = repository.save(option);

        TagEnsureResult tagResult = ensureTagIfRequested(owner, kind, trimmed, createTag);
        return new OptionCreateResult(saved, true, false, tagResult.created(), tagResult.duplicate());
    }

    @Transactional
    public CatalogAttributeOptionEntity addCustom(Kind kind, String name) {
        return addCustom(null, kind, name, shouldAutoCreateTag(kind)).option();
    }

    @Transactional
    public void delete(AccountEntity user, Long id) {
        CatalogAttributeOptionEntity opt = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hodnota číselníku (id=" + id + ") nenalezena."));
        if (opt.isSystem()) {
            throw new ForbiddenException("Systémovou hodnotu nelze smazat.");
        }
        if (opt.getOwner() != null && user != null && !opt.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Hodnota číselníku nepatří uživateli.");
        }
        repository.delete(opt);
    }

    @Transactional
    public void delete(Long id) {
        delete(null, id);
    }

    private Optional<CatalogAttributeOptionEntity> findVisibleDuplicate(AccountEntity owner, Kind kind, String name) {
        String normalized = TextNormalizer.comparable(name);
        List<CatalogAttributeOptionEntity> visible = owner == null
                ? repository.findByKindOrderByIsSystemDescNameAsc(kind)
                : repository.findVisibleTo(kind, owner.getId());
        return visible.stream()
                .filter(o -> TextNormalizer.comparable(o.getName()).equals(normalized))
                .findFirst();
    }

    private TagEnsureResult ensureTagIfRequested(AccountEntity owner, Kind kind, String name, Boolean createTag) {
        if (!Boolean.TRUE.equals(createTag)) {
            return new TagEnsureResult(false, false);
        }
        TagCategory category = categoryForKind(kind);
        if (category == null || owner == null) {
            return new TagEnsureResult(false, false);
        }
        String normalized = TextNormalizer.comparable(name);
        Optional<TrainingTagEntity> duplicate = tagRepository.findVisibleTo(owner.getId()).stream()
                .filter(t -> TextNormalizer.comparable(t.getName()).equals(normalized))
                .findFirst();
        if (duplicate.isPresent()) {
            return new TagEnsureResult(false, true);
        }
        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setOwner(owner);
        tag.setSystem(false);
        tag.setName(name);
        tag.setColor(defaultColor(category));
        tag.setCategory(category);
        tagRepository.save(tag);
        return new TagEnsureResult(true, false);
    }

    public static boolean shouldAutoCreateTag(Kind kind) {
        return kind == Kind.BODY_REGION || kind == Kind.MOVEMENT_PATTERN;
    }

    public static TagCategory categoryForKind(Kind kind) {
        return switch (kind) {
            case BODY_REGION -> TagCategory.BODY_REGION;
            case MOVEMENT_PATTERN -> TagCategory.MOVEMENT_PATTERN;
            case EQUIPMENT -> TagCategory.EQUIPMENT;
        };
    }

    private static Comparator<CatalogAttributeOptionEntity> optionComparator() {
        return Comparator
                .comparing(CatalogAttributeOptionEntity::isSystem).reversed()
                .thenComparing(o -> TextNormalizer.comparable(o.getLabel()));
    }

    private static String defaultColor(TagCategory category) {
        return switch (category) {
            case BODY_REGION -> "#0dcaf0";
            case MOVEMENT_PATTERN -> "#9F371B";
            case EQUIPMENT -> "#6c757d";
            case GENERAL -> "#6c757d";
        };
    }

    private record TagEnsureResult(boolean created, boolean duplicate) {}

    public record OptionCreateResult(
            CatalogAttributeOptionEntity option,
            boolean created,
            boolean duplicate,
            boolean tagCreated,
            boolean tagDuplicate) {}
}