package com.ragnarok.ragnarok_customers_training_diary.benefit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ScoutMeto kolo 7: řádek tabulky „Výhody" (společná pro všechny USER, edituje admin).
 * Nápověda (3. sloupec) je buď prostý text ({@link HelpType#TEXT}), nebo tlačítko
 * ({@link HelpType#BUTTON}) — uživatel přes něj pošle e-mail na adresu výhody;
 * {@link #buttonPresetText} se přiloží skrytě jako ověření, že mail jde z aplikace.
 */
@Entity
@Table(name = "benefit_item")
@Getter
@Setter
@NoArgsConstructor
public class BenefitItemEntity {

    public enum HelpType { TEXT, BUTTON }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pořadí řádku v tabulce. */
    @Column(nullable = false)
    private int position = 0;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "help_type", nullable = false, length = 8)
    private HelpType helpType = HelpType.TEXT;

    /** Text nápovědy (jen pro {@link HelpType#TEXT}). */
    @Column(name = "help_text", columnDefinition = "TEXT")
    private String helpText;

    @Column(name = "button_label", length = 64)
    private String buttonLabel;

    @Column(name = "button_email")
    private String buttonEmail;

    @Column(name = "button_subject")
    private String buttonSubject;

    /** Skrytý přednastavený text — USER ho nevidí, přikládá se do mailu. */
    @Column(name = "button_preset_text", columnDefinition = "TEXT")
    private String buttonPresetText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
