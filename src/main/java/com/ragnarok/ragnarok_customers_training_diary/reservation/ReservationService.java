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
    private final com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;
    private final ReservationWaitlistService waitlistService;

    public ReservationService(ReservationClient client,
            com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService,
            ReservationWaitlistService waitlistService) {
        this.client = client;
        this.emailService = emailService;
        this.waitlistService = waitlistService;
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

    // =============================================================================
    // Phase 7.2: moje rezervace + historie + zrušení
    // =============================================================================

    /** Jedna „moje rezervace" pro UI. */
    public record MyReservation(
            Long reservationId,
            Long trainingId,
            String title,
            LocalDateTime start,
            LocalDateTime end,
            int slots,
            boolean past) {}

    /**
     * Moje rezervace v okně [dnes − {@code daysBack}, dnes + {@code daysAhead}].
     * Páruje se podle jména + příjmení přihlášeného (rezervační systém v public
     * odpovědi nevrací email). Vrací nadcházející i proběhlé (historie), chronologicky.
     */
    public List<MyReservation> listMyReservations(AccountEntity account, int daysBack, int daysAhead) {
        if (account == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(daysBack).atStartOfDay();
        LocalDateTime to = today.plusDays(daysAhead).atTime(23, 59);
        LocalDateTime now = LocalDateTime.now();

        String fn = safe(account.getFirstName());
        String ln = safe(account.getLastName());

        return client.listTrainings(from, to).stream()
                .filter(t -> t.reservations() != null)
                .flatMap(t -> t.reservations().stream()
                        .filter(r -> fn.equalsIgnoreCase(safe(r.firstName()))
                                && ln.equalsIgnoreCase(safe(r.secondName())))
                        .map(r -> new MyReservation(
                                r.reservationId(), t.trainingId(), t.title(), t.start(), t.end(),
                                r.numberOfBookedEntries(),
                                t.end() != null ? t.end().isBefore(now) : (t.start() != null && t.start().isBefore(now)))))
                .sorted(Comparator.comparing(MyReservation::start,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Zruší rezervaci přihlášeného klienta. Ověří (ScoutMeto kolo 7):
     * (1) rezervace patří uživateli, (2) lekce ještě neproběhla,
     * (3) do začátku zbývá víc než 30 minut. Po zrušení pošle potvrzovací e-mail.
     *
     * @return zrušená rezervace (pro hlášku v UI s dnem a názvem lekce)
     */
    public MyReservation cancelReservation(AccountEntity account, Long reservationId) {
        if (account == null || reservationId == null) {
            throw new ReservationException("Chybí údaje pro zrušení.");
        }
        MyReservation reservation = listMyReservations(account, 365, 365).stream()
                .filter(r -> reservationId.equals(r.reservationId()))
                .findFirst()
                .orElseThrow(() -> new ReservationException(
                        "Tuto rezervaci nelze zrušit (nepatří ti, nebo už neexistuje)."));

        LocalDateTime now = LocalDateTime.now();
        if (reservation.start() != null) {
            if (reservation.start().isBefore(now)) {
                throw new ReservationException("Lekce už proběhla nebo právě probíhá — rezervaci nelze zrušit.");
            }
            if (now.plusMinutes(30).isAfter(reservation.start())) {
                throw new ReservationException(
                        "Do začátku lekce zbývá méně než 30 minut — rezervaci už nelze zrušit.");
            }
        }

        client.cancelReservation(reservationId);
        log.info("[reservation] client account_id={} cancelled reservation_id={}",
                account.getId(), reservationId);
        emailService.sendReservationCancelledNotification(account,
                reservation.title(), reservation.start());
        // Review fix: úklid PROMOTED záznamu — po zrušení se může znovu hlásit jako náhradník
        try {
            waitlistService.clearPromoted(account.getId(), reservation.trainingId());
        } catch (RuntimeException ex) {
            log.warn("[waitlist] clearPromoted failed: {}", ex.getMessage());
        }
        // ScoutMeto kolo 7: uvolnilo se místo → okamžitá promoce prvního náhradníka
        try {
            waitlistService.promoteForTraining(reservation.trainingId());
        } catch (RuntimeException ex) {
            log.warn("[waitlist] instant promotion after cancel failed: {}", ex.getMessage());
        }
        return reservation;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
