package com.ragnarok.ragnarok_customers_training_diary.reservation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ScoutMeto kolo 7: periodická kontrola náhradníků. Zachytí uvolnění místa, které
 * proběhlo mimo diary (admin smaže rezervaci přímo v rezervačním systému) — okamžitá
 * promoce při zrušení přes diary běží zvlášť v {@link ReservationService}.
 *
 * <p>Interval default 2 minuty; vypnutí přes {@code ragnarok.reservation.enabled=false}
 * (např. v testech) — pak job jen tiše skončí.
 */
@Component
public class WaitlistPromotionJob {

    private static final Logger log = LoggerFactory.getLogger(WaitlistPromotionJob.class);

    private final ReservationWaitlistService waitlistService;

    @Value("${ragnarok.reservation.enabled:true}")
    private boolean reservationEnabled;

    public WaitlistPromotionJob(ReservationWaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @Scheduled(fixedDelayString = "${ragnarok.reservation.waitlist-check-ms:120000}",
               initialDelayString = "${ragnarok.reservation.waitlist-initial-delay-ms:60000}")
    public void checkWaitlists() {
        if (!reservationEnabled) {
            return;
        }
        try {
            waitlistService.promoteAllPending();
        } catch (RuntimeException ex) {
            log.warn("[waitlist] periodic check failed: {}", ex.getMessage());
        }
    }
}
