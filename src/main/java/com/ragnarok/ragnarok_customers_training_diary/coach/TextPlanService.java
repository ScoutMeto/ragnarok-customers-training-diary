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
        return repository.findByTemplateTrueAndGroupOfferFalseOrderByCreatedAtDesc();
    }

    public TextPlanEntity getTemplate(Long id) {
        TextPlanEntity p = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Textová šablona (id=" + id + ") nenalezena."));
        // Skupinová nabídka má taky template=true → musíme ji tady vyloučit, jinak by ji
        // individuální /admin/text-plans flow (edit/assign/delete) omylem akceptoval.
        if (!p.isTemplate() || p.isGroupOffer()) {
            throw new NotFoundException("To není (individuální) textová šablona.");
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

    // ----- ScoutMeto kolo 6: skupinové textové nabídky -----

    public List<TextPlanEntity> listGroupOffers() {
        return repository.findByGroupOfferTrueOrderByCreatedAtDesc();
    }

    public TextPlanEntity getGroupOffer(Long id) {
        TextPlanEntity p = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Skupinová nabídka (id=" + id + ") nenalezena."));
        if (!p.isGroupOffer()) {
            throw new NotFoundException("To není skupinová textová nabídka.");
        }
        return p;
    }

    @Transactional
    public TextPlanEntity createGroupOffer(AccountEntity admin, String title, String body) {
        validate(title, body);
        TextPlanEntity p = new TextPlanEntity();
        p.setTitle(title.trim());
        p.setBody(body);
        p.setTemplate(true);
        p.setGroupOffer(true);
        p.setCreatedBy(admin);
        return repository.save(p);
    }

    @Transactional
    public TextPlanEntity updateGroupOffer(Long id, String title, String body) {
        validate(title, body);
        TextPlanEntity p = getGroupOffer(id);
        p.setTitle(title.trim());
        p.setBody(body);
        return p;
    }

    @Transactional
    public void deleteGroupOffer(Long id) {
        repository.delete(getGroupOffer(id));
    }

    /** Už si uživatel tuto nabídku přidal? */
    public boolean hasAddedOffer(Long userId, Long offerId) {
        return repository.existsByOwner_IdAndSourceTemplate_Id(userId, offerId);
    }

    /** ID všech nabídek/šablon, které už uživatel má jako kopii (jeden dotaz — bez N+1). */
    public java.util.Set<Long> addedSourceIds(Long userId) {
        return repository.findAddedSourceIdsByOwnerId(userId);
    }

    /**
     * Uživatel si přidá skupinovou textovou nabídku → vznikne jeho editovatelná kopie
     * v „Můj plán". Idempotentní: pokud už ji má, nevytváří duplicitu.
     */
    @Transactional
    public TextPlanEntity addGroupOfferToUser(Long offerId, AccountEntity user) {
        TextPlanEntity offer = getGroupOffer(offerId);
        if (repository.existsByOwner_IdAndSourceTemplate_Id(user.getId(), offerId)) {
            return null; // už přidáno
        }
        TextPlanEntity copy = new TextPlanEntity();
        copy.setTitle(offer.getTitle());
        copy.setBody(offer.getBody());
        copy.setTemplate(false);
        copy.setGroupOffer(false);
        copy.setOwner(user);
        copy.setCreatedBy(offer.getCreatedBy());
        copy.setSourceTemplate(offer);
        return repository.save(copy);
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
