package com.ragnarok.ragnarok_customers_training_diary.reservation;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationResponse;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.TrainingResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Business vrstva nad {@link ReservationClient}. Poskytuje UI-friendly metody
 * pro {@code ReservationPageController}.
 *
 * <p>Phase 7.1: jen veřejné funkce — listing lekcí a rezervace 1 klikem.
 * „Moje rezervace" a cancel se odkládá na Phase 7.2 (vyžaduje admin přístup).
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationClient client;

    public ReservationService(ReservationClient client) {
        this.client = client;
    }

    /**
     * Vrátí nadcházející lekce v rezervačním systému (default: dnes + 7 dní dopředu),
     * seřazené chronologicky od nejbližší. Lekce, které už proběhly (end &lt; now),
     * jsou odfiltrované.
     */
    public List<TrainingResponse> listUpcomingTrainings(int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(daysAhead).atTime(23, 59);
        LocalDateTime now = LocalDateTime.now();

        return client.listTrainings(from, to).stream()
                // Skipuj proběhlé lekce (end před teď)
                .filter(t -> t.end() == null || t.end().isAfter(now))
                .sorted(Comparator.comparing(TrainingResponse::start,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Vytvoří rezervaci na konkrétní lekci pro přihlášeného klienta. Údaje
     * (jméno, email, telefon) se předvyplní z {@link AccountEntity}.
     *
     * @param account přihlášený klient
     * @param trainingId ID lekce v rezervačním systému
     * @param slots počet míst (default 1)
     * @return ID nově vytvořené rezervace
     * @throws ReservationException při chybě (rezervace odmítnuta, systém nedostupný)
     */
    public Long createReservationForClient(AccountEntity account, Long trainingId, int slots) {
        if (account == null) {
            throw new ReservationException("Nelze rezervovat anonymně.");
        }
        if (trainingId == null) {
            throw new ReservationException("Chybí ID lekce.");
        }
        int validSlots = Math.max(1, Math.min(slots, 10));

        CreateReservationRequest req = new CreateReservationRequest(
                trainingId,
                account.getFirstName(),
                account.getLastName(),
                account.getEmail(),
                account.getPhone(),
                validSlots
        );

        CreateReservationResponse resp = client.createReservation(req);
        log.info("[reservation] created reservation_id={} for client account_id={} training={}",
                resp.reservationId(), account.getId(), trainingId);
        return resp.reservationId();
    }
}
