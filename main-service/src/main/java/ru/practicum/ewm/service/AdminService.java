package ru.practicum.ewm.service;

import ru.practicum.ewm.dto.categoryDto.CategoryDto;
import ru.practicum.ewm.dto.categoryDto.NewCategoryDto;
import ru.practicum.ewm.dto.compilationDto.CompilationDto;
import ru.practicum.ewm.dto.compilationDto.NewCompilationDto;
import ru.practicum.ewm.dto.compilationDto.UpdateCompilationRequest;
import ru.practicum.ewm.dto.eventDto.EventFullDto;
import ru.practicum.ewm.dto.eventDto.UpdateEventAdminRequest;
import ru.practicum.ewm.dto.userDto.NewUserRequest;
import ru.practicum.ewm.dto.userDto.UserDto;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminService {
    UserDto addUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);

    CategoryDto addCategory(NewCategoryDto request);

    CategoryDto updateCategory(Long categoryId, CategoryDto request);

    void deleteCategory(Long categoryId);

    List<EventFullDto> getEvents(List<Long> users, List<Event.EventState> states,
                                 List<Long> categories, LocalDateTime rangeStart,
                                 LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request);

    CompilationDto addCompilation(NewCompilationDto request);

    CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request);

    void deleteCompilation(Long compilationId);
}
