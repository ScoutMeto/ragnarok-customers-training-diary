package com.ragnarok.ragnarok_customers_training_diary.benefit;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.common.NotFoundException;
import com.ragnarok.ragnarok_customers_training_diary.mail.EmailService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScoutMeto kolo 7: správa tabulky Výhody + odesílání požadavků na využití výhody.
 */
@Service
@Transactional
public class BenefitService {

    private static final Logger log = LoggerFactory.getLogger(BenefitService.class);

    private final BenefitItemRepository repository;
    private final EmailService emailService;

    public BenefitService(BenefitItemRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<BenefitItemEntity> listAll() {
        return repository.findAllByOrderByPositionAscIdAsc();
    }

    @Transactional(readOnly = true)
    public BenefitItemEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Výhoda (id=" + id + ") nenalezena."));
    }

    public BenefitItemEntity save(BenefitItemEntity item) {
        validate(item);
        return repository.save(item);
    }

    public void delete(Long id) {
        repository.delete(getById(id));
    }

    /**
     * Uživatel využije výhodu s tlačítkem: odešle se e-mail na adresu výhody v pořadí
     * předmět → text uživatele (nepovinný) → přednastavený text → údaje uživatele.
     */
    public void useBenefit(AccountEntity user, Long benefitId, String userText) {
        BenefitItemEntity benefit = getById(benefitId);
        if (benefit.getHelpType() != BenefitItemEntity.HelpType.BUTTON) {
            throw new IllegalArgumentException("Tato výhoda nemá tlačítko pro odeslání požadavku.");
        }
        if (benefit.getButtonEmail() == null || benefit.getButtonEmail().isBlank()) {
            throw new IllegalArgumentException("Výhoda nemá nastavenou cílovou e-mailovou adresu — ozvi se trenérovi.");
        }
        if (!user.isBenefitsVisible()) {
            throw new IllegalArgumentException("Sekce Výhody není pro tvůj účet aktivní.");
        }
        emailService.sendBenefitRequest(benefit, user, userText);
        log.info("[benefit] account_id={} used benefit id={} ({})", user.getId(), benefitId, benefit.getName());
    }

    private void validate(BenefitItemEntity item) {
        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Název výhody je povinný.");
        }
        if (item.getHelpType() == BenefitItemEntity.HelpType.BUTTON
                && (item.getButtonEmail() == null || item.getButtonEmail().isBlank())) {
            throw new IllegalArgumentException("U výhody s tlačítkem je cílový e-mail povinný.");
        }
    }
}
