package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.account.AccountRole;
import com.ragnarok.ragnarok_customers_training_diary.common.ForbiddenException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpointy pro statistiky. Vrací JSON do Chart.js na klientské `/analysis`
 * stránce. Klient sees jen svá data, admin overview (gym-wide) je samostatný endpoint.
 */
@RestController
@RequestMapping("/api/analysis")
public class AnalysisRestController {

    private final AnalysisService service;

    public AnalysisRestController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/total-volume")
    public List<AnalysisService.DateValuePoint> totalVolumeByDay(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.totalVolumeByDay(user, from, to);
    }

    @GetMapping("/pr-history")
    public List<AnalysisService.DateValuePoint> prHistory(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam Long catalogItemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.prHistory(user, catalogItemId, from, to);
    }

    @GetMapping("/max-weight")
    public MaxWeightResponse maxWeight(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam Long catalogItemId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return new MaxWeightResponse(service.maxWeightForExercise(user, catalogItemId, from, to));
    }

    @GetMapping("/sets-per-body-region")
    public List<AnalysisService.LabelValuePoint> setsPerBodyRegion(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.setsPerBodyRegion(user, from, to);
    }

    @GetMapping("/sets-per-movement-pattern")
    public List<AnalysisService.LabelValuePoint> setsPerMovementPattern(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.setsPerMovementPattern(user, from, to);
    }

    @GetMapping("/rpe-trend")
    public List<AnalysisService.DateValuePoint> rpeTrend(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.rpeTrend(user, from, to);
    }

    @GetMapping("/frequency-heatmap")
    public Map<LocalDate, Integer> frequencyHeatmap(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.frequencyHeatmap(user, from, to);
    }

    @GetMapping("/volume-per-difficulty")
    public List<AnalysisService.LabelValuePoint> volumePerDifficulty(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.volumePerDifficulty(user, from, to);
    }

    @GetMapping("/rpe-per-day")
    public Map<LocalDate, Short> rpePerDay(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.rpePerDay(user, from, to);
    }

    // -----------------------------------------------------------------------------
    // Admin overview
    // -----------------------------------------------------------------------------

    @GetMapping("/admin/overview")
    public AnalysisService.AdminOverview adminOverview(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (user.getRole() != AccountRole.ADMIN) {
            throw new ForbiddenException("Admin přístup vyžadován.");
        }
        return service.adminOverview(from, to);
    }

    public record MaxWeightResponse(BigDecimal maxWeightKg) {}
}
