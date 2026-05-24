package com.ragnarok.ragnarok_customers_training_diary.training.types.circuit;

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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "circuit_config")
@Getter
@Setter
@NoArgsConstructor
public class CircuitConfigEntity {

    @Id
    @Column(name = "training_exercise_id")
    private Long trainingExerciseId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "training_exercise_id")
    private TrainingExerciseEntity trainingExercise;

    @Column(nullable = false)
    private Integer rounds = 3;

    @Column(name = "rest_between_rounds_s")
    private Integer restBetweenRoundsS;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "circuitConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<CircuitStepEntity> steps = new ArrayList<>();

    @OneToMany(mappedBy = "circuitConfig", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("roundIndex ASC")
    private List<CircuitRoundRestEntity> roundRests = new ArrayList<>();
}
