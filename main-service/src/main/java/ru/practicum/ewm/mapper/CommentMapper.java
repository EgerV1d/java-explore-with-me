package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.model.Comment;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentMapper {
    private final UserShortMapper userShortMapper;

    public CommentDto toDto(Comment comment) {
        if (comment == null) return null;

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .author(userShortMapper.toDto(comment.getAuthor()))
                .eventId(comment.getEvent() != null ? comment.getEvent().getId() : null)
                .status(comment.getStatus())
                .build();
    }

    public Comment toEntity(NewCommentDto dto, User author, Event event) {
        if (dto == null) return null;

        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setAuthor(author);
        comment.setEvent(event);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setStatus(Comment.CommentStatus.PENDING);
        return comment;
    }

    public List<CommentDto> toDtoList(List<Comment> comments) {
        if (comments == null) return null;
        return comments.stream()
                .map(this::toDto)
                .filter(Objects::nonNull)
                .toList();
    }
}
