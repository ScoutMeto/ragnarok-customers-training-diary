package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingResponseDTO;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public interface TrainingService {

    TrainingDTO createTraining(TrainingDTO trainingDTO);

    // (vyřešeno)Zisk údajů pro proměnnou List<ReservationEntity> reservationsList (každá jednotka) - vyřešeno pomocí fetch.EAGER
    List<TrainingResponseDTO> getAllTrainingsAsCalendarEvents(LocalDateTime startDate, LocalDateTime endDate);

    TrainingDTO getOneTrainingById(Long trainingId);

    void removeOneTraining(Long trainingId);

    void removeAimedTrainings(List<Long> trainingIds);

    void removeAllPlannedTrainings(Long trainingId);

    TrainingDTO editOneTraining(Long trainingId, TrainingDTO trainingDTO);

    void editAllPlannedTrainings(Long trainingId, TrainingDTO trainingDTO);
}
