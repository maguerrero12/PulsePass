package com.pulsepass.repository;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    // Eventos publicados ordenados por fecha (FR-EVT-005)
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    // Eventos por código de venue
    List<Event> findByVenueCode(String venueCode);

    // Buscar eventos por nombre artístico (FR-SRC-001)
    @Query("SELECT e FROM Event e JOIN e.artists a WHERE a.stageName = :stageName")
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    // Eventos por ciudad y artista (FR-SRC-002)
    @Query("SELECT e FROM Event e JOIN e.artists a WHERE e.venue.city = :city AND a.stageName = :stageName")
    List<Event> findByVenueCityAndArtistStageName(@Param("city") String city, @Param("stageName") String stageName);

    // Eventos recomendados (FR-SRC-003)
    @Query("SELECT DISTINCT e FROM Event e JOIN e.artists a " +
            "WHERE e.status = :status AND e.eventDate > :date " +
            "AND e.venue.city = :city AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%')) " +
            "ORDER BY e.eventDate ASC")
    List<Event> findRecommendedEvents(
            @Param("status") EventStatus status,
            @Param("date") LocalDateTime date,
            @Param("city") String city,
            @Param("artistText") String artistText);
}