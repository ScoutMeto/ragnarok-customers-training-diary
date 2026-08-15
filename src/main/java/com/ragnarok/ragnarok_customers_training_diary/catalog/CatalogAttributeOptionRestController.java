package com.ragnarok.ragnarok_customers_training_diary.catalog;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
import com.ragnarok.ragnarok_customers_training_diary.catalog.CatalogAttributeOptionEntity.Kind;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog-options")
public class CatalogAttributeOptionRestController {

    private final CatalogAttributeOptionService service;

    public CatalogAttributeOptionRestController(CatalogAttributeOptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<OptionResponse> list(@AuthenticationPrincipal AccountEntity user, @RequestParam("kind") Kind kind) {
        return service.listVisibleTo(user, kind).stream().map(OptionResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<OptionResponse> add(
            @AuthenticationPrincipal AccountEntity user,
            @RequestBody CreateRequest request) {
        boolean createTag = request.createTag() != null
                ? request.createTag()
                : CatalogAttributeOptionService.shouldAutoCreateTag(request.kind());
        CatalogAttributeOptionService.OptionCreateResult result = service.addCustom(
                user, request.kind(), request.name(), createTag);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(OptionResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AccountEntity user, @PathVariable Long id) {
        service.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    public record CreateRequest(Kind kind, String name, Boolean createTag) {}

    public record OptionResponse(
            Long id,
            String name,
            String label,
            Kind kind,
            boolean system,
            boolean created,
            boolean duplicate,
            boolean tagCreated,
            boolean tagDuplicate) {

        static OptionResponse from(CatalogAttributeOptionEntity e) {
            return from(e, false, false, false, false);
        }

        static OptionResponse from(CatalogAttributeOptionService.OptionCreateResult result) {
            return from(result.option(), result.created(), result.duplicate(), result.tagCreated(), result.tagDuplicate());
        }

        private static OptionResponse from(CatalogAttributeOptionEntity e, boolean created, boolean duplicate,
                boolean tagCreated, boolean tagDuplicate) {
            return new OptionResponse(
                    e.getId(),
                    e.getName(),
                    e.getLabel(),
                    e.getKind(),
                    e.isSystem(),
                    created,
                    duplicate,
                    tagCreated,
                    tagDuplicate);
        }
    }
}