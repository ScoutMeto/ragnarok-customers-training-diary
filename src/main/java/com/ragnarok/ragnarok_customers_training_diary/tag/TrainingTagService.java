package com.ragnarok.ragnarok_customers_training_diary.tag;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD nad uživatelskými tagy + read systémových.
 *
 * <p>System tagy ({@code is_system=true}, {@code owner=null}) jsou globální a nelze
 * je editovat/mazat z aplikační vrstvy. Custom tagy patří vždy konkrétnímu uživateli
 * a viditelné jsou jen jemu.
 */
@Service
@Transactional
public class TrainingTagService {

    private final TrainingTagRepository repository;

    public TrainingTagService(TrainingTagRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TrainingTagEntity> findVisibleTo(AccountEntity user) {
        return repository.findVisibleTo(user.getId());
    }

    public TrainingTagEntity createCustomTag(AccountEntity owner, String name, String color) {
        TrainingTagEntity tag = new TrainingTagEntity();
        tag.setOwner(owner);
        tag.setSystem(false);
        tag.setName(name.trim());
        tag.setColor(color);
        return repository.save(tag);
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
}
