package com.ragnarok.ragnarok_customers_training_diary.training.types.emom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Override pro konkrétní minutu EMOM (1-based). Použij když 5. minuta má víc reps než ostatní. */
@Entity
@Table(name = "emom_minute_override")
@Getter
@Setter
@NoArgsConstructor
public class EmomMinuteOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emom_config_id", nullable = false)
    private EmomConfigEntity emomConfig;

    @Column(name = "minute_index", nullable = false)
    private Integer minuteIndex;

    private Integer reps;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(length = 255)
    private String note;
}
