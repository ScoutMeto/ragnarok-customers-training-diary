package com.ragnarok.ragnarok_customers_training_diary.service;

import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingResponseDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.mapper.TrainingMapper;
import com.ragnarok.ragnarok_customers_training_diary.entity.TrainingEntity;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.TrainingRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;

@Service
public class TrainingServiceImpl implements TrainingService {

    @Autowired
    TrainingRepository trainingRepository;

    @Autowired
    TrainingMapper trainingMapper;


    @Override
    @Transactional
    public TrainingDTO createTraining(TrainingDTO trainingDTO) {
        // Ruční vytvoření nové entitní instance (mapper zlobil)
        TrainingEntity trainingEntity = new TrainingEntity();

        trainingEntity.setNameOfLesson(trainingDTO.getNameOfLesson());
        trainingEntity.setNumberOfFreeSlots(trainingDTO.getNumberOfFreeSlots());
        trainingEntity.setDateOfCurrentLesson(trainingDTO.getDateOfCurrentLesson());
        trainingEntity.setStartOfCurrentLesson(trainingDTO.getStartOfCurrentLesson());
        trainingEntity.setEndOfCurrentLesson(trainingDTO.getEndOfCurrentLesson());
        trainingEntity.setRepeatIntervalInDays(trainingDTO.getRepeatIntervalInDays());
        trainingEntity.setCoachName(trainingDTO.getCoachName());
        trainingEntity.setNumberOfCopyConcreteTraining(trainingDTO.getNumberOfCopyConcreteTraining());

        // ParentId se nenastavuje při první instanci
        trainingEntity.setParentTrainingId(null);

        TrainingEntity savedEntity = trainingRepository.save(trainingEntity);
        Long parentId = savedEntity.getTrainingId();

        // Vytvoření kopií pokud je počet větší než 1
        if (trainingDTO.getNumberOfCopyConcreteTraining() > 1) {
            createRepeatedLessons(trainingDTO, trainingDTO.getRepeatIntervalInDays(), parentId);
        }

        System.out.println("Trénink uložen: " + trainingDTO + ". Zároveň bylo vytvořeno " + trainingDTO.getNumberOfCopyConcreteTraining() + " kopií vzájemně vzdálených " + trainingDTO.getRepeatIntervalInDays() + " dnů.");

        return trainingMapper.toDTO(savedEntity); // Volitelně: můžeš i ručně vytvořit TrainingDTO pokud mapper blbne
    }

    // Při založení tréninku opakovat lekci kolikrát je libo
    private void createRepeatedLessons(TrainingDTO modelLesson, int repeatIntervalInDays, Long parentId) {
        LocalDateTime nextDate = modelLesson.getDateOfCurrentLesson().plusDays(repeatIntervalInDays);
        int remainNumberOfCopy = modelLesson.getNumberOfCopyConcreteTraining() - 1;

        for (int i = 1; i < modelLesson.getNumberOfCopyConcreteTraining(); i++) {
            TrainingEntity repeatedEntity = new TrainingEntity();

            repeatedEntity.setNameOfLesson(modelLesson.getNameOfLesson());
            repeatedEntity.setNumberOfFreeSlots(modelLesson.getNumberOfFreeSlots());

            repeatedEntity.setDateOfCurrentLesson(nextDate);
            repeatedEntity.setStartOfCurrentLesson(nextDate);
            repeatedEntity.setEndOfCurrentLesson(
                    nextDate.plusHours(
                            modelLesson.getEndOfCurrentLesson().getHour() - modelLesson.getStartOfCurrentLesson().getHour() + 1
                    )
            );

            repeatedEntity.setRepeatIntervalInDays(repeatIntervalInDays);
            repeatedEntity.setCoachName(modelLesson.getCoachName());

            repeatedEntity.setNumberOfCopyConcreteTraining(remainNumberOfCopy);
            repeatedEntity.setParentTrainingId(parentId);

            trainingRepository.save(repeatedEntity);

            nextDate = nextDate.plusDays(repeatIntervalInDays);
            remainNumberOfCopy--;
        }
    }

