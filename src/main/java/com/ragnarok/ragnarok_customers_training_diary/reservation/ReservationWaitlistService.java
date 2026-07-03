package com.ragnarok.ragnarok_customers_training_diary.reservation;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.mail.EmailService;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.TrainingResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ScoutMeto kolo 7: Náhradník na plnou lekci. Waitlist žije celý v diary — rezervační
 * systém nevyžaduje žádnou změnu.
 *
 * <ul>
 *   <li>{@link #join} — přihlášení jako náhradník (jen plná, neproběhlá lekce; max
 *       tolik náhradníků, kolik je kapacita lekce).</li>
 *   <li>{@link #leave} — odhlášení z náhradníků.</li>
 *   <li>{@link #promoteForTraining} — FIFO promoce: dokud je volné místo a čeká
 *       náhradník, vytvoří mu rezervaci přes veřejné API + pošle e-mail. Volá se
 *       okamžitě po zrušení přes diary a periodicky jobem (zachytí i uvolnění
 *       adminem přímo v rezervačním systému).</li>
 * </ul>
 */
@Service
@Transactional
public class ReservationWaitlistService {

    private static final Logger log = LoggerFactory.getLogger(ReservationWaitlistService.class);

    private final ReservationWaitlistRepository repository;
    private final ReservationClient client;
    private final EmailService emailService;

    public ReservationWaitlistService(ReservationWaitlistRepository repository,
                                      ReservationClient client,
                                      EmailService emailService) {
        this.repository = repository;
        this.client = client;
        this.emailService = emailService;
    }

    /**
     * Přihlásí uživatele jako náhradníka na plnou lekci.
     * {@code synchronized}: single-instance deployment — serializuje souběžné joiny,
     * aby dva klienti naráz nepřekročili limit náhradníků (count → save není atomické).
     */
    public synchronized void join(AccountEntity account, Long extTrainingId) {
        TrainingResponse training = findTraining(extTrainingId)
                .orElseThrow(() -> new ReservationException("Lekce nenalezena."));
        if (training.start() != null && training.start().isBefore(LocalDateTime.now())) {
            throw new ReservationException("Lekce už začala — náhradníci se nepřihlašují.");
        }
        if (training.freeSlots() > 0) {
            throw new ReservationException("Na lekci je volno — zapiš se normálně.");
        }
        // Review fix: kdo už na lekci rezervaci má, náhradníka nepotřebuje (jinak by mu
        // promoce po vlastním zrušení vytvořila rezervaci znovu). Párování dle jména —
        // stejně jako „moje rezervace" (public API nevrací email).
        boolean alreadyBooked = training.reservations() != null && training.reservations().stream()
                .anyMatch(r -> safeEquals(r.firstName(), account.getFirstName())
                        && safeEquals(r.secondName(), account.getLastName()));
        if (alreadyBooked) {
            throw new ReservationException("Na tuto lekci už máš rezervaci — náhradníka nepotřebuješ.");
        }
        long waiting = repository.countByExtTrainingIdAndStatus(
                extTrainingId, ReservationWaitlistEntity.Status.WAITING);
        int capacity = training.capacity();
        if (capacity > 0 && waiting >= capacity) {
            throw new ReservationException("Seznam náhradníků je plný (max " + capacity + ").");
        }
        Optional<ReservationWaitlistEntity> existing =
                repository.findByAccount_IdAndExtTrainingId(account.getId(), extTrainingId);
        if (existing.isPresent()) {
            throw new ReservationException(existing.get().getStatus() == ReservationWaitlistEntity.Status.WAITING
                    ? "Už jsi na seznamu náhradníků."
                    : "Už jsi z náhradníků dostal(a) rezervaci.");
        }

        ReservationWaitlistEntity entry = new ReservationWaitlistEntity();
        entry.setAccount(account);
        entry.setExtTrainingId(extTrainingId);
        entry.setTrainingTitle(training.title());
        entry.setTrainingStart(training.start());
        repository.save(entry);
        log.info("[waitlist] account_id={} joined waitlist for training={}", account.getId(), extTrainingId);
    }

    /** Odhlásí uživatele z náhradníků (smaže WAITING záznam). */
    public void leave(AccountEntity account, Long extTrainingId) {
        ReservationWaitlistEntity entry = repository
                .findByAccount_IdAndExtTrainingId(account.getId(), extTrainingId)
                .orElseThrow(() -> new ReservationException("Nejsi na seznamu náhradníků."));
        if (entry.getStatus() != ReservationWaitlistEntity.Status.WAITING) {
            throw new ReservationException("Z náhradníků už máš rezervaci — zruš rovnou rezervaci.");
        }
        repository.delete(entry);
        log.info("[waitlist] account_id={} left waitlist for training={}", account.getId(), extTrainingId);
    }

    /** Stejný cutoff jako 30min pravidlo pro zrušení rezervace. */
    private static final java.time.Duration PROMOTION_CUTOFF = java.time.Duration.ofMinutes(30);

    /**
     * FIFO promoce náhradníků na dané lekci: dokud je volné místo a někdo čeká,
     * vytvoří rezervaci + pošle e-mail. Bezpečné volat opakovaně (idempotentní).
     * {@code synchronized}: promoci volá souběžně request thread (okamžitě po zrušení)
     * i periodický job — serializace brání dvojité rezervaci téhož náhradníka
     * (single-instance deployment; při škálování nahradit DB zámkem/podmíněným UPDATE).
     */
    public synchronized void promoteForTraining(Long extTrainingId) {
        Optional<TrainingResponse> trainingOpt = findTraining(extTrainingId);
        if (trainingOpt.isEmpty()) {
            return; // lekce už není v okně / smazána
        }
        TrainingResponse training = trainingOpt.get();
        // Review fix: nepromovat < 30 min před startem — promovaný by rezervaci už
        // nemohl zrušit (30min pravidlo), auto-rezervace bez možnosti couvnout je past.
        if (training.start() != null
                && LocalDateTime.now().plus(PROMOTION_CUTOFF).isAfter(training.start())) {
            return;
        }
        int free = training.freeSlots();
        if (free <= 0) {
            return;
        }
        List<ReservationWaitlistEntity> waiting = repository
                .findByExtTrainingIdAndStatusOrderByCreatedAtAscIdAsc(
                        extTrainingId, ReservationWaitlistEntity.Status.WAITING);
        for (ReservationWaitlistEntity entry : waiting) {
            if (free <= 0) break;
            AccountEntity acc = entry.getAccount();
            try {
                client.createReservation(new CreateReservationRequest(
                        extTrainingId, acc.getFirstName(), acc.getLastName(),
                        acc.getEmail(), acc.getPhone(), 1));
                entry.setStatus(ReservationWaitlistEntity.Status.PROMOTED);
                entry.setPromotedAt(LocalDateTime.now());
                repository.save(entry);
                free--;
                emailService.sendWaitlistPromotedNotification(acc, training.title(), training.start());
                log.info("[waitlist] promoted account_id={} to training={}", acc.getId(), extTrainingId);
            } catch (ReservationException ex) {
                // Lekce se mezitím zaplnila / API chyba — náhradník zůstává v pořadí
                log.warn("[waitlist] promotion failed for account_id={} training={}: {}",
                        acc.getId(), extTrainingId, ex.getMessage());
                break;
            }
        }
    }

    /** Kontrolní průchod všech lekcí s čekajícími náhradníky (volá periodický job). */
    public void promoteAllPending() {
        List<Long> trainingIds = repository.findTrainingIdsWithWaiting(
                ReservationWaitlistEntity.Status.WAITING, LocalDateTime.now());
        for (Long id : trainingIds) {
            try {
                promoteForTraining(id);
            } catch (RuntimeException ex) {
                log.warn("[waitlist] check failed for training={}: {}", id, ex.getMessage());
            }
        }
    }

    /** Moje waitlist záznamy (mapa pro UI: lekce → stav). */
    @Transactional(readOnly = true)
    public List<ReservationWaitlistEntity> listForAccount(Long accountId) {
        return repository.findByAccount_Id(accountId);
    }

    /** Počet čekajících náhradníků na lekci. */
    @Transactional(readOnly = true)
    public long countWaiting(Long extTrainingId) {
        return repository.countByExtTrainingIdAndStatus(
                extTrainingId, ReservationWaitlistEntity.Status.WAITING);
    }

    /** Pořadí uživatele mezi čekajícími (1-based); null, když nečeká. */
    @Transactional(readOnly = true)
    public Integer positionOf(Long accountId, Long extTrainingId) {
        List<ReservationWaitlistEntity> waiting = repository
                .findByExtTrainingIdAndStatusOrderByCreatedAtAscIdAsc(
                        extTrainingId, ReservationWaitlistEntity.Status.WAITING);
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i).getAccount().getId().equals(accountId)) {
                return i + 1;
            }
        }
        return null;
    }

    /**
     * Review fix: po zrušení rezervace smaže případný PROMOTED záznam uživatele pro lekci —
     * uvolní UNIQUE constraint, aby se mohl znovu přihlásit jako náhradník.
     */
    public void clearPromoted(Long accountId, Long extTrainingId) {
        repository.findByAccount_IdAndExtTrainingId(accountId, extTrainingId)
                .filter(e -> e.getStatus() == ReservationWaitlistEntity.Status.PROMOTED)
                .ifPresent(repository::delete);
    }

    /** Najde lekci v rezervačním systému podle ID (okno dnes → +60 dní). */
    private Optional<TrainingResponse> findTraining(Long extTrainingId) {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(60).atTime(23, 59);
        return client.listTrainings(from, to).stream()
                .filter(t -> extTrainingId.equals(t.trainingId()))
                .findFirst();
    }

    private static boolean safeEquals(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
