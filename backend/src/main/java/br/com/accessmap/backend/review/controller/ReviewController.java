package br.com.accessmap.backend.review.controller;

import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Review>> listByPlace(@RequestParam String placeId) {
        return ResponseEntity.ok(reviewService.findByPlaceId(placeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Review> create(@Valid @RequestBody ReviewRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }
}
