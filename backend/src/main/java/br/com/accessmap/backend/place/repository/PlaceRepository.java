package br.com.accessmap.backend.place.repository;

import br.com.accessmap.backend.place.model.Place;
import br.com.accessmap.backend.review.enums.AccessibilityTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, String> {

    Optional<Place> findByPlaceId(String placeId);

    /**
     * Locais que têm TODAS as características pedidas. Um local "tem" a característica quando a maioria
     * estrita dos votos naquela tag é ADEQUADO (adequado &gt; inadequado + inexistente) — assim uma única
     * avaliação negativa ou contraditória não basta para o local passar no filtro.
     * <p>
     * Cada linha de {@code tagStats} é uma tag distinta do local (a chave do mapa é única), então
     * contar as linhas que satisfazem a regra e comparar com o total de tags pedidas implementa o AND.
     */
    @Query(value = "SELECT p FROM Place p WHERE :tagCount = (" +
            "SELECT COUNT(t) FROM p.tagStats t " +
            "WHERE KEY(t) IN :tags AND t.adequadoCount > t.inadequadoCount + t.inexistenteCount)",
            countQuery = "SELECT COUNT(p) FROM Place p WHERE :tagCount = (" +
                    "SELECT COUNT(t) FROM p.tagStats t " +
                    "WHERE KEY(t) IN :tags AND t.adequadoCount > t.inadequadoCount + t.inexistenteCount)")
    Page<Place> findByAllTagsAdequate(Collection<AccessibilityTag> tags, long tagCount, Pageable pageable);
}
