package com.ragnarok.ragnarok_customers_training_diary.tag;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionRepository;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.common.TextNormalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrainingTagService {

    private final TrainingTagRepository repository;
    private final CatalogAttributeOptionRepository catalogOptionRepository;

    public TrainingTagService(TrainingTagRepository repository,
            CatalogAttributeOptionRepository catalogOptionRepository) {
        this.repository = repository;
        this.catalogOptionRepository = catalogOptionRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainingTagEntity> findVisibleTo(AccountEntity user) {
        return repository.findVisibleTo(user.getId()).stream()
                .sorted(tagComparator())
                .toList();
    }

    public TagCreateResult createCustomTag(AccountEntity owner, String name, String color, TagCategory category) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Název tagu nesmí být prázdný.");
        }
        TagCategory effectiveCategory = category != null ? category : TagCategory.GENERAL;
        String trimmed = name.trim();
        Optional<TrainingTagEntity> duplicate = findVisibleDuplicate(owner, trimmed);
        if (duplicate.isPresent()) {
            return new TagCreateResult(duplicate.get(), false, true, false);
        }

        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setOwner(owner);
        tag.setSystem(false);
        tag.setName(trimmed);
        tag.setColor(color == null || color.isBlank() ? defaultColor(effectiveCategory) : color);
        tag.setCategory(effectiveCategory);
        TrainingTagEntity saved = repository.save(tag);

        boolean catalogOptionCreated = false;
        CatalogAttributeOptionEntity.Kind kind = kindForCategory(effectiveCategory);
        if (kind != null) {
            catalogOptionCreated = ensureCatalogOption(owner, kind, trimmed);
        }
        return new TagCreateResult(saved, true, false, catalogOptionCreated);
    }

    public TrainingTagEntity createCustomTag(AccountEntity owner, String name, String color) {
        return createCustomTag(owner, name, color, TagCategory.GENERAL).tag();
    }

    public void deleteCustomTag(AccountEntity user, Long tagId) {
        TrainingTagEntity tag = repository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Tag (id=" + tagId + ") nenalezen."));

        if (tag.isSystem()) {
            throw new ForbiddenException("Systémový tag nelze smazat.");
        }
        if (tag.getOwner() == null || !tag.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Tag nepatří uživateli.");
        }
        repository.delete(tag);
    }

    private Optional<TrainingTagEntity> findVisibleDuplicate(AccountEntity owner, String name) {
        String normalized = TextNormalizer.comparable(name);
        return repository.findVisibleTo(owner.getId()).stream()
                .filter(t -> TextNormalizer.comparable(t.getName()).equals(normalized))
                .findFirst();
    }

    private boolean ensureCatalogOption(AccountEntity owner, CatalogAttributeOptionEntity.Kind kind, String name) {
        String normalized = TextNormalizer.comparable(name);
        Optional<CatalogAttributeOptionEntity> existing = catalogOptionRepository.findVisibleTo(kind, owner.getId()).stream()
                .filter(o -> TextNormalizer.comparable(o.getName()).equals(normalized))
                .findFirst();
        if (existing.isPresent()) {
            return false;
        }
        CatalogAttributeOptionEntity option = new CatalogAttributeOptionEntity(kind, name, false);
        option.setOwner(owner);
        catalogOptionRepository.save(option);
        return true;
    }

    private static CatalogAttributeOptionEntity.Kind kindForCategory(TagCategory category) {
        return switch (category) {
            case BODY_REGION -> CatalogAttributeOptionEntity.Kind.BODY_REGION;
            case MOVEMENT_PATTERN -> CatalogAttributeOptionEntity.Kind.MOVEMENT_PATTERN;
            case EQUIPMENT -> CatalogAttributeOptionEntity.Kind.EQUIPMENT;
            case GENERAL -> null;
        };
    }

    private static Comparator<TrainingTagEntity> tagComparator() {
        return Comparator
                .comparingInt((TrainingTagEntity tag) -> tag.getCategory() != null ? tag.getCategory().ordinal() : 0)
                .thenComparing(tag -> TextNormalizer.comparable(tag.getName()));
    }

    private static String defaultColor(TagCategory category) {
        return switch (category) {
            case BODY_REGION -> "#0dcaf0";
            case MOVEMENT_PATTERN -> "#9F371B";
            case EQUIPMENT -> "#6c757d";
            case GENERAL -> "#6c757d";
        };
    }

    public record TagCreateResult(
            TrainingTagEntity tag,
            boolean created,
            boolean duplicate,
            boolean catalogOptionCreated) {}
}