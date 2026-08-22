package ru.practicum.ewm.dto.commentDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.userDto.UserShortDto;
import ru.practicum.ewm.model.Comment;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;
    private String text;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private UserShortDto author;
    private Long eventId;
    private Comment.CommentStatus status;
}
