package com.ragnarok.ragnarok_customers_training_diary.coach;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 21: správa textových plánů. Admin vytváří šablony a přiřazuje je uživatelům
 * (vznikne editovatelná kopie). Uživatel si svou kopii čte a edituje.
 */
@Service
@Transactional(readOnly = true)
public class TextPlanService {

    private final TextPlanRepository repository;
    private final AccountRepository accountRepository;
    private final com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;

    public TextPlanService(TextPlanRepository repository, AccountRepository accountRepository,
            com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
    }

    // ----- Admin: šablony -----

    public List<TextPlanEntity> listTemplates() {
        return repository.findByTemplateTrueOrderByCreatedAtDesc();
    }

    public TextPlanEntity getTemplate(Long id) {
        TextPlanEntity p = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Textová šablona (id=" + id + ") nenalezena."));
        if (!p.isTemplate()) {
            throw new NotFoundException("To není textová šablona.");
        }
        return p;
    }

    @Transactional
    public TextPlanEntity createTemplate(AccountEntity admin, String title, String body) {
        validate(title, body);
        TextPlanEntity p = new TextPlanEntity();
        p.setTitle(title.trim());
        p.setBody(body);
        p.setTemplate(true);
        p.setCreatedBy(admin);
        return repository.save(p);
    }

    @Transactional
    public TextPlanEntity updateTemplate(Long id, String title, String body) {
        validate(title, body);
        TextPlanEntity p = getTemplate(id);
        p.setTitle(title.trim());
        p.setBody(body);
        return p;
    }

    @Transactional
    public void deleteTemplate(Long id) {
        repository.delete(getTemplate(id));
    }

    /** Přiřadí šablonu uživateli → vytvoří jeho editovatelnou kopii (+ notifikace). */
    @Transactional
    public TextPlanEntity assignToUser(Long templateId, Long userId) {
        TextPlanEntity tpl = getTemplate(templateId);
        AccountEntity user = accountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Uživatel (id=" + userId + ") nenalezen."));
        TextPlanEntity copy = new TextPlanEntity();
        copy.setTitle(tpl.getTitle());
        copy.setBody(tpl.getBody());
        copy.setTemplate(false);
        copy.setOwner(user);
        copy.setCreatedBy(tpl.getCreatedBy());
        copy.setSourceTemplate(tpl);
        TextPlanEntity saved = repository.save(copy);

        String trainerName = tpl.getCreatedBy() != null
                ? tpl.getCreatedBy().getFirstName() + " " + tpl.getCreatedBy().getLastName() : null;
        emailService.sendNewPlanAssignedNotification(user, "textový plán", tpl.getTitle(), trainerName);
        return saved;
    }

    // ----- Uživatel: kopie -----

    public List<TextPlanEntity> listForUser(Long userId) {
        return repository.findByOwner_IdAndTemplateFalseOrderByCreatedAtDesc(userId);
    }

    /** Kopie uživatele s ověřením vlastnictví (admin smí taky). */
    public TextPlanEntity getForUser(AccountEntity user, Long id) {
        TextPlanEntity p = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Textový plán (id=" + id + ") nenalezen."));
        boolean isAdmin = user.getRole() == com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.ADMIN;
        if (p.isTemplate()
                || p.getOwner() == null
                || (!isAdmin && !p.getOwner().getId().equals(user.getId()))) {
            throw new ForbiddenException("Tento plán ti nepatří.");
        }
        return p;
    }

    /** Uživatel edituje svou kopii. */
    @Transactional
    public void updateOwnCopy(AccountEntity user, Long id, String title, String body) {
        validate(title, body);
        TextPlanEntity p = getForUser(user, id);
        p.setTitle(title.trim());
        p.setBody(body);
    }

    private void validate(String title, String body) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Název je povinný.");
        }
        if (body == null) {
            throw new IllegalArgumentException("Text plánu je povinný.");
        }
    }
}
