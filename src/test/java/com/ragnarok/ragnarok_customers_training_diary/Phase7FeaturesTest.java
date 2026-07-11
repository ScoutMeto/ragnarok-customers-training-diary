package com.ragnarok.ragnarok_customers_training_diary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationClient;
import com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationException;
import com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationService;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationResponse;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.TrainingResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Smoke testy pro Phase 7.1 (rezervační integrace).
 *
 * <p>Mock {@link ReservationClient} — netestujeme reálný HTTP call, jen že
 * {@link ReservationService} správně sestaví request a propaguje odpověď.
 */
class Phase7FeaturesTest {

    private ReservationClient client;
    private ReservationService service;
    private AccountEntity account;
    private com.ragnarok.ragnarok_customers_training_diary.mail.EmailService emailService;
    private com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistService waitlistService;
    private com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository accountRepository;

    @BeforeEach
    void setup() {
        client = Mockito.mock(ReservationClient.class);
        emailService = Mockito.mock(com.ragnarok.ragnarok_customers_training_diary.mail.EmailService.class);
        waitlistService = Mockito.mock(com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistService.class);
        accountRepository = Mockito.mock(com.ragnarok.ragnarok_customers_training_diary.account.AccountRepository.class);
        service = new ReservationService(client, emailService, waitlistService, accountRepository);

        account = new AccountEntity();
        account.setId(42L);
        account.setEmail("marek@example.cz");
        account.setFirstName("Marek");
        account.setLastName("Novák");
        account.setPhone("+420777111222");
        account.setRole(AccountRole.USER);
    }

    @Test
    void listUpcoming_delegatesToClient_returnsResult() {
        TrainingResponse t = new TrainingResponse(
                1L, "KB strength", LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1), 5, List.of(),
                java.util.Map.of("numberOfFreeSlots", 8, "coachName", "Matej"));
        when(client.listTrainings(any(), any())).thenReturn(List.of(t));

        List<TrainingResponse> result = service.listUpcomingTrainings(7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("KB strength");
        assertThat(result.get(0).capacity()).isEqualTo(8);
        assertThat(result.get(0).freeSlots()).isEqualTo(8); // capacity 8 - booked 0
        assertThat(result.get(0).coachName()).isEqualTo("Matej");
        verify(client, times(1)).listTrainings(any(), any());
    }

    @Test
    void createReservation_buildsRequestFromAccountAndReturnsId() {
        // CreateReservationResponse fields: reservationId, trainingId, firstName,
        // secondName, userEmail, telephoneNumber, numberOfBookedEntries
        when(client.createReservation(any())).thenReturn(new CreateReservationResponse(
                999L, 123L, "Marek", "Novák", "marek@example.cz", "+420777111222", 1));

        Long reservationId = service.createReservationForClient(account, 123L, 1);

        assertThat(reservationId).isEqualTo(999L);

        ArgumentCaptor<CreateReservationRequest> captor = ArgumentCaptor.forClass(CreateReservationRequest.class);
        verify(client).createReservation(captor.capture());
        CreateReservationRequest sent = captor.getValue();
        assertThat(sent.trainingId()).isEqualTo(123L);
        assertThat(sent.firstName()).isEqualTo("Marek");
        assertThat(sent.secondName()).isEqualTo("Novák");
        assertThat(sent.userEmail()).isEqualTo("marek@example.cz");
        assertThat(sent.telephoneNumber()).isEqualTo("+420777111222");
        assertThat(sent.numberOfBookedEntries()).isEqualTo(1);
    }

    @Test
    void createReservation_clampsSlotsToValidRange() {
        when(client.createReservation(any())).thenReturn(new CreateReservationResponse(
                1L, 1L, "M", "N", "m@n.cz", null, 1));

        service.createReservationForClient(account, 1L, 0);   // → 1
        service.createReservationForClient(account, 1L, 99);  // → 10

        ArgumentCaptor<CreateReservationRequest> captor = ArgumentCaptor.forClass(CreateReservationRequest.class);
        verify(client, times(2)).createReservation(captor.capture());
        assertThat(captor.getAllValues().get(0).numberOfBookedEntries()).isEqualTo(1);
        assertThat(captor.getAllValues().get(1).numberOfBookedEntries()).isEqualTo(10);
    }

    @Test
    void createReservation_throwsForNullAccount() {
        assertThatThrownBy(() -> service.createReservationForClient(null, 1L, 1))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("anonymně");
    }