    // Data pro kalendář
    @Override
    public List<TrainingResponseDTO> getAllTrainingsAsCalendarEvents(LocalDateTime startDate, LocalDateTime endDate) {
        List<TrainingEntity> trainings = trainingRepository.findByDateOfCurrentLessonBetween(startDate, endDate);

        return trainings.stream().map(training -> {
            TrainingResponseDTO dto = new TrainingResponseDTO();
            dto.setTrainingId(training.getTrainingId());
            dto.setTitle(training.getNameOfLesson());
            dto.setStart(training.getStartOfCurrentLesson());
            dto.setEnd(training.getEndOfCurrentLesson());


            // ExtendedProps jako další data, která fullCalendar běžně nedistribuuje a proto musejí být odeslány takto
            Map<String, Object> extendedProps = new HashMap<>();
            extendedProps.put("lessonName", training.getNameOfLesson());
            extendedProps.put("coachName", training.getCoachName());
            extendedProps.put("numberOfFreeSlots", training.getNumberOfFreeSlots());
            extendedProps.put("trainingId", training.getTrainingId());


            dto.setExtendedProps(extendedProps);

            return dto;
        }).toList();
    }

    // Přípravná metoda to delete/edit - zobrazí trénink pro úpravu po kliknutí na přehled v celém týdnu a umožní vybrat z možností: vymzat/upravit (při volbě předá ID další funkci)
    // (vyřešeno)Zisk údajů pro proměnnou List<ReservationEntity> reservationsList (každá jednotka) - vyřešeno pomocí fetch.EAGER
    @Override
    public TrainingDTO getOneTrainingById(Long trainingId) {
        TrainingEntity trainingEntity = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Trénink podle zadaného id " + trainingId + " nenalezen v databázi."));
        System.out.println("Trénink s ID: " + trainingId + " načten.");
        return trainingMapper.toDTO(trainingEntity);
    }

    //Vymazat 1 trénink a všechny jeho rezervace.
    @Transactional
    @Override
    public void removeOneTraining(Long trainingId) {
        // Najdi trénink podle ID
        TrainingEntity trainingEntity = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Trénink podle zadaného ID " + trainingId + " nenalezen."));

        // Odstraní trénink (kaskádově i všechny jeho rezervace)
        trainingRepository.delete(trainingEntity);
        System.out.println("Trénink ID: " + trainingId + " a jeho rezervace byly vymazány.");
    }

    // Odstranit všechny vybrané tréninky (i jejich rezervace).
    @Transactional
    @Override
    public void removeAimedTrainings(List<Long> trainingIds) {
        // Najdi trénink podle ID
        for (Long id : trainingIds) {
            TrainingEntity trainingEntity = trainingRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Trénink podle zadaného ID " + id + " nenalezen."));

            trainingRepository.delete(trainingEntity);
            System.out.println("Trénink ID: " + id + " a jeho rezervace byly vymazány.");
        }
    }

    // Odstranit všechny následující tréninky.
            /*vymaže zvolený trénink a všechny následující tréninkové jednotky, které:
        -se nacházejí ve stejném čase
        -o týden později
        -mají stejný název
        -vymaže také případné existující rezervace
        -TRÉNINKY PŘI VYTVÁŘENÍ PROPOJENÉ POMOCÍ ID (podle ID nalézané a mazané)
         */
    @Transactional
    @Override
    public void removeAllPlannedTrainings(Long trainingId) {
        TrainingEntity clickedTraining = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Trénink nenalezen."));

        Long parentTrainingId = clickedTraining.getParentTrainingId() != null
                ? clickedTraining.getParentTrainingId()
                : clickedTraining.getTrainingId(); // Pokud klikneme na originál, použijeme jeho ID

        LocalDateTime startOfCurrentLesson = clickedTraining.getStartOfCurrentLesson();

        List<TrainingEntity> toDelete = trainingRepository
                .findByParentTrainingIdAndStartOfCurrentLessonGreaterThanEqual(parentTrainingId, startOfCurrentLesson);

        trainingRepository.deleteAll(toDelete);

        // Smaž i právě kliknutý trénink
        trainingRepository.delete(clickedTraining);
    }

