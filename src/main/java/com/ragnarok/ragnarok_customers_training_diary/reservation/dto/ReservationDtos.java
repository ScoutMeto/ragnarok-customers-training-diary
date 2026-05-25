package com.ragnarok.ragnarok_customers_training_diary.reservation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs zrcadlí struktury z rezervačního systému
 * ({@code com.matejmarek.ragnarok_customers_reservation_system.dto}).
 *
 * <p>Záměrně jen ta pole, která potřebujeme — Jackson ignoruje neznámá pole.
 */
public final class ReservationDtos {

    private ReservationDtos() {}

    // -----------------------------------------------------------------------------
    // Response: výpis lekcí (veřejný endpoint)
    // -----------------------------------------------------------------------------

    /**
     * Z rezervačního: {@code PartialTrainingResponseDTO}.
     * Vrací jen partial reservation (bez emailů a telefonů).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TrainingResponse(
            Long trainingId,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            int numberOfTotalFreeSlots,
            List<PartialReservation> reservations,
            Map<String, Object> extendedProps
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PartialReservation(
            Long reservation_id,
            Long trainingId,
            String firstName,
            String secondName,
            boolean admin,
            int numberOfBookedEntries
    ) {}

    // -----------------------------------------------------------------------------
    // Request + response: vytvoření rezervace (veřejný endpoint)
    // -----------------------------------------------------------------------------

    /**
     * Body pro {@code POST /api/createNewReservation}.
     * Mapuje se na rezervační {@code ReservationDTO} (relevantní subset polí).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CreateReservationRequest(
            Long trainingId,
            String firstName,
            String secondName,
            String userEmail,
            String telephoneNumber,
            int numberOfBookedEntries
    ) {}

    /** Odpověď rezervačního systému na vytvořenou rezervaci. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CreateReservationResponse(
            Long reservation_id,
            Long trainingId,
            String firstName,
            String secondName,
            String userEmail,
            String telephoneNumber,
            int numberOfBookedEntries
    ) {}
}
