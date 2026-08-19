package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    @Query("SELECT COUNT(p) FROM ParticipationRequest p WHERE p.event.id = :eventId AND p.status = 'CONFIRMED'")
    Long countConfirmedRequestsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT p.event.id, COUNT(p) FROM ParticipationRequest p " +
            "WHERE p.event.id IN :eventIds AND p.status = 'CONFIRMED' " +
            "GROUP BY p.event.id")
    List<Object[]> countConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long userId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, ParticipationRequest.RequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndIdIn(Long eventId, List<Long> requestIds);
}
