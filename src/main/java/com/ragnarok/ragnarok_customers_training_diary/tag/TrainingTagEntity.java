package com.ragnarok.ragnarok_customers_training_diary.tag;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tag pro štítkování tréninků. Systémové tagy jsou globální, vlastní tagy patří
 * konkrétnímu uživateli. Kategorie rozhoduje, zda tag vstupuje do analytických
 * grafů oblastí těla, pohybových vzorců nebo náčiní.
 */
@Entity
@Table(name = "training_tag")
@Getter
@Setter
@NoArgsConstructor
public class TrainingTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 16)
    private String color;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    /**
     * Stabilní identifikátor systémového tagu. U vlastních tagů zůstává null,
     * jejich analytický význam určuje {@link #category}.
     */
    @Column(name = "system_key", length = 32)
    private String systemKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TagCategory category = TagCategory.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}