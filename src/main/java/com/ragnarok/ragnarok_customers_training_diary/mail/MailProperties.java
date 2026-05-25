package com.ragnarok.ragnarok_customers_training_diary.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurace odesílatele emailů + base URL pro odkazy v mailech.
 * Mapuje properties z {@code application.properties} ({@code ragnarok.mail.*}).
 */
@Configuration
@ConfigurationProperties(prefix = "ragnarok.mail")
public class MailProperties {

    /** Email odesílatele (např. {@code noreply@ragnarok-diary.cz}). */
    private String from = "noreply@ragnarok-diary.cz";

    /** Zobrazované jméno odesílatele (např. „Ragnarok Training Diary"). */
    private String fromName = "Ragnarok Training Diary";

    /** Base URL aplikace, používá se v odkazech v mailech. */
    private String baseUrl = "http://localhost:8080";

    /**
     * Když {@code true}, emaily se neodesílají přes SMTP, jen se logují do console.
     * Užitečné pro lokální dev bez SMTP. Default: true (bezpečnější).
     */
    private boolean fake = true;

    /** Cron expression pro denní reminder skupinových tréninků. */
    private String groupReminderCron = "0 0 18 * * *";

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public boolean isFake() { return fake; }
    public void setFake(boolean fake) { this.fake = fake; }

    public String getGroupReminderCron() { return groupReminderCron; }
    public void setGroupReminderCron(String groupReminderCron) { this.groupReminderCron = groupReminderCron; }
}
