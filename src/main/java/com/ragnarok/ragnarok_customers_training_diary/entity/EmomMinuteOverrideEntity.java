package com.ragnarok.ragnarok_customers_training_diary.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "emom_minute_override")
@Table(name = "emom_minute_override")
@Getter
@Setter
public class EmomMinuteOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emomMinuteOverrideId;

    @ManyToOne
    @JoinColumn(name = "emom_id", referencedColumnName = "emomId", nullable = false)
    private EmomEntity emom;

    private int minuteIndex;
    private Integer reps;
    private Double weight;
    private String notes;
}