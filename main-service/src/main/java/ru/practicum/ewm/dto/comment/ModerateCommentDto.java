package ru.practicum.ewm.dto.comment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.model.Comment;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModerateCommentDto {

    @NotNull(message = "Статус обязателен")
    private Comment.CommentStatus status;

    @Size(max = 500)
    private String rejectionReason;
}
