package com.ragnarok.ragnarok_customers_training_diary.training;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.tag.TrainingTagEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trénink — jednotka deníku patřící jednomu klientovi.
 *
 * <p>Vlastní cviky ({@link TrainingExerciseEntity}) v daném pořadí. Trénink je M:N
 * navázán na tagy ({@link TrainingTagEntity}) přes {@code training_tag_link}.
 */
@Entity
@Table(name = "training")
@Getter
@Setter
@NoArgsConstructor
public class TrainingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AccountEntity owner;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private TrainingDifficulty difficulty;

    private Short rpe;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "training", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<TrainingExerciseEntity> exercises = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "training_tag_link",
            joinColumns = @JoinColumn(name = "training_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TrainingTagEntity> tags = new HashSet<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // --- helpery pro správu kolekcí (drží oboustranný vztah konzistentní) ---

    public void addExercise(TrainingExerciseEntity exercise) {
        exercises.add(exercise);
        exercise.setTraining(this);
    }

    public void removeExercise(TrainingExerciseEntity exercise) {
        exercises.remove(exercise);
        exercise.setTraining(null);
    }
}
