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

    /**
     * Vlastník tréninku. {@code null} pro skupinové tréninky (visibility=GROUP).
     * Pro PRIVATE musí být NOT NULL (DB constraint {@code training_visibility_owner_check}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AccountEntity owner;

    /**
     * Kdo trénink založil. Pro PRIVATE typicky = owner. Pro GROUP = admin (trenér).
     * Nullable kvůli ON DELETE SET NULL — když admin odejde, jeho group tréninky zůstanou.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AccountEntity createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TrainingVisibility visibility = TrainingVisibility.PRIVATE;

    /**
     * Pokud je tento trénink vytvořen z šablony, odkaz na originál (visibility=TEMPLATE).
     * Vyplní se v {@link TrainingService} při „přiřazení šablony klientovi". Nullable.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_template_id")
    private TrainingEntity sourceTemplate;

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

    /** Phase 14 (B8): uživatel si označí důležitý trénink vlaječkou. */
    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

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
