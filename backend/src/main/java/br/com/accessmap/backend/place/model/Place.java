package br.com.accessmap.backend.place.model;

import br.com.accessmap.backend.review.enums.AccessibilityTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "place_tag_stats", joinColumns = @JoinColumn(name = "place_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "tag")
    @Builder.Default
    private Map<AccessibilityTag, TagStats> tagStats = new EnumMap<>(AccessibilityTag.class);

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
