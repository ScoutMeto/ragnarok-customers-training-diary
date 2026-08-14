package com.ragnarok.ragnarok_customers_training_diary.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only REST API nad katalogem cviků. Slouží zejména pro JS autocomplete
 * ve formulářích deníku.
 */
@RestController
@RequestMapping("/api/exercise-catalog")
public class ExerciseCatalogRestController {

    private final ExerciseCatalogService service;

    public ExerciseCatalogRestController(ExerciseCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<CatalogItemResponse> list(@RequestParam(value = "q", required = false) String query) {
        return service.search(query).stream()
                .map(CatalogItemResponse::from)
                .toList();
    }

    public record CatalogItemResponse(
            Long id,
            String name,
            java.util.List<String> bodyRegions,
            java.util.List<String> movementPatterns,
            String primaryMuscle,
            String equipment
    ) {
        static CatalogItemResponse from(ExerciseCatalogItemEntity e) {
            return new CatalogItemResponse(
                    e.getId(),
                    e.getName(),
                    e.getBodyRegions().stream().map(Enum::name).toList(),
                    java.util.List.copyOf(e.getMovementPatterns()),
                    e.getPrimaryMuscle(),
                    e.getEquipment()
            );
        }
    }
}