    @Test
    void createReservation_throwsForNullTrainingId() {
        assertThatThrownBy(() -> service.createReservationForClient(account, null, 1))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("ID lekce");
    }

    @Test
    void createReservation_secondBookingOfSameLesson_rejected() {
        // ScoutMeto kolo 8: max jedna rezervace na lekci
        when(client.listTrainings(any(), any())).thenReturn(List.of(
                trainingWithReservation(123L, 500L, "Marek", "Novák", LocalDateTime.now().plusDays(1))));

        assertThatThrownBy(() -> service.createReservationForClient(account, 123L, 1))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("už máš rezervaci");
        verify(client, times(0)).createReservation(any());
    }

    @Test
    void createReservation_propagatesClientException() {
        when(client.createReservation(any())).thenThrow(
                new ReservationException("Rezervace odmítnuta: plně obsazeno"));

        assertThatThrownBy(() -> service.createReservationForClient(account, 1L, 1))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("plně obsazeno");
    }

    // -----------------------------------------------------------------------------
    // Admin zrušení rezervace libovolného klienta
    // -----------------------------------------------------------------------------

    private AccountEntity adminAccount() {
        AccountEntity admin = new AccountEntity();
        admin.setId(1L);
        admin.setEmail("admin@admin.cz");
        admin.setFirstName("Admin");
        admin.setLastName("Trenér");
        admin.setRole(AccountRole.ADMIN);
        return admin;
    }

    private TrainingResponse trainingWithReservation(Long trainingId, Long reservationId,
            String firstName, String lastName, LocalDateTime start) {
        var reservation = new com.ragnarok.ragnarok_customers_training_diary.reservation.dto
                .ReservationDtos.PartialReservation(reservationId, trainingId, firstName, lastName, false, 1);
        return new TrainingResponse(trainingId, "KB strength", start, start.plusHours(1),
                0, List.of(reservation), java.util.Map.of("numberOfFreeSlots", 8));
    }

    @Test
    void adminCancel_byNonAdmin_forbidden() {
        assertThatThrownBy(() -> service.cancelReservationAsAdmin(account, 500L))
                .isInstanceOf(com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException.class);
        verify(client, times(0)).cancelReservation(any());
    }

    @Test
    void adminCancel_cancelsEmailsMatchedClientAndPromotesWaitlist() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        when(client.listTrainingsStrict(any(), any()))
                .thenReturn(List.of(trainingWithReservation(123L, 500L, "Alice", "Nová", start)));

        AccountEntity alice = new AccountEntity();
        alice.setId(77L);
        alice.setEmail("alice@example.cz");
        alice.setFirstName("Alice");
        alice.setLastName("Nová");
        alice.setRole(AccountRole.USER);
        when(accountRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc())
                .thenReturn(List.of(alice));

        var cancelled = service.cancelReservationAsAdmin(adminAccount(), 500L);

