package com.ragnarok.ragnarok_customers_training_diary.controller;

import com.ragnarok.ragnarok_customers_training_diary.dto.AimedTrainingsRequestDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingDTO;
import com.ragnarok.ragnarok_customers_training_diary.dto.TrainingResponseDTO;
import com.ragnarok.ragnarok_customers_training_diary.entity.repository.TrainingRepository;
import com.ragnarok.ragnarok_customers_training_diary.service.AdminService;
import com.ragnarok.ragnarok_customers_training_diary.service.TrainingService;
import io.swagger.annotations.Api;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.Setter;
import lombok.Getter;

@Setter
@Getter
@Api
@RestController
public class TrainingController {

    @Autowired
    TrainingService trainingService;
    @Autowired
    TrainingRepository trainingRepository;
    @Autowired
    AdminService adminService;


    @PostMapping({"api/createNewTraining/", "api/createNewTraining"})
    public ResponseEntity<?> addTraining (@RequestBody @Valid TrainingDTO trainingDTO) {
        return ResponseEntity.ok("Trénink vytvořen");
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping({"api/loadAllTrainings/", "api/loadAllTrainings"})
    public List<TrainingResponseDTO> getTrainingsForCalendar(

            @RequestParam("start") OffsetDateTime start,
            @RequestParam("end") OffsetDateTime end) {

        LocalDateTime startDate = start.toLocalDateTime();
        LocalDateTime endDate = end.toLocalDateTime();


        System.out.println("Backend DEBUG: start=" + startDate + ", end=" + endDate);
        return trainingService.getAllTrainingsAsCalendarEvents(startDate, endDate);
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping({"api/loadOneTraining/{id}/", "api/loadOneTraining/{id}"})
    public TrainingDTO getOneTraining(@PathVariable("id") Long trainingId) {
        /*
        Nalézá všechny rezervace podle ID a vkládá je do Listu k TrainingEntity
        (použito pro přehled: kdo je přihlášen na trénink, kolik míst je obsazených)
         -vyřešeno načítáním EAGER
         */
        return trainingService.getOneTrainingById(trainingId);

    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @DeleteMapping({"api/deleteTrainingChosenInOverview/{id}/", "api/deleteTrainingChosenInOverview/{id}"})
    public ResponseEntity<Void> removeTraining(@PathVariable("id") Long trainingId) {
        trainingService.removeOneTraining(trainingId);
        return ResponseEntity.noContent().build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @DeleteMapping({"api/deleteTrainingsChosenInOverview/{id}/", "api/deleteTrainingsChosenInOverview/{id}"})
    public ResponseEntity<Void> removeTrainings(@PathVariable("id") Long trainingId) {
        trainingService.removeAllPlannedTrainings(trainingId);
        return ResponseEntity.noContent().build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    @DeleteMapping({"api/deleteAimedTrainingsChosenInOverview/", "api/deleteAimedTrainingsChosenInOverview"})
    public ResponseEntity<Void> removeAimedTrainings(@RequestBody AimedTrainingsRequestDTO amimedTrainingsRequestDTO) {
        trainingService.removeAimedTrainings(amimedTrainingsRequestDTO.getTrainingIds());
        return ResponseEntity.noContent().build();
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    // PUT metoda pro úpravu jednoho vybraného tréninku
    @PutMapping({"api/editTrainingChosenInOverview/{trainingId}/", "api/editTrainingChosenInOverview/{trainingId}"})
    public TrainingDTO editTraining (@PathVariable("trainingId") Long trainingId, @RequestBody TrainingDTO trainingDTO) {
        return trainingService.editOneTraining(trainingId, trainingDTO);
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    // PUT metoda pro úpravu všech následujících tréninků, včetně aktuálně vybraného
    @PutMapping({"api/editAllPlanned/{trainingId}/", "api/editAllPlanned/{trainingId}"})
    public ResponseEntity<Void> editAllPlannedTrainings(@PathVariable("trainingId") Long trainingId, @RequestBody TrainingDTO trainingDTO) {
        trainingService.editAllPlannedTrainings(trainingId, trainingDTO);
        return ResponseEntity.noContent().build();  // HTTP 204 No Content pokud úprava proběhne úspěšně
    }
}
