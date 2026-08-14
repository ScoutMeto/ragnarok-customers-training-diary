package com.ragnarok.ragnarok_customers_training_diary.tag;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Tag pro štítkování tréninků. Dva typy:
 * <ul>
 *     <li><b>System tag</b> ({@code isSystem=true}, {@code owner=null}) — předdefinovaný
 *         (např. KB, CARDIO, BODYWEIGHT, OS_RESETS, ...), seed migrace</li>
 *     <li><b>Custom tag</b> ({@code isSystem=false}, {@code owner!=null}) — klient si
 *         přidá vlastní (např. "ranní", "doma", "Tabata-only")</li>
 * </ul>
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
     * Stabilní identifikátor systémového tagu (kolo 10) — u vlastních tagů {@code null}.
     *
     * <p>UI i statistiky se rozhodují podle klíče, ne podle {@link #name}, aby přejmenování
     * tagu (počeštění, úprava terminologie) nerozbilo logiku jednotek a agregací.
     * Hodnoty viz {@link SystemTag}.
     */
    @Column(name = "system_key", length = 32)
    private String systemKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