    // Editace - jeden trénink
    @Transactional
    @Override
    public TrainingDTO editOneTraining(Long trainingId, TrainingDTO trainingDTO) {
        TrainingEntity trainingEntity = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Trénink podle zadaného ID " + trainingId + " nenalezen."));

        boolean hasChanged = !Objects.equals(trainingEntity.getNameOfLesson(), trainingDTO.getNameOfLesson()) ||
                !Objects.equals(trainingEntity.getStartOfCurrentLesson(), trainingDTO.getStartOfCurrentLesson()) ||
                !Objects.equals(trainingEntity.getEndOfCurrentLesson(), trainingDTO.getEndOfCurrentLesson());

        if (hasChanged) {
            trainingEntity.setParentTrainingId(null);
        }

        trainingEntity.setNameOfLesson(trainingDTO.getNameOfLesson());
        trainingEntity.setNumberOfFreeSlots(trainingDTO.getNumberOfFreeSlots());
        trainingEntity.setDateOfCurrentLesson(trainingDTO.getDateOfCurrentLesson());
        trainingEntity.setStartOfCurrentLesson(trainingDTO.getStartOfCurrentLesson());
        trainingEntity.setEndOfCurrentLesson(trainingDTO.getEndOfCurrentLesson());
        trainingEntity.setRepeatIntervalInDays(trainingDTO.getRepeatIntervalInDays());
        trainingEntity.setCoachName(trainingDTO.getCoachName());
        trainingEntity.setNumberOfCopyConcreteTraining(trainingDTO.getNumberOfCopyConcreteTraining());

        trainingRepository.save(trainingEntity);
        return trainingMapper.toDTO(trainingEntity);
    }


    // Editace - všechny následující tréninky
    @Transactional
    @Override
    public void editAllPlannedTrainings(Long trainingId, TrainingDTO trainingDTO) {
        TrainingEntity clickedTraining = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new EntityNotFoundException("Trénink nenalezen."));

        Long parentTrainingId = clickedTraining.getParentTrainingId() != null
                ? clickedTraining.getParentTrainingId()
                : clickedTraining.getTrainingId();

        LocalDateTime startFrom = clickedTraining.getStartOfCurrentLesson();
        List<TrainingEntity> toEdit = trainingRepository
                .findByParentTrainingIdAndStartOfCurrentLessonGreaterThanEqual(parentTrainingId, startFrom);

        int interval = trainingDTO.getRepeatIntervalInDays();

        boolean hasChanged = !Objects.equals(clickedTraining.getNameOfLesson(), trainingDTO.getNameOfLesson()) ||
                !Objects.equals(clickedTraining.getStartOfCurrentLesson(), trainingDTO.getStartOfCurrentLesson()) ||
                !Objects.equals(clickedTraining.getEndOfCurrentLesson(), trainingDTO.getEndOfCurrentLesson());

        if (hasChanged) {
            clickedTraining.setParentTrainingId(null);
            trainingRepository.save(clickedTraining);

            for (int i = 0; i < toEdit.size(); i++) {
                TrainingEntity training = toEdit.get(i);
                training.setParentTrainingId(clickedTraining.getTrainingId());

                training.setNameOfLesson(trainingDTO.getNameOfLesson());
                training.setCoachName(trainingDTO.getCoachName());
                training.setNumberOfFreeSlots(trainingDTO.getNumberOfFreeSlots());

                training.setStartOfCurrentLesson(trainingDTO.getStartOfCurrentLesson().plusDays((i + 1) * interval));
                training.setEndOfCurrentLesson(trainingDTO.getEndOfCurrentLesson().plusDays((i + 1) * interval));
                training.setDateOfCurrentLesson(trainingDTO.getDateOfCurrentLesson().plusDays((i + 1) * interval));

                trainingRepository.save(training);
            }
        } else {
            for (int i = 0; i < toEdit.size(); i++) {
                TrainingEntity training = toEdit.get(i);

                training.setNameOfLesson(trainingDTO.getNameOfLesson());
                training.setCoachName(trainingDTO.getCoachName());
                training.setNumberOfFreeSlots(trainingDTO.getNumberOfFreeSlots());

                training.setStartOfCurrentLesson(trainingDTO.getStartOfCurrentLesson().plusDays((i + 1) * interval));
                training.setEndOfCurrentLesson(trainingDTO.getEndOfCurrentLesson().plusDays((i + 1) * interval));
                training.setDateOfCurrentLesson(trainingDTO.getDateOfCurrentLesson().plusDays((i + 1) * interval));

                trainingRepository.save(training);
            }
        }

        clickedTraining.setNameOfLesson(trainingDTO.getNameOfLesson());
        clickedTraining.setCoachName(trainingDTO.getCoachName());
        clickedTraining.setNumberOfFreeSlots(trainingDTO.getNumberOfFreeSlots());
        clickedTraining.setStartOfCurrentLesson(trainingDTO.getStartOfCurrentLesson());
        clickedTraining.setEndOfCurrentLesson(trainingDTO.getEndOfCurrentLesson());
        clickedTraining.setDateOfCurrentLesson(trainingDTO.getDateOfCurrentLesson());

        trainingRepository.save(clickedTraining);
    }
}







