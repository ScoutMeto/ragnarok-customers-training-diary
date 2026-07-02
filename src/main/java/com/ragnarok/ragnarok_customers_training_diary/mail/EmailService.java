package com.ragnarok.ragnarok_customers_training_diary.mail;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Centrální odesílatel transakčních i notifikačních emailů. Pokud je
 * {@link MailProperties#isFake()}, místo SMTP volání jen loguje obsah —
 * pro lokální dev bez nastaveného SMTP.
 *
 * <p>Všechny metody jsou {@link Async @Async} — neblokují request thread.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d. M. yyyy", new Locale("cs", "CZ"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;
    private final MailProperties props;

    public EmailService(JavaMailSender mailSender, MailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    // ============================================================================
    // 1) Confirmation code při registraci
    // ============================================================================

    @Async
    public void sendConfirmationCode(AccountEntity account, String code) {
        String subject = "Ragnarok Training Diary — potvrzovací kód";
        String text =
                "Ahoj " + account.getFirstName() + ",\n\n" +
                "tvůj potvrzovací kód pro registraci do Ragnarok Training Diary:\n\n" +
                "    " + code + "\n\n" +
                "Zadej ho na stránce " + props.getBaseUrl() + "/confirm-email\n\n" +
                "Kód platí 60 minut. Pokud jsi se neregistroval(a), ignoruj tento mail.\n\n" +
                "—\nRagnarok Training Diary";
        send(account.getEmail(), subject, text, "confirmation-code");
    }

    // ============================================================================
    // 1b) Kód pro reset zapomenutého hesla (ScoutMeto kolo 6)
    // ============================================================================

    @Async
    public void sendPasswordResetCode(AccountEntity account, String code) {
        String subject = "Ragnarok Training Diary — kód pro obnovu hesla";
        String text =
                "Ahoj " + account.getFirstName() + ",\n\n" +
                "požádal(a) jsi o obnovu hesla. Tvůj kód:\n\n" +
                "    " + code + "\n\n" +
                "Zadej ho na stránce " + props.getBaseUrl() + "/reset-password spolu s novým heslem.\n\n" +
                "Kód platí 60 minut. Pokud jsi o obnovu nežádal(a), tento mail ignoruj — heslo zůstává beze změny.\n\n" +
                "—\nRagnarok Training Diary";
        send(account.getEmail(), subject, text, "password-reset");
    }

    // ============================================================================
    // 2) Welcome mail po potvrzení emailu
    // ============================================================================

    @Async
    public void sendWelcomeEmail(AccountEntity account) {
        if (!account.isNotifWelcome()) return;
        String subject = "Vítej v Ragnarok Training Diary!";
        String text =
                "Ahoj " + account.getFirstName() + ",\n\n" +
                "tvůj účet je aktivní. Pojď trénovat:\n\n" +
                props.getBaseUrl() + "/dashboard\n\n" +
                "Co s tím můžeš dělat:\n" +
                "  - zaznamenat si vlastní tréninky (cviky, sety, RPE)\n" +
                "  - vidět skupinové tréninky, co ti naplánoval trenér\n" +
                "  - sledovat statistiky (volume, RPE trend, body region split)\n" +
                "  - dostávat plány od trenéra (markdown)\n\n" +
                "Notifikace si můžeš nastavit v profilu.\n\n" +
                "—\nRagnarok Training Diary";
        send(account.getEmail(), subject, text, "welcome");
    }

    // ============================================================================
    // 3) Reminder skupinového tréninku zítra (cron)
    // ============================================================================

    @Async
    public void sendGroupTrainingReminder(AccountEntity account, String trainingName,
                                           LocalDate date, java.time.LocalTime startTime,
                                           String trainerName) {
        if (!account.isNotifGroupTrainingReminder()) return;
        String subject = "Zítra v gymu: " + (trainingName != null ? trainingName : "skupinový trénink");
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(account.getFirstName()).append(",\n\n");
        text.append("připomínáme, že zítra ").append(date.format(DATE_FMT));
        if (startTime != null) {
            text.append(" v ").append(startTime.format(TIME_FMT));
        }
        text.append(" je v gymu skupinový trénink");
        if (trainingName != null) text.append(" „").append(trainingName).append("\"");
        text.append(".\n");
        if (trainerName != null) text.append("Trenér: ").append(trainerName).append("\n");
        text.append("\nDetail v deníku: ").append(props.getBaseUrl()).append("/diary\n\n");
        text.append("Nechceš tyhle připomínky? Vypneš je v nastavení účtu.\n\n");
        text.append("—\nRagnarok Training Diary");
        send(account.getEmail(), subject, text.toString(), "group-reminder");
    }

    // ============================================================================
    // 4) Nová šablona / coach plan přiřazen
    // ============================================================================

    @Async
    public void sendNewPlanAssignedNotification(AccountEntity client, String kind,
                                                 String title, String trainerName) {
        if (!client.isNotifNewPlanAssigned()) return;
        String subject = "Máš nový plán od trenéra";
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(client.getFirstName()).append(",\n\n");
        text.append("trenér");
        if (trainerName != null) text.append(" ").append(trainerName);
        text.append(" ti právě připravil ").append(kind);
        if (title != null) text.append(" „").append(title).append("\"");
        text.append(".\n\n");
        if ("šablonu tréninku".equals(kind)) {
            text.append("Najdeš ji ve svém deníku: ").append(props.getBaseUrl()).append("/diary\n");
        } else {
            text.append("Otevři si plán: ").append(props.getBaseUrl()).append("/my-plan\n");
        }
        text.append("\nNechceš tyhle notifikace? Vypneš je v nastavení účtu.\n\n");
        text.append("—\nRagnarok Training Diary");
        send(client.getEmail(), subject, text.toString(), "plan-assigned");
    }

    // ============================================================================
    // 5) Nový komentář k tréninku
    // ============================================================================

    @Async
    public void sendNewCommentNotification(AccountEntity recipient, AccountEntity commenter,
                                            String trainingName, Long trainingId, String commentText) {
        if (!recipient.isNotifNewComment()) return;
        String subject = (commenter.getFirstName() != null ? commenter.getFirstName() : "Někdo")
                + " okomentoval tvůj trénink";
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(recipient.getFirstName()).append(",\n\n");
        text.append(commenter.getFirstName()).append(" ").append(commenter.getLastName());
        text.append(" přidal(a) komentář k tréninku");
        if (trainingName != null) text.append(" „").append(trainingName).append("\"");
        text.append(":\n\n");
        // jednoduchý quote — odsadit každý řádek
        if (commentText != null) {
            for (String line : commentText.split("\n", -1)) {
                text.append("  > ").append(line).append("\n");
            }
        }
        text.append("\nOdpovědět můžeš v aplikaci: ").append(props.getBaseUrl())
                .append("/diary/").append(trainingId).append("\n\n");
        text.append("Nechceš tyhle notifikace? Vypneš je v nastavení účtu.\n\n");
        text.append("—\nRagnarok Training Diary");
        send(recipient.getEmail(), subject, text.toString(), "new-comment");
    }

    // ============================================================================
    // 6) Potvrzení o vytvoření tréninku (Phase 19b)
    // ============================================================================

    @Async
    public void sendTrainingCreatedNotification(AccountEntity client, String trainingName, Long trainingId) {
        if (!client.isNotifTrainingCreated()) return;
        String subject = "Trénink uložen do deníku";
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(client.getFirstName()).append(",\n\n");
        text.append("uložil(a) sis nový trénink");
        if (trainingName != null && !trainingName.isBlank()) text.append(" „").append(trainingName).append("\"");
        text.append(" do svého deníku.\n\n");
        text.append("Otevři si ho: ").append(props.getBaseUrl()).append("/diary/").append(trainingId).append("\n\n");
        text.append("Nechceš tyhle notifikace? Vypneš je v nastavení účtu.\n\n");
        text.append("—\nRagnarok Training Diary");
        send(client.getEmail(), subject, text.toString(), "training-created");
    }

    // ============================================================================
    // 7) Deaktivace účtu adminem (Phase 19b)
    // ============================================================================

    @Async
    public void sendAccountDeactivatedNotification(AccountEntity client) {
        if (!client.isNotifAccountDeactivated()) return;
        String subject = "Tvůj účet byl uveden do neaktivního režimu";
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(client.getFirstName()).append(",\n\n");
        text.append("tvůj účet byl trenérem uveden do neaktivního (read-only) režimu. ");
        text.append("Můžeš se i nadále přihlásit a prohlížet svoje záznamy, ale nelze přidávat ");
        text.append("ani upravovat tréninky.\n\n");
        text.append("Pokud je to omyl nebo máš dotaz, ozvi se trenérovi.\n\n");
        text.append("—\nRagnarok Training Diary");
        send(client.getEmail(), subject, text.toString(), "account-deactivated");
    }

    // ============================================================================
    // 8) Rezervace zrušena (ScoutMeto kolo 7)
    // ============================================================================

    @Async
    public void sendReservationCancelledNotification(AccountEntity account, String lessonTitle,
                                                     java.time.LocalDateTime lessonStart) {
        String subject = "Rezervace zrušena" + (lessonTitle != null ? ": " + lessonTitle : "");
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(account.getFirstName()).append(",\n\n");
        text.append("tvoje rezervace na lekci");
        if (lessonTitle != null) text.append(" „").append(lessonTitle).append("\"");
        if (lessonStart != null) {
            text.append(" dne ").append(lessonStart.toLocalDate().format(DATE_FMT))
                .append(" v ").append(lessonStart.toLocalTime().format(TIME_FMT));
        }
        text.append(" byla zrušena.\n\n");
        text.append("Kdyby sis to rozmyslel(a), můžeš se znovu zapsat v aplikaci: ")
            .append(props.getBaseUrl()).append("/reservations\n\n");
        text.append("—\nRagnarok Training Diary");
        send(account.getEmail(), subject, text.toString(), "reservation-cancelled");
    }

    // ============================================================================
    // 9) Náhradník povýšen — místo se uvolnilo, rezervace vytvořena (ScoutMeto kolo 7)
    // ============================================================================

    @Async
    public void sendWaitlistPromotedNotification(AccountEntity account, String lessonTitle,
                                                 java.time.LocalDateTime lessonStart) {
        String subject = "Na lekci je volno — máš rezervaci!"
                + (lessonTitle != null ? " (" + lessonTitle + ")" : "");
        StringBuilder text = new StringBuilder();
        text.append("Ahoj ").append(account.getFirstName()).append(",\n\n");
        text.append("na lekci");
        if (lessonTitle != null) text.append(" „").append(lessonTitle).append("\"");
        if (lessonStart != null) {
            text.append(" dne ").append(lessonStart.toLocalDate().format(DATE_FMT))
                .append(" v ").append(lessonStart.toLocalTime().format(TIME_FMT));
        }
        text.append(" se uvolnilo místo. Byl(a) jsi na seznamu náhradníků, takže jsme ti\n");
        text.append("rovnou vytvořili rezervaci — počítáme s tebou!\n\n");
        text.append("Pokud se nemůžeš zúčastnit, zruš rezervaci v aplikaci (nejpozději 30 minut\n");
        text.append("před začátkem): ").append(props.getBaseUrl()).append("/reservations\n\n");
        text.append("—\nRagnarok Training Diary");
        send(account.getEmail(), subject, text.toString(), "waitlist-promoted");
    }

    // ============================================================================
    // Low-level send
    // ============================================================================

    private void send(String toEmail, String subject, String text, String kind) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[mail/{}] skipping: empty recipient", kind);
            return;
        }
        if (props.isFake()) {
            log.info("[mail/{}] FAKE-SEND to={} subject={}\n----- BODY -----\n{}\n----- END -----",
                    kind, toEmail, subject, text);
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            try {
                helper.setFrom(props.getFrom(), props.getFromName());
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(props.getFrom());
            }
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(mime);
            log.info("[mail/{}] sent to={}", kind, toEmail);
        } catch (MessagingException | MailException e) {
            log.error("[mail/{}] FAILED to={} reason={}", kind, toEmail, e.getMessage(), e);
        }
    }
}
