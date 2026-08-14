package com.ragnarok.ragnarok_customers_training_diary.analysis;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON pro novou výkonnostní analytiku (kolo 10). Data jsou vždy jen za přihlášeného
 * uživatele — období si volí ve filtru nahoře na stránce.
 */
@RestController
@RequestMapping("/api/analysis/performance")
public class PerformanceAnalysisRestController {

    private final PerformanceAnalysisService service;

    public PerformanceAnalysisRestController(PerformanceAnalysisService service) {
        this.service = service;
    }

    /** P58: souhrn za vybraný cvik. */
    @GetMapping("/exercise-summary")
    public PerformanceAnalysisService.ExerciseSummary exerciseSummary(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam String name,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.exerciseSummary(user, name, from, to);
    }

    /** P59 + P62: tréninky za období, volitelně filtrované na tagy tréninku. */
    @GetMapping("/trainings")
    public List<PerformanceAnalysisService.TrainingSummary> trainings(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<Long> tagIds) {
        return service.trainingSummaries(user, from, to, tagIds);
    }

    /** P60: pět výkonnostních oblastí po trénincích. */
    @GetMapping("/areas")
    public List<PerformanceAnalysisService.AreaPoint> areas(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.performanceAreas(user, from, to);
    }

    /** P61: rozpady pro koláčové, sloupcové a radar grafy. */
    @GetMapping("/distributions")
    public PerformanceAnalysisService.Distributions distributions(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.distributions(user, from, to);
    }

    /** P62: cviky, které mají VŠECHNY zvolené tagy. */
    @GetMapping("/exercises-by-tags")
    public List<PerformanceAnalysisService.ExerciseOccurrence> exercisesByTags(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Set<Long> tagIds) {
        return service.exercisesByTags(user, from, to, tagIds);
    }

    /** P63: sledované proměnné v čase. */
    @GetMapping("/variables")
    public List<PerformanceAnalysisService.VariablePoint> variables(
            @AuthenticationPrincipal AccountEntity user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.variableTrends(user, from, to);
    }
}
