package ru.practicum.ewm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId AND c.status = 'PUBLISHED'")
    List<Comment> findPublishedByEventId(@Param("eventId") Long eventId, Pageable pageable);

    List<Comment> findByAuthorIdOrderByCreatedOnDesc(Long userId, Pageable pageable);

    List<Comment> findByStatusOrderByCreatedOnAsc(Comment.CommentStatus status, Pageable pageable);
}
