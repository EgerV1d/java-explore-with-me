package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.commentDto.CommentDto;
import ru.practicum.ewm.dto.commentDto.ModerateCommentDto;
import ru.practicum.ewm.dto.commentDto.NewCommentDto;
import ru.practicum.ewm.dto.commentDto.UpdateCommentDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CommentMapper;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;
import ru.practicum.ewm.repository.CommentRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;


    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findPublishedByEventId(eventId, pageable);
        return commentMapper.toDtoList(comments);
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toEntity(request, user, event);

        if (!event.getRequestModeration()) {
            comment.setStatus(Comment.CommentStatus.PUBLISHED);
            log.info("Комментарий автоматически опубликован (модерация отключена)");
        }

        Comment saved = commentRepository.save(comment);

        log.info("Пользователь {} добавил комментарий к событию {}", userId, eventId);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является автором комментария");
        }

        comment.setText(request.getText());
        comment.setUpdatedOn(LocalDateTime.now());

        if (comment.getStatus() == Comment.CommentStatus.PENDING) {
            comment.setStatus(Comment.CommentStatus.PENDING);
            log.info("Комментарий {} отправлен на повторную модерацию после редактирования", commentId);
        }

        if (comment.getStatus() == Comment.CommentStatus.REJECTED) {
            comment.setStatus(Comment.CommentStatus.PENDING);
            comment.setRejectionReason(null);
            log.info("Отклоненный комментарий {} отправлен на повторную модерацию", commentId);
        }

        Comment saved = commentRepository.save(comment);
        log.info("Пользователь {} обновил комментарий {}", userId, commentId);
        return commentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new NotFoundException("Пользователь не является автором комментария");
        }

        commentRepository.delete(comment);
        log.info("Пользователь {} удалил комментарий {}", userId, commentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository.findByAuthorIdOrderByCreatedOnDesc(userId, pageable);
        return commentMapper.toDtoList(comments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getPendingComments(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Comment> comments = commentRepository
                .findByStatusOrderByCreatedOnAsc(Comment.CommentStatus.PENDING, pageable);
        return commentMapper.toDtoList(comments);
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, ModerateCommentDto request) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id=" + commentId + " не найден"));

        if (comment.getStatus() != Comment.CommentStatus.PENDING) {
            throw new ConflictException("Комментарий не ожидает модерации " +
                    "(текущий статус: " + comment.getStatus() + ')');
        }

        Comment.CommentStatus newStatus = request.getStatus();
        if (newStatus == Comment.CommentStatus.PUBLISHED) {
            comment.setStatus(Comment.CommentStatus.PUBLISHED);
            comment.setRejectionReason(null);
            log.info("Администратор опубликовал комментарий {}", commentId);
        } else if (newStatus == Comment.CommentStatus.REJECTED) {
            comment.setStatus(Comment.CommentStatus.REJECTED);

            String reason = request.getRejectionReason();
            if (reason == null || reason.trim().isEmpty()) {
                reason = "Комментарий не прошел модерацию";
            }
            comment.setRejectionReason(reason);
            log.info("Администратор отклонил комментарий {}: {}", commentId, reason);
        } else {
            throw new IllegalArgumentException("Недопустимый статус для модерации: " + newStatus);
        }

        Comment saved = commentRepository.save(comment);
        return commentMapper.toDto(saved);
    }
}
