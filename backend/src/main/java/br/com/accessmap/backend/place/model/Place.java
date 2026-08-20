package br.com.accessmap.backend.place.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String placeId;

    @Builder.Default
    private Double averageScore = 0.0;

    @Builder.Default
    private Integer reviewCount = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
