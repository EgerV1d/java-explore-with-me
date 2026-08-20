package ru.practicum.ewm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findAllByInitiatorId(Long userId, Pageable pageable);

    List<Event> findByCategoryId(Long categoryId);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (e.eventDate BETWEEN :rangeStart AND :rangeEnd)")
    List<Event> findAllByAdminFilters(@Param("users") List<Long> users,
                                      @Param("states") List<Event.EventState> states,
                                      @Param("categories") List<Long> categories,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = :state " +
            "AND (CAST(:text AS TEXT) IS NULL OR e.annotation ILIKE CONCAT('%', :text, '%') " +
            "OR e.description ILIKE CONCAT('%', :text, '%')) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS BOOLEAN) IS NULL OR e.paid = :paid) " +
            "AND e.event_date BETWEEN :rangeStart AND :rangeEnd",
            nativeQuery = true)
    List<Event> findPublishedEvents(@Param("state") String state,
                                    @Param("text") String text,
                                    @Param("categories") List<Long> categories,
                                    @Param("paid") Boolean paid,
                                    @Param("rangeStart") LocalDateTime rangeStart,
                                    @Param("rangeEnd") LocalDateTime rangeEnd,
                                    Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = :state " +
            "AND (CAST(:text AS TEXT) IS NULL OR e.annotation ILIKE CONCAT('%', :text, '%') " +
            "OR e.description ILIKE CONCAT('%', :text, '%')) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS BOOLEAN) IS NULL OR e.paid = :paid) " +
            "AND e.event_date BETWEEN :rangeStart AND :rangeEnd " +
            "ORDER BY e.event_date ASC",
            nativeQuery = true)
    List<Event> findPublishedEventsSortedByDate(@Param("state") String state,
                                                @Param("text") String text,
                                                @Param("categories") List<Long> categories,
                                                @Param("paid") Boolean paid,
                                                @Param("rangeStart") LocalDateTime rangeStart,
                                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                                Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = :state " +
            "AND (CAST(:text AS TEXT) IS NULL OR e.annotation ILIKE CONCAT('%', :text, '%') " +
            "OR e.description ILIKE CONCAT('%', :text, '%')) " +
            "AND (CAST(:categories AS TEXT) IS NULL OR e.category_id IN (:categories)) " +
            "AND (CAST(:paid AS BOOLEAN) IS NULL OR e.paid = :paid) " +
            "AND e.event_date BETWEEN :rangeStart AND :rangeEnd",
            nativeQuery = true)
    List<Event> findPublishedEventsSortedByViews(@Param("state") String state,
                                                 @Param("text") String text,
                                                 @Param("categories") List<Long> categories,
                                                 @Param("paid") Boolean paid,
                                                 @Param("rangeStart") LocalDateTime rangeStart,
                                                 @Param("rangeEnd") LocalDateTime rangeEnd,
                                                 Pageable pageable);
}
