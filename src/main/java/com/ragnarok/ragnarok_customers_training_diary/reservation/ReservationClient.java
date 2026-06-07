package com.ragnarok.ragnarok_customers_training_diary.reservation;

import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationRequest;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.CreateReservationResponse;
import com.ragnarok.ragnarok_customers_training_diary.reservation.dto.ReservationDtos.TrainingResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * HTTP klient pro externí rezervační systém. Volá pouze <strong>veřejné</strong>
 * endpointy (anonymous, žádná auth) — listing lekcí a vytvoření rezervace.
 *
 * <p>Implementováno přes Spring {@link RestClient} (Spring 6+ native).
 */
@Component
public class ReservationClient {

    private static final Logger log = LoggerFactory.getLogger(ReservationClient.class);
    private static final DateTimeFormatter ISO_OFFSET =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ReservationProperties properties;
    private final RestClient httpClient;

    public ReservationClient(ReservationProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        int timeoutMs = (int) Duration.ofSeconds(properties.getTimeoutSeconds()).toMillis();
        rf.setConnectTimeout(timeoutMs);
        rf.setReadTimeout(timeoutMs);

        this.httpClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(rf)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    // =============================================================================
    // Veřejné API rezervačního systému
    // =============================================================================

    /**
     * Vrátí všechny tréninky v daném období. Volá veřejný endpoint
     * {@code GET /api/loadAllTrainingsWithoutAuthorization?start={iso}&end={iso}}.
     *
     * @param from start období (inclusive)
     * @param to end období (exclusive je v reservation systému, ale akceptujeme oboje)
     * @return seznam tréninků (nebo prázdný list při výpadku — diary nesmí spadnout)
     */
    public List<TrainingResponse> listTrainings(LocalDateTime from, LocalDateTime to) {
        if (!properties.isEnabled()) {
            log.debug("[reservation] disabled, returning empty list");
            return Collections.emptyList();
        }
        try {
            // Rezervační systém očekává ISO offset datetime (např. 2026-05-25T00:00:00+02:00).
            // Pošleme s UTC offsetem (Z).
            String startIso = from.atOffset(ZoneOffset.UTC).format(ISO_OFFSET);
            String endIso = to.atOffset(ZoneOffset.UTC).format(ISO_OFFSET);

            List<TrainingResponse> result = httpClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/loadAllTrainingsWithoutAuthorization")
                            .queryParam("start", startIso)
                            .queryParam("end", endIso)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TrainingResponse>>() {});

            return result != null ? result : Collections.emptyList();
        } catch (HttpStatusCodeException ex) {
            log.warn("[reservation] listTrainings HTTP {} from {} to {}: {}",
                    ex.getStatusCode(), from, to, ex.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (ResourceAccessException ex) {
            log.warn("[reservation] listTrainings network error: {}", ex.getMessage());
            return Collections.emptyList();
        } catch (Exception ex) {
            log.error("[reservation] listTrainings unexpected error", ex);
            return Collections.emptyList();
        }
    }

    /**
     * Vytvoří novou rezervaci na lekci. Volá veřejný endpoint
     * {@code POST /api/createNewReservation}.
     *
     * @return potvrzení rezervace s ID
     * @throws ReservationException při HTTP chybě, timeoutu nebo network problému
     */
    public CreateReservationResponse createReservation(CreateReservationRequest request) {
        if (!properties.isEnabled()) {
            throw new ReservationException("Rezervační systém je dočasně vypnutý.");
        }
        try {
            CreateReservationResponse response = httpClient.post()
                    .uri("/api/createNewReservation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CreateReservationResponse.class);

            if (response == null) {
                throw new ReservationException("Rezervační systém vrátil prázdnou odpověď.");
            }
            log.info("[reservation] created reservation id={} for training={} email={}",
                    response.reservationId(), response.trainingId(), request.userEmail());
            return response;
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            String body = ex.getResponseBodyAsString();
            log.warn("[reservation] createReservation HTTP {}: {}", status, body);
            if (status == HttpStatus.CONFLICT || status == HttpStatus.BAD_REQUEST) {
                throw new ReservationException("Rezervace odmítnuta: " + body);
            }
            throw new ReservationException("Rezervační systém vrátil chybu " + status);
        } catch (ResourceAccessException ex) {
            log.warn("[reservation] createReservation network error: {}", ex.getMessage());
            throw new ReservationException("Rezervační systém je nedostupný. Zkus to za chvíli.");
        } catch (Exception ex) {
            log.error("[reservation] createReservation unexpected error", ex);
            throw new ReservationException("Něco se pokazilo při komunikaci s rezervačním systémem.");
        }
    }

    /**
     * Phase 7.2: zruší rezervaci. Volá keyed endpoint
     * {@code DELETE /api/cancelReservationForClient/{id}} se sdíleným klíčem v hlavičce X-Api-Key.
     *
     * @throws ReservationException při chybě
     */
    public void cancelReservation(Long reservationId) {
        if (!properties.isEnabled()) {
            throw new ReservationException("Rezervační systém je dočasně vypnutý.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ReservationException("Zrušení rezervace není nakonfigurováno (chybí API klíč).");
        }
        try {
            httpClient.delete()
                    .uri("/api/cancelReservationForClient/{id}", reservationId)
                    .header("X-Api-Key", properties.getApiKey())
                    .retrieve()
                    .toBodilessEntity();
            log.info("[reservation] cancelled reservation_id={}", reservationId);
        } catch (HttpStatusCodeException ex) {
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            log.warn("[reservation] cancelReservation HTTP {}: {}", status, ex.getResponseBodyAsString());
            if (status == HttpStatus.FORBIDDEN) {
                throw new ReservationException("Zrušení rezervace odmítnuto (neplatný klíč).");
            }
            if (status == HttpStatus.NOT_FOUND) {
                throw new ReservationException("Rezervace už neexistuje.");
            }
            throw new ReservationException("Rezervační systém vrátil chybu " + status);
        } catch (ResourceAccessException ex) {
            log.warn("[reservation] cancelReservation network error: {}", ex.getMessage());
            throw new ReservationException("Rezervační systém je nedostupný. Zkus to za chvíli.");
        } catch (Exception ex) {
            log.error("[reservation] cancelReservation unexpected error", ex);
            throw new ReservationException("Něco se pokazilo při rušení rezervace.");
        }
    }
}
