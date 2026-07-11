package com.ragnarok.ragnarok_customers_training_diary.training.types.kbsport;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Phase 13 (A12): jeden interval KB sport setu — počet opakování + volitelně strana (L/P). */
@Entity
@Table(name = "kb_sport_interval")
@Getter
@Setter
@NoArgsConstructor
public class KbSportIntervalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kb_sport_config_id", nullable = false)
    private KbSportConfigEntity kbSportConfig;

    @Column(name = "interval_index", nullable = false)
    private Integer intervalIndex;

    private Integer reps;

    /** ScoutMeto kolo 8: trvání části v sekundách (podrobný záznam po částech). */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** Strana u unilaterálního cviku: "L" / "P" / null. */
    @Column(length = 1)
    private String side;

    @Column(length = 255)
    private String note;
}
