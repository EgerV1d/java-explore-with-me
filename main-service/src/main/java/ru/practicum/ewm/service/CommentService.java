package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.comment.CommentDto;
import ru.practicum.ewm.dto.comment.ModerateCommentDto;
import ru.practicum.ewm.dto.comment.NewCommentDto;
import ru.practicum.ewm.dto.comment.UpdateCommentDto;

import java.util.List;

public interface CommentService {
    //публичные методы
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    //приват методы
    CommentDto addComment(Long userId, Long eventId, NewCommentDto request);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto request);

    void deleteComment(Long userId, Long commentId);

    List<CommentDto> getUserComments(Long userId, int from, int size);

    //админ методы
    List<CommentDto> getPendingComments(int from, int size);

    CommentDto moderateComment(Long commentId, ModerateCommentDto request);
}
