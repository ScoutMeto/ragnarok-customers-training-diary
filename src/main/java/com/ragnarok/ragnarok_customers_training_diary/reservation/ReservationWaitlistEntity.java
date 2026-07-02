package com.ragnarok.ragnarok_customers_training_diary.reservation;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ScoutMeto kolo 7: náhradník na plnou lekci. Žije jen v diary (rezervační systém
 * o waitlistu neví). Pořadí náhradníků určuje {@link #createdAt}; při uvolnění místa
 * je první WAITING záznam povýšen — vytvoří se skutečná rezervace přes veřejné API
 * a stav se přepne na PROMOTED.
 */
@Entity
@Table(name = "reservation_waitlist")
@Getter
@Setter
@NoArgsConstructor
public class ReservationWaitlistEntity {

    public enum Status { WAITING, PROMOTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    /** ID lekce v rezervačním systému. */
    @Column(name = "ext_training_id", nullable = false)
    private Long extTrainingId;

    @Column(name = "training_title")
    private String trainingTitle;

    @Column(name = "training_start")
    private LocalDateTime trainingStart;

    /** Určuje pořadí náhradníků (FIFO). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.WAITING;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
