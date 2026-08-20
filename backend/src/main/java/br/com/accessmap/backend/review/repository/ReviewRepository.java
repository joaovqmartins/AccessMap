package br.com.accessmap.backend.review.repository;

import br.com.accessmap.backend.review.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, String> {
    List<Review> findByPlaceId(String placeId);
}
