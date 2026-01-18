package com.ragnarok.ragnarok_customers_training_diary.entity.repository;

import com.ragnarok.ragnarok_customers_training_diary.entity.TrainingEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingEntity, Long>, JpaSpecificationExecutor<TrainingEntity> {

    List<TrainingEntity> findByDateOfCurrentLessonBetween(@Param ("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    List<TrainingEntity> findByParentTrainingIdAndStartOfCurrentLessonGreaterThanEqual(Long parentTrainingId, LocalDateTime startOfCurrentLesson);



}
