package com.ragnarok.ragnarok_customers_training_diary.training.types.cardio;

import com.ragnarok.ragnarok_customers_training_diary.training.TrainingExerciseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dlouhé pomalé kardio (kolo 10).
 *
 * <p>Model je obecný — neváže se na konkrétní sport (aktivita se vybírá z katalogu
 * cviků) a nevyžaduje jednu jednotku výkonu. Povinná je jen doba trvání; vzdálenost,
 * opakování, kroky i převýšení jsou volitelné a lze je kombinovat.
 *
 * <p>Obecné atributy tréninku (tepová frekvence, RPE, poznámka, tělesná váha) se tu
 * záměrně neduplikují — jsou na tréninkové jednotce, resp. na cviku.
 */
@Entity
@Table(name = "cardio_config")
@Getter
@Setter
@NoArgsConstructor
public class CardioConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    /** Celkový čas od zahájení do ukončení aktivity včetně přestávek. */
    @Column(name = "elapsed_seconds", nullable = false)
    private Integer elapsedSeconds;

    /** Čistý čas aktivity bez přestávek (dopočet, uživatel smí přepsat). */
    @Column(name = "active_seconds")
    private Integer activeSeconds;

    /** Vzdálenost vždy v metrech; UI zobrazuje m nebo km. */
    @Column(name = "distance_m")
    private Integer distanceM;

    private Integer repetitions;

    private Integer steps;

    /** Převýšení v metrech. */
    @Column(name = "elevation_gain_m")
    private Integer elevationGainM;

    @Column(name = "avg_speed_kmh", precision = 6, scale = 2)
    private BigDecimal avgSpeedKmh;

    /** Průměrné tempo v sekundách na kilometr (zobrazuje se jako 5:12 min/km). */
    @Column(name = "avg_pace_s_per_km")
    private Integer avgPaceSPerKm;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "cardioConfig", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<CardioPauseEntity> pauses = new ArrayList<>();

    /** Souhrnná délka všech přestávek v sekundách. */
    public int totalPauseSeconds() {
        return pauses.stream()
                .mapToInt(p -> p.getDurationSeconds() != null ? p.getDurationSeconds() : 0)
                .sum();
    }

    /**
     * Čistý čas aktivity. Pokud ho uživatel nezadal, dopočítá se jako
     * {@code elapsed − suma pauz}. Nikdy nevrací záporné číslo.
     */
    public Integer resolvedActiveSeconds() {
        if (activeSeconds != null) return activeSeconds;
        if (elapsedSeconds == null) return null;
        return Math.max(0, elapsedSeconds - totalPauseSeconds());
    }

    /**
     * Průměrná rychlost v km/h. Pro sportovní výkon se počítá z čistého času,
     * ne z celkového — uživatelem zadaná hodnota má přednost.
     */
    public BigDecimal resolvedAvgSpeedKmh() {
        if (avgSpeedKmh != null) return avgSpeedKmh;
        Integer active = resolvedActiveSeconds();
        if (distanceM == null || active == null || active <= 0) return null;
        return BigDecimal.valueOf(distanceM)
                .multiply(BigDecimal.valueOf(3.6))
                .divide(BigDecimal.valueOf(active), 2, RoundingMode.HALF_UP);
    }

    /** Průměrné tempo v sekundách na kilometr (opět z čistého času). */
    public Integer resolvedAvgPaceSPerKm() {
        if (avgPaceSPerKm != null) return avgPaceSPerKm;
        Integer active = resolvedActiveSeconds();
        if (distanceM == null || distanceM <= 0 || active == null || active <= 0) return null;
        return (int) Math.round(active * 1000.0 / distanceM);
    }
}
