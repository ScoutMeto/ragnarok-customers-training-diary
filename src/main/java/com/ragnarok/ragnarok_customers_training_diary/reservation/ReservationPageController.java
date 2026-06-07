package com.ragnarok.ragnarok_customers_training_diary.reservation;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Thymeleaf stránka {@code /reservations} — kalendář nadcházejících lekcí
 * z externího rezervačního systému + tlačítko „Rezervovat 1 klikem".
 *
 * <p>Vyžaduje přihlášení (security default).
 */
@Controller
public class ReservationPageController {

    private static final Logger log = LoggerFactory.getLogger(ReservationPageController.class);
    private static final int DAYS_AHEAD = 7;

    private final ReservationService reservationService;

    public ReservationPageController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/reservations")
    public String listUpcoming(@AuthenticationPrincipal AccountEntity account, Model model) {
        model.addAttribute("trainings", reservationService.listUpcomingTrainings(DAYS_AHEAD));
        model.addAttribute("daysAhead", DAYS_AHEAD);
        model.addAttribute("currentEmail", account != null ? account.getEmail() : null);
        // Phase 7.2: moje rezervace — nadcházející + historie (párování dle jména)
        var mine = reservationService.listMyReservations(account, 90, 60);
        model.addAttribute("myUpcoming", mine.stream().filter(r -> !r.past()).toList());
        model.addAttribute("myPast", mine.stream().filter(r -> r.past()).toList());
        return "reservations/list";
    }

    @PostMapping("/reservations/cancel")
    public String cancelReservation(@AuthenticationPrincipal AccountEntity account,
                                    @RequestParam("reservationId") Long reservationId,
                                    RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservation(account, reservationId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Rezervace zrušena.");
        } catch (ReservationException ex) {
            log.warn("[reservation] cancel failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("flashError", "Zrušení selhalo: " + ex.getMessage());
        }
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/book")
    public String bookTraining(@AuthenticationPrincipal AccountEntity account,
                                @RequestParam("trainingId") Long trainingId,
                                @RequestParam(value = "slots", defaultValue = "1") int slots,
                                RedirectAttributes redirectAttributes) {
        try {
            Long reservationId = reservationService.createReservationForClient(account, trainingId, slots);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "✅ Rezervace vytvořena (ID: " + reservationId + "). Těšíme se na tebe!");
        } catch (ReservationException ex) {
            log.warn("[reservation] book failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("flashError",
                    "Rezervace selhala: " + ex.getMessage());
        }
        return "redirect:/reservations";
    }
}
