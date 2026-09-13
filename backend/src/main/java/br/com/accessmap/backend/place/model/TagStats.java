package br.com.accessmap.backend.place.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TagStats {

    @Builder.Default
    @Column(name = "adequado_count")
    private Long adequadoCount = 0L;

    @Builder.Default
    @Column(name = "inadequado_count")
    private Long inadequadoCount = 0L;

    @Builder.Default
    @Column(name = "inexistente_count")
    private Long inexistenteCount = 0L;
}
