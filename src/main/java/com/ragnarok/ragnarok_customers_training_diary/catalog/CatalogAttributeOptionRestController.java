package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionEntity.Kind;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 20a/b: REST API pro rozšiřitelný číselník katalogu (pohybový vzorec, náčiní).
 * Pod /api/** → CSRF-exempt (volá se z JS na katalog formuláři).
 */
@RestController
@RequestMapping("/api/catalog-options")
public class CatalogAttributeOptionRestController {

    private final CatalogAttributeOptionService service;

    public CatalogAttributeOptionRestController(CatalogAttributeOptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<OptionResponse> list(@RequestParam("kind") Kind kind) {
        return service.list(kind).stream().map(OptionResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<OptionResponse> add(@RequestBody CreateRequest request) {
        var opt = service.addCustom(request.kind(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(OptionResponse.from(opt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(Kind kind, String name) {}

    public record OptionResponse(Long id, String name, boolean system) {
        static OptionResponse from(CatalogAttributeOptionEntity e) {
            return new OptionResponse(e.getId(), e.getName(), e.isSystem());
        }
    }
}
