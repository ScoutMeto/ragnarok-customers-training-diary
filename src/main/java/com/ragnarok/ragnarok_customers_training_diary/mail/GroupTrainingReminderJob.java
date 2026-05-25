package com.ragnarok.ragnarok_customers_training_diary.mail;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.training.TrainingService;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cron job: každý večer (default 18:00) zkontroluje, jestli zítra je nějaký skupinový
 * trénink. Pokud ano, pošle upomínku všem klientům, kteří mají
 * {@code notif_group_training_reminder=true}.
 *
 * <p>Cron expression je konfigurovatelný přes {@code ragnarok.mail.group-reminder-cron}.
 */
@Component
public class GroupTrainingReminderJob {

    private static final Logger log = LoggerFactory.getLogger(GroupTrainingReminderJob.class);

    private final TrainingService trainingService;
    private final AccountRepository accountRepository;
    private final EmailService emailService;

    public GroupTrainingReminderJob(TrainingService trainingService,
                                     AccountRepository accountRepository,
                                     EmailService emailService) {
        this.trainingService = trainingService;
        this.accountRepository = accountRepository;
        this.emailService = emailService;
    }

    /**
     * Spouštěno dle cron expression v {@code ragnarok.mail.group-reminder-cron}
     * (default {@code 0 0 18 * * *} = každý den v 18:00).
     */
    @Scheduled(cron = "${ragnarok.mail.group-reminder-cron:0 0 18 * * *}")
    @Transactional(readOnly = true)
    public void sendTomorrowReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<TrainingEntity> tomorrowGroup = trainingService.listGroupTrainingsForDay(tomorrow);
        if (tomorrowGroup.isEmpty()) {
            log.info("[group-reminder] zitra {} zadny skupinovy trenink, skip", tomorrow);
            return;
        }

        // Aktivní klienti (USER role, deleted_at NULL, emailConfirmed)
        List<AccountEntity> recipients = accountRepository
                .findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc()
                .stream()
                .filter(a -> a.getRole() == AccountRole.USER)
                .filter(AccountEntity::isEmailConfirmed)
                .filter(AccountEntity::isNotifGroupTrainingReminder)
                .toList();

        log.info("[group-reminder] zitra {} skupinovych: {}, recipienti: {}",
                tomorrow, tomorrowGroup.size(), recipients.size());

        for (TrainingEntity training : tomorrowGroup) {
            String trainerName = training.getCreatedBy() != null
                    ? training.getCreatedBy().getFirstName() + " " + training.getCreatedBy().getLastName()
                    : null;
            for (AccountEntity client : recipients) {
                emailService.sendGroupTrainingReminder(client, training.getName(),
                        tomorrow, training.getStartTime(), trainerName);
            }
        }
    }
}
