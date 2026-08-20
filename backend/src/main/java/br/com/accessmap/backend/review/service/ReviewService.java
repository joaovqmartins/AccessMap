package br.com.accessmap.backend.review.service;

import br.com.accessmap.backend.identity.service.UserService;
import br.com.accessmap.backend.place.service.PlaceService;
import br.com.accessmap.backend.review.dto.ReviewRequestDto;
import br.com.accessmap.backend.review.model.Review;
import br.com.accessmap.backend.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceService placeService;
    private final UserService userService;

    public List<Review> findByPlaceId(String placeId) {
        return reviewRepository.findByPlaceId(placeId);
    }

    public Review findById(String id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação não encontrada"));
    }

    public Review create(ReviewRequestDto request) {
        userService.findById(request.getUserId());
        placeService.findOrCreateByPlaceId(request.getPlaceId());

        Review review = Review.builder()
                .userId(request.getUserId())
                .placeId(request.getPlaceId())
                .rating(request.getRating())
                .comment(request.getComment())
                .tags(request.getTags())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepository.save(review);
        placeService.registerNewReview(request.getPlaceId(), request.getRating());

        return saved;
    }
}
