package com.ragnarok.ragnarok_customers_training_diary.coach;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD nad {@link CoachPlanEntity}. Tvorbu/editaci/mazání smí jen admin (vynuceno
 * security configem na URL pattern /admin/**). Klient může jen číst svoje plány.
 */
@Service
@Transactional
public class CoachPlanService {

    private final CoachPlanRepository repository;
    private final AccountRepository accountRepository;
    private final com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;

    public CoachPlanService(CoachPlanRepository repository, AccountRepository accountRepository,
                             com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<CoachPlanEntity> listForClient(Long clientId) {
        return repository.findByClient_IdOrderByValidFromDesc(clientId);
    }

    @Transactional(readOnly = true)
    public Optional<CoachPlanEntity> findActiveForClient(Long clientId, LocalDate date) {
        return repository.findActiveForClient(clientId, date);
    }

    @Transactional(readOnly = true)
    public CoachPlanEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Coach plan (id=" + id + ") nenalezen."));
    }

    /**
     * Klient může číst jen své plány (read-only check).
     */
    @Transactional(readOnly = true)
    public CoachPlanEntity getForClientOrAdmin(AccountEntity user, Long planId) {
        CoachPlanEntity plan = getById(planId);
        boolean isAdmin = user.getRole() == AccountRole.ADMIN;
        boolean isClient = plan.getClient().getId().equals(user.getId());
        if (!isAdmin && !isClient) {
            throw new ForbiddenException("Tento coach plan nepatří uživateli.");
        }
        return plan;
    }

    public CoachPlanEntity create(AccountEntity author, Long clientId,
                                   String title, String bodyMarkdown,
                                   LocalDate validFrom, LocalDate validTo) {
        AccountEntity client = accountRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Klient (id=" + clientId + ") nenalezen."));

        CoachPlanEntity plan = new CoachPlanEntity();
        plan.setClient(client);
        plan.setAuthor(author);
        plan.setTitle(title);
        plan.setBodyMarkdown(bodyMarkdown);
        plan.setValidFrom(validFrom);
        plan.setValidTo(validTo);
        CoachPlanEntity saved = repository.save(plan);

        // Phase 6: notifikace klientovi že má nový plán
        emailService.sendNewPlanAssignedNotification(client, "plán od trenéra",
                title, author.getFirstName() + " " + author.getLastName());

        return saved;
    }

    public CoachPlanEntity update(Long id, String title, String bodyMarkdown,
                                   LocalDate validFrom, LocalDate validTo) {
        CoachPlanEntity plan = getById(id);
        plan.setTitle(title);
        plan.setBodyMarkdown(bodyMarkdown);
        plan.setValidFrom(validFrom);
        plan.setValidTo(validTo);
        return repository.save(plan);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Coach plan (id=" + id + ") nenalezen.");
        }
        repository.deleteById(id);
    }
}
