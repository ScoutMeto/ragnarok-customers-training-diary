package com.ragnarok.ragnarok_customers_training_diary.lesson;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupLessonPlanRepository extends JpaRepository<GroupLessonPlanEntity, Long> {

    /** Pro klientský pohled — rozsah týden zpět/dopředu. */
    List<GroupLessonPlanEntity> findByLessonDateBetweenOrderByLessonDateAscStartTimeAsc(
            LocalDate from, LocalDate to);

    /** Pro admin sekci — všechny seřazené nejnovější nahoře. */
    List<GroupLessonPlanEntity> findAllByOrderByLessonDateDescStartTimeDesc();
}
