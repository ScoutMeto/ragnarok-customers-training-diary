package com.ragnarok.ragnarok_customers_training_diary.training.types.cardio;

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

/**
 * Přestávka uvnitř jednoho cardio záznamu (kolo 10).
 *
 * <p>Není to interval — je to událost v průběhu jedné souvislé aktivity. Zadává se
 * čas od začátku aktivity, délka a zda byla aktivní (chůze, protahování) nebo pasivní.
 * Zapisovat přestávky je dobrovolné; bez nich je čistý čas roven celkovému.
 */
@Entity
@Table(name = "cardio_pause")
@Getter
@Setter
@NoArgsConstructor
public class CardioPauseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cardio_config_id", nullable = false)
    private CardioConfigEntity cardioConfig;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** Čas od začátku celé aktivity, kdy přestávka začala (v sekundách). */
    @Column(name = "start_from_begin_s", nullable = false)
    private Integer startFromBeginS;

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    /** Aktivní pauza (chůze, protahování) × pasivní (sed, stání). */
    @Column(name = "active_pause", nullable = false)
    private boolean activePause = false;

    @Column(name = "distance_at_pause_m")
    private Integer distanceAtPauseM;

    @Column(name = "repetitions_at_pause")
    private Integer repetitionsAtPause;

    @Column(name = "steps_at_pause")
    private Integer stepsAtPause;

    @Column(length = 255)
    private String note;
}
