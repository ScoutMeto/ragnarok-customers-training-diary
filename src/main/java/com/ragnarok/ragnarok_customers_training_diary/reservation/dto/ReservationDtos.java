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
     *
     * <p>Důležité: rezervační systém v root vrací {@code numberOfTotalFreeSlots = 0}
     * (bug nebo nepoužitý field) — skutečná kapacita a počet rezervací jsou v
     * {@link #extendedProps}. Použij convenience metody {@link #capacity()},
     * {@link #bookedSlots()}, {@link #freeSlots()}, {@link #coachName()}.
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
    ) {
        /** Jméno trenéra z {@code extendedProps.coachName}, nebo {@code null}. */
        public String coachName() {
            return extString("coachName");
        }

        /** Celková kapacita lekce z {@code extendedProps.numberOfFreeSlots}. */
        public int capacity() {
            return extInt("numberOfFreeSlots");
        }

        /** Součet {@code numberOfBookedEntries} přes všechny rezervace lekce. */
        public int bookedSlots() {
            if (reservations == null) return 0;
            return reservations.stream()
                    .mapToInt(PartialReservation::numberOfBookedEntries)
                    .sum();
        }

        /** Reálná volná místa = capacity - booked (nikdy záporné). */
        public int freeSlots() {
            return Math.max(0, capacity() - bookedSlots());
        }

        /** {@code true} pokud lekce nemá volná místa. */
        public boolean isFull() {
            return freeSlots() == 0;
        }

        // -- helpers --
        private String extString(String key) {
            if (extendedProps == null) return null;
            Object v = extendedProps.get(key);
            return v != null ? v.toString() : null;
        }
        private int extInt(String key) {
            if (extendedProps == null) return 0;
            Object v = extendedProps.get(key);
            if (v instanceof Number n) return n.intValue();
            if (v instanceof String s) {
                try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
            }
            return 0;
        }
    }

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