        assertThat(cancelled.clientFirstName()).isEqualTo("Alice");
        assertThat(cancelled.clientLastName()).isEqualTo("Nová");
        assertThat(cancelled.trainingId()).isEqualTo(123L);
        assertThat(cancelled.promotionWindowOpen()).isTrue(); // 2 h do startu > 30min cutoff
        verify(client).cancelReservation(500L);
        verify(emailService).sendReservationCancelledByAdminNotification(eq(alice), eq("KB strength"), eq(start));
        verify(waitlistService).removeEntry(77L, 123L);
        verify(waitlistService).promoteForTraining(123L);
    }

    @Test
    void adminCancel_noMatchingAccount_stillCancelsAndPromotes() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        when(client.listTrainingsStrict(any(), any()))
                .thenReturn(List.of(trainingWithReservation(123L, 500L, "Externí", "Host", start)));
        when(accountRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc())
                .thenReturn(List.of());

        service.cancelReservationAsAdmin(adminAccount(), 500L);

        verify(client).cancelReservation(500L);
        verify(emailService, times(0)).sendReservationCancelledByAdminNotification(any(), any(), any());
        verify(waitlistService).promoteForTraining(123L);
    }

    @Test
    void adminCancel_startedLesson_rejected() {
        when(client.listTrainingsStrict(any(), any())).thenReturn(List.of(
                trainingWithReservation(123L, 500L, "Alice", "Nová", LocalDateTime.now().minusMinutes(10))));

        assertThatThrownBy(() -> service.cancelReservationAsAdmin(adminAccount(), 500L))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("proběhla");
        verify(client, times(0)).cancelReservation(any());
    }

    @Test
    void adminCancel_unknownReservation_rejected() {
        when(client.listTrainingsStrict(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.cancelReservationAsAdmin(adminAccount(), 999L))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("nenalezena");
        verify(client, times(0)).cancelReservation(any());
    }

    @Test
    void adminCancel_reservationSystemDown_reportsOutageNotMissing() {
        // Review fix: výpadek systému nesmí skončit hláškou „rezervace nenalezena"
        when(client.listTrainingsStrict(any(), any()))
                .thenThrow(new ReservationException("Rezervační systém je nedostupný. Zkus to za chvíli."));

        assertThatThrownBy(() -> service.cancelReservationAsAdmin(adminAccount(), 500L))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("nedostupný");
        verify(client, times(0)).cancelReservation(any());
    }

    @Test
    void adminCancel_lessThan30MinBeforeStart_promotionWindowClosed() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(15);
        when(client.listTrainingsStrict(any(), any()))
                .thenReturn(List.of(trainingWithReservation(123L, 500L, "Alice", "Nová", start)));
        when(accountRepository.findByDeletedAtIsNullOrderByLastNameAscFirstNameAsc())
                .thenReturn(List.of());

        var cancelled = service.cancelReservationAsAdmin(adminAccount(), 500L);

        // admin zrušit smí (bez 30min pravidla), ale flash nesmí slibovat promoci
        assertThat(cancelled.promotionWindowOpen()).isFalse();
        verify(client).cancelReservation(500L);
    }

    @Test
    void manualBooking_removesWaitingEntry() {
        // Review fix: booked + WAITING nesmí koexistovat — po ručním zápisu úklid waitlistu
        when(client.createReservation(any())).thenReturn(new CreateReservationResponse(
                999L, 123L, "Marek", "Novák", "marek@example.cz", "+420777111222", 1));

        service.createReservationForClient(account, 123L, 1);

        verify(waitlistService).removeWaiting(42L, 123L);
    }

    @Test
    void promotion_skipsCandidateWhoAlreadyBooked() {
        // Review fix: náhradník, který se mezitím zapsal ručně, se nesmí promovat
        // (duplicitní rezervace) — jeho WAITING záznam se smaže a promuje se další.
        var repository = Mockito.mock(
                com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistRepository.class);
        var waitlist = new com.ragnarok.ragnarok_customers_training_diary.reservation
                .ReservationWaitlistService(repository, client, emailService);

        LocalDateTime start = LocalDateTime.now().plusHours(2);
        // kapacita 2, Bob už má rezervaci → 1 volné místo
        var bobReservation = new com.ragnarok.ragnarok_customers_training_diary.reservation.dto
                .ReservationDtos.PartialReservation(600L, 123L, "Bob", "Zapsaný", false, 1);
        TrainingResponse training = new TrainingResponse(123L, "KB strength", start, start.plusHours(1),
                1, List.of(bobReservation), java.util.Map.of("numberOfFreeSlots", 2));
        when(client.listTrainings(any(), any())).thenReturn(List.of(training));

        var bobEntry = waitlistEntry(10L, "Bob", "Zapsaný", "bob@example.cz");
        var cyrilEntry = waitlistEntry(11L, "Cyril", "Čekal", "cyril@example.cz");
        when(repository.findByExtTrainingIdAndStatusOrderByCreatedAtAscIdAsc(
                eq(123L), eq(com.ragnarok.ragnarok_customers_training_diary.reservation
                        .ReservationWaitlistEntity.Status.WAITING)))
                .thenReturn(List.of(bobEntry, cyrilEntry));
        when(client.createReservation(any())).thenReturn(new CreateReservationResponse(
                700L, 123L, "Cyril", "Čekal", "cyril@example.cz", null, 1));

        waitlist.promoteForTraining(123L);

        verify(repository).delete(bobEntry); // Bobův zbytkový WAITING pryč, bez promoce
        ArgumentCaptor<CreateReservationRequest> captor =
                ArgumentCaptor.forClass(CreateReservationRequest.class);
        verify(client, times(1)).createReservation(captor.capture());
        assertThat(captor.getValue().firstName()).isEqualTo("Cyril"); // promován až Cyril
    }

    private com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistEntity
            waitlistEntry(Long accountId, String fn, String ln, String email) {
        AccountEntity acc = new AccountEntity();
        acc.setId(accountId);
        acc.setFirstName(fn);
        acc.setLastName(ln);
        acc.setEmail(email);
        acc.setRole(AccountRole.USER);
        var entry = new com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistEntity();
        entry.setAccount(acc);
        entry.setExtTrainingId(123L);
        return entry;
    }
}
