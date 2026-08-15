package com.ragnarok.ragnarok_customers_training_diary.tag;

import com.ragnarok.ragnarok_customers_training_diary.account.AccountEntity;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TrainingTagRestController {

    private final TrainingTagService service;

    public TrainingTagRestController(TrainingTagService service) {
        this.service = service;
    }

    @GetMapping
    public List<TagResponse> listVisible(@AuthenticationPrincipal AccountEntity user) {
        return service.findVisibleTo(user).stream().map(TagResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TagResponse> createCustom(
            @AuthenticationPrincipal AccountEntity user,
            @RequestBody CreateTagRequest request) {
        TrainingTagService.TagCreateResult result = service.createCustomTag(
                user, request.name(), request.color(), request.category());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TagResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id) {
        service.deleteCustomTag(user, id);
        return ResponseEntity.noContent().build();
    }

    public record CreateTagRequest(String name, String color, TagCategory category) {}

    public record TagResponse(
            Long id,
            String name,
            String color,
            boolean system,
            String systemKey,
            TagCategory category,
            String categoryLabel,
            boolean created,
            boolean duplicate,
            boolean catalogOptionCreated) {

        static TagResponse from(TrainingTagEntity tag) {
            return from(tag, false, false, false);
        }

        static TagResponse from(TrainingTagService.TagCreateResult result) {
            return from(result.tag(), result.created(), result.duplicate(), result.catalogOptionCreated());
        }

        private static TagResponse from(TrainingTagEntity tag, boolean created, boolean duplicate,
                boolean catalogOptionCreated) {
            TagCategory category = tag.getCategory() != null ? tag.getCategory() : TagCategory.GENERAL;
            return new TagResponse(
                    tag.getId(),
                    tag.getName(),
                    tag.getColor(),
                    tag.isSystem(),
                    tag.getSystemKey(),
                    category,
                    category.getLabel(),
                    created,
                    duplicate,
                    catalogOptionCreated);
        }
    }
}