package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.dto.ReviewUpdateRequestDto;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Review>> list(@RequestParam(required = false) String placeId,
                                             @RequestParam(required = false) String userId) {
        boolean hasPlace = placeId != null && !placeId.isBlank();
        boolean hasUser = userId != null && !userId.isBlank();

        if (hasPlace && hasUser) {
            return ResponseEntity.ok(reviewService.findByUserIdAndPlaceId(userId, placeId));
        }
        if (hasPlace) {
            return ResponseEntity.ok(reviewService.findByPlaceId(placeId));
        }
        if (hasUser) {
            return ResponseEntity.ok(reviewService.findByUserId(userId));
        }
        return ResponseEntity.ok(reviewService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Review> create(@Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Review> update(@PathVariable String id, @Valid @RequestBody ReviewUpdateRequestDto request) {
        return ResponseEntity.ok(reviewService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        reviewService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Avaliação removida com sucesso"));
    }
}
