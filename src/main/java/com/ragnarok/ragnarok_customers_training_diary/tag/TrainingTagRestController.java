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

/**
 * REST API pro tagy — list (system + own custom), create custom, delete custom.
 */
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
        TrainingTagEntity tag = service.createCustomTag(user, request.name(), request.color());
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(tag));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AccountEntity user,
            @PathVariable Long id) {
        service.deleteCustomTag(user, id);
        return ResponseEntity.noContent().build();
    }

    public record CreateTagRequest(String name, String color) {}

    public record TagResponse(Long id, String name, String color, boolean system) {
        static TagResponse from(TrainingTagEntity tag) {
            return new TagResponse(tag.getId(), tag.getName(), tag.getColor(), tag.isSystem());
        }
    }
}
