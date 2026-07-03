package com.ragnarok.ragnarok_customers_training_diary;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationService;
import com.ragnarok.ragnarok_customers_training_diary.reservation.ReservationWaitlistService;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.PartialReservation;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.TrainingResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Render test admin správy rezervací na stránce {@code /reservations}:
 * admin vidí seznam přihlášených klientů + tlačítko Zrušit, běžný uživatel ne.
 *
 * <p>{@link ReservationService} je mock — netestujeme HTTP na rezervační systém,
 * jen Thymeleaf render admin bloku.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReservationPageTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private ReservationWaitlistService waitlistService;

    private AccountEntity admin;
    private AccountEntity user;

    @BeforeEach
    void setup() {
        admin = account(1L, "admin@admin.cz", "Admin", "Trenér", AccountRole.ADMIN);
        user = account(2L, "user@example.cz", "Marek", "Novák", AccountRole.USER);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TrainingResponse lesson = new TrainingResponse(
                123L, "KB strength", start, start.plusHours(1), 7,
                List.of(new PartialReservation(500L, 123L, "Alice", "Nová", false, 1)),
                Map.of("numberOfFreeSlots", 8, "coachName", "Matej"));

        when(reservationService.listUpcomingTrainings(anyInt())).thenReturn(List.of(lesson));
        when(reservationService.listMyReservations(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(waitlistService.listForAccount(any())).thenReturn(List.of());
    }

    private static AccountEntity account(Long id, String email, String fn, String ln, AccountRole role) {
        AccountEntity a = new AccountEntity();
        a.setId(id);
        a.setEmail(email);
        a.setFirstName(fn);
        a.setLastName(ln);
        a.setRole(role);
        a.setEmailConfirmed(true);
        return a;
    }

    @Test
    void admin_seesBookedClientsAndCancelButton() throws Exception {
        mockMvc.perform(get("/reservations").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Přihlášení (")))
                .andExpect(content().string(containsString("Alice Nová")))
                .andExpect(content().string(containsString("/reservations/admin-cancel")));
    }

    @Test
    void regularUser_doesNotSeeAdminSection() throws Exception {
        mockMvc.perform(get("/reservations").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Přihlášení ("))))
                .andExpect(content().string(not(containsString("/reservations/admin-cancel"))));
    }
}
