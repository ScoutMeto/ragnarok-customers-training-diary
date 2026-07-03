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

    /** ScoutMeto kolo 7: stránkovaný admin výpis nabídek (naposled vytvořené). */
    public org.springframework.data.domain.Page<TextPlanEntity> listGroupOffersPaged(int page, int size) {
        return repository.findByGroupOfferTrue(
                org.springframework.data.domain.PageRequest.of(Math.max(0, page), size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id")));
    }

    /** ScoutMeto kolo 7: stránkovaný admin výpis individuálních textových šablon. */
    public org.springframework.data.domain.Page<TextPlanEntity> listTemplatesPaged(int page, int size) {
        return repository.findByTemplateTrueAndGroupOfferFalse(
                org.springframework.data.domain.PageRequest.of(Math.max(0, page), size,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt", "id")));
    }

    /** Pondělí aktuálního týdne — hranice týdenního okna nabídek. */
    public static java.time.LocalDate currentWeekMonday() {
        return java.time.LocalDate.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }

    /**
     * ScoutMeto kolo 7: nabídky viditelné uživatelům — jen publikované v aktuálním týdnu
     * (pondělí–neděle dle data publikace). S novým pondělkem nabídka minulého týdne zmizí.
     */
    public List<TextPlanEntity> listVisibleGroupOffers() {
        return repository
                .findByGroupOfferTrueAndPublishedTrueAndPublishedAtGreaterThanEqualOrderByPublishedAtDescIdDesc(
                        currentWeekMonday());
    }

    /** ScoutMeto kolo 7: publikuje nabídku (datum publikace = dnes → spadne do aktuálního týdne). */
    @Transactional
    public void publishGroupOffer(Long id) {
        TextPlanEntity p = getGroupOffer(id);
        p.setPublished(true);
        p.setPublishedAt(java.time.LocalDate.now());
    }

    /**
     * ScoutMeto kolo 7: duplikát nabídky — přesná kopie, jen „datum" je aktuální
     * (createdAt teď; při publikované předloze se kopie publikuje dneškem → aktuální týden).
     */
    @Transactional
    public TextPlanEntity duplicateGroupOffer(Long id, AccountEntity admin) {
        TextPlanEntity source = getGroupOffer(id);
        TextPlanEntity copy = new TextPlanEntity();
        copy.setTitle(source.getTitle());
        copy.setBody(source.getBody());
        copy.setTemplate(true);
        copy.setGroupOffer(true);
        copy.setCreatedBy(admin);
        copy.setPublished(source.isPublished());
        copy.setPublishedAt(source.isPublished() ? java.time.LocalDate.now() : null);
        return repository.save(copy);
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
    public TextPlanEntity createGroupOffer(AccountEntity admin, String title, String body, boolean publish) {
        validate(title, body);
        TextPlanEntity p = new TextPlanEntity();
        p.setTitle(title.trim());
        p.setBody(body);
        p.setTemplate(true);
        p.setGroupOffer(true);
        p.setCreatedBy(admin);
        p.setPublished(publish);
        p.setPublishedAt(publish ? java.time.LocalDate.now() : null);
        return repository.save(p);
    }

    /**
     * Update nabídky. {@code publish=true} nastaví publikaci (u draftu čerstvým dneškem);
     * {@code publish=false} nabídku stáhne do draftu (uživatelům zmizí).
     */
    @Transactional
    public TextPlanEntity updateGroupOffer(Long id, String title, String body, boolean publish) {
        validate(title, body);
        TextPlanEntity p = getGroupOffer(id);
        p.setTitle(title.trim());
        p.setBody(body);
        if (publish) {
            // Review fix: „Uložit" obnoví publikaci dneškem i u nabídky mimo týdenní okno
            // (dřív se prošlá nabídka nedala vrátit do aktuálního týdne bez duplikace).
            if (!p.isPublished() || p.getPublishedAt() == null
                    || p.getPublishedAt().isBefore(currentWeekMonday())) {
                p.setPublished(true);
                p.setPublishedAt(java.time.LocalDate.now());
            }
        } else {
            p.setPublished(false);
        }
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

    /** ScoutMeto kolo 7: uživatel smaže svou kopii textového plánu. */
    @Transactional
    public void deleteOwnCopy(AccountEntity user, Long id) {
        TextPlanEntity p = getForUser(user, id);
        repository.delete(p);
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
