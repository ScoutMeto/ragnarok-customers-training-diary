package com.ragnarok.ragnarok_customers_training_diary.equipment;

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
 * Phase 11 (A14): pomůcka/náčiní pro per-exercise výběr. System defaults
 * ({@code isSystem=true}, owner NULL) vidí všichni; custom ({@code owner != null})
 * vidí jen autor. Klient si vlastní pomůcku přidá zadáním názvu ve formuláři
 * a může ji smazat.
 */
@Entity
@Table(name = "equipment_option")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
