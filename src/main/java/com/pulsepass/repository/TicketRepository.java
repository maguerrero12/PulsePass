package com.pulsepass.repository;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    // Navegación de relación (FR-TKT-006)
    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    // Tickets pagados por código de evento (FR-TKT-007)
    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    // Conteo de ventas con JPQL (FR-TKT-008)
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = :status")
    long countTicketsByEventCodeAndStatus(@Param("eventCode") String eventCode, @Param("status") TicketStatus status);
}