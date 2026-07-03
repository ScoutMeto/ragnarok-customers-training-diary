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
    private final ReservationWaitlistService waitlistService;

    public ReservationPageController(ReservationService reservationService,
                                     ReservationWaitlistService waitlistService) {
        this.reservationService = reservationService;
        this.waitlistService = waitlistService;
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
        // ScoutMeto kolo 7: mapa lekce → moje rezervace (tlačítko Zrušit přímo u karty lekce)
        java.util.Map<Long, ReservationService.MyReservation> myByTraining = new java.util.HashMap<>();
        for (var r : mine) {
            if (!r.past() && r.trainingId() != null) {
                myByTraining.putIfAbsent(r.trainingId(), r);
            }
        }
        model.addAttribute("myByTraining", myByTraining);
        // hranice pro zobrazení tlačítka: do startu musí zbývat víc než 30 minut
        model.addAttribute("nowPlus30", java.time.LocalDateTime.now().plusMinutes(30));

        // ScoutMeto kolo 7: náhradníci — moje čekání + počty na lekci
        java.util.Map<Long, ReservationWaitlistEntity> myWaitlist = new java.util.HashMap<>();
        java.util.Map<Long, Integer> myWaitlistPosition = new java.util.HashMap<>();
        if (account != null) {
            for (var w : waitlistService.listForAccount(account.getId())) {
                if (w.getStatus() == ReservationWaitlistEntity.Status.WAITING) {
                    myWaitlist.put(w.getExtTrainingId(), w);
                    Integer pos = waitlistService.positionOf(account.getId(), w.getExtTrainingId());
                    if (pos != null) myWaitlistPosition.put(w.getExtTrainingId(), pos);
                }
            }
        }
        model.addAttribute("myWaitlist", myWaitlist);
        model.addAttribute("myWaitlistPosition", myWaitlistPosition);
        java.util.Map<Long, Long> waitlistCounts = new java.util.HashMap<>();
        for (var t : reservationService.listUpcomingTrainings(DAYS_AHEAD)) {
            if (t.isFull() && t.trainingId() != null) {
                waitlistCounts.put(t.trainingId(), waitlistService.countWaiting(t.trainingId()));
            }
        }
        model.addAttribute("waitlistCounts", waitlistCounts);
        // Admin správa rezervací: trenér vidí přihlášené klienty a může jim zrušit rezervaci
        model.addAttribute("isAdmin", account != null
                && account.getRole() == com.ragnarok.ragnarok_customers_training_diary.account.AccountRole.ADMIN);
        return "reservations/list";
    }

    /** Admin zruší rezervaci libovolného klienta (role hlídá i service vrstva). */
    @PostMapping("/reservations/admin-cancel")
    public String adminCancelReservation(@AuthenticationPrincipal AccountEntity account,
                                         @RequestParam("reservationId") Long reservationId,
                                         RedirectAttributes redirectAttributes) {
        try {
            var cancelled = reservationService.cancelReservationAsAdmin(account, reservationId);
            String client = (cancelled.clientFirstName() != null ? cancelled.clientFirstName() : "")
                    + " " + (cancelled.clientLastName() != null ? cancelled.clientLastName() : "");
            // Review fix: neslibovat promoci náhradníka, když už neproběhne (30min cutoff)
            String waitlistNote = cancelled.promotionWindowOpen()
                    ? " Uvolněné místo dostane případný první náhradník."
                    : " Do startu zbývá méně než 30 minut — náhradník se už automaticky nepovolává.";
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Rezervace klienta " + client.trim() + " na lekci „"
                            + (cancelled.title() != null ? cancelled.title() : "Lekce")
                            + "\" byla zrušena. Pokud má účet v deníku, dostal e-mail." + waitlistNote);
        } catch (ReservationException ex) {
            log.warn("[reservation] admin cancel failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("flashError", "Zrušení selhalo: " + ex.getMessage());
        } catch (com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException ex) {
            // Nemělo by nastat (URL pravidlo pouští jen ADMIN), ale konzistentní UX pro jistotu
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    // -----------------------------------------------------------------------------
    // ScoutMeto kolo 7: náhradník
    // -----------------------------------------------------------------------------

    @PostMapping("/reservations/waitlist")
    public String joinWaitlist(@AuthenticationPrincipal AccountEntity account,
                               @RequestParam("trainingId") Long trainingId,
                               RedirectAttributes redirectAttributes) {
        try {
            waitlistService.join(account, trainingId);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Jsi na seznamu náhradníků. Když se uvolní místo, vytvoříme ti rezervaci a dáme vědět e-mailem.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/waitlist/leave")
    public String leaveWaitlist(@AuthenticationPrincipal AccountEntity account,
                                @RequestParam("trainingId") Long trainingId,
                                RedirectAttributes redirectAttributes) {
        try {
            waitlistService.leave(account, trainingId);
            redirectAttributes.addFlashAttribute("flashSuccess", "Odhlásili jsme tě ze seznamu náhradníků.");
        } catch (ReservationException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/reservations";
    }

    @PostMapping("/reservations/cancel")
    public String cancelReservation(@AuthenticationPrincipal AccountEntity account,
                                    @RequestParam("reservationId") Long reservationId,
                                    RedirectAttributes redirectAttributes) {
        try {
            var cancelled = reservationService.cancelReservation(account, reservationId);
            String when = cancelled.start() != null
                    ? " dne " + cancelled.start().toLocalDate() + " v "
                        + cancelled.start().toLocalTime().toString().substring(0, 5)
                    : "";
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Rezervace na lekci „" + (cancelled.title() != null ? cancelled.title() : "Lekce")
                            + "\"" + when + " byla zrušena. Potvrzení ti přijde na e-mail.");
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
