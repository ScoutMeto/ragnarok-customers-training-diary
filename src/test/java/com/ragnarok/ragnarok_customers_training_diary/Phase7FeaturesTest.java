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

    @BeforeEach
    void setup() {
        client = Mockito.mock(ReservationClient.class);
        service = new ReservationService(client,
                Mockito.mock(com.ragnarok.ragnarok_customers_training_diary.mail.EmailService.class),
                Mockito.mock(com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistService.class));

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
    void createReservation_propagatesClientException() {
        when(client.createReservation(any())).thenThrow(
                new ReservationException("Rezervace odmítnuta: plně obsazeno"));

        assertThatThrownBy(() -> service.createReservationForClient(account, 1L, 1))
                .isInstanceOf(ReservationException.class)
                .hasMessageContaining("plně obsazeno");
    }
}
