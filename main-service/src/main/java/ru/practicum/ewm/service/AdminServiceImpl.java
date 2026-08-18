package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.*;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.*;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;
    private final LocationRepository locationRepository;
    private final ParticipationRequestRepository requestRepository;

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final EventMapper eventMapper;
    private final CompilationMapper compilationMapper;
    private final LocationMapper locationMapper;

    @Override
    public UserDto addUser(NewUserRequest request) {
        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        log.info("Админ добавил пользователя: id={}, email={}", saved.getId(), saved.getEmail());
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<User> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllByIdIn(ids, pageable);
        } else {
            users = userRepository.findAll(pageable).getContent();
        }
        return users.stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
        userRepository.deleteById(userId);
        log.info("Админ удалил пользователя: id={}", userId);
    }

    @Override
    public CategoryDto addCategory(NewCategoryDto request) {
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        log.info("Админ сохранил категорию: id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toDto(saved);
    }

    @Override
    public CategoryDto updateCategory(Long categoryId, CategoryDto request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдено"));
        category.setName(request.getName());
        Category saved = categoryRepository.save(category);
        log.info("Админ изменил категорию id={}, name={}", saved.getId(), saved.getName());
        return categoryMapper.toDto(saved);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
        if (!eventRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new ConflictException("Категория не пуста");
        }
        categoryRepository.delete(category);
        log.info("Админ удалил категорию");
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getEvents(List<Long> users, List<Event.EventState> states,
                                        List<Long> categories, LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd, int from, int size) {
        int validSize = size > 0 ? size : 10;
        int page = from / validSize;
        Pageable pageable = PageRequest.of(page, validSize);

        LocalDateTime start = rangeStart != null ?
                rangeStart : LocalDateTime.of(1900, 1, 1, 0, 0, 0);
        LocalDateTime end = rangeEnd != null ?
                rangeEnd : LocalDateTime.of(3000, 1, 1, 0, 0, 0);

        List<Event> events = eventRepository.findAllByAdminFilters(users, states, categories,
                start, end, pageable);

        List<EventFullDto> result = eventMapper.toFullDtoList(events);

        for (EventFullDto dto : result) {
            Long confirmedCount = requestRepository.countConfirmedRequestsByEventId(dto.getId());
            dto.setConfirmedRequests(confirmedCount);
            log.debug("Event id={} has {} confirmed requests", dto.getId(), confirmedCount);
        }

        return result;
    }

    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (request.getStateAction() != null) {
            if ("PUBLISH_EVENT".equals(request.getStateAction())) {
                if (event.getState() != Event.EventState.PENDING) {
                    throw new ConflictException("Невозможно опубликовать ивент, потому что не PENDING статус");
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                    throw new ConflictException("Дата события должна быть как минимум через 1 час от текущего времени");
                }
                event.setState(Event.EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if ("REJECT_EVENT".equals(request.getStateAction())) {
                if (event.getState() == Event.EventState.PUBLISHED) {
                    throw new ConflictException("Невозможно отменить опубликованное событие");
                }
                event.setState(Event.EventState.CANCELED);
            }
        }

        if (request.getEventDate() != null) {
            if (request.getEventDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Дата события не может быть в прошлом");
            }
            event.setEventDate(request.getEventDate());
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория не найдена"));
            event.setCategory(category);
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            Location location = locationMapper.toEntity(request.getLocation());
            location = locationRepository.save(location);
            event.setLocation(location);
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        Event saved = eventRepository.save(event);
        log.info("Админ обновил событие: id={}, state={}", saved.getId(), saved.getState());
        return eventMapper.toFullDto(saved);
    }

    @Override
    public CompilationDto addCompilation(NewCompilationDto request) {
        Compilation compilation = compilationMapper.toEntity(request);
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(request.getEvents());
            compilation.setEvents(events);
        }
        Compilation saved = compilationRepository.save(compilation);
        log.info("Админ добавил подборку: id={}, title={}", saved.getId(), saved.getTitle());
        return compilationMapper.toDto(saved);
    }

    @Override
    public CompilationDto updateCompilation(Long compilationId, UpdateCompilationRequest request) {
        Compilation compilation = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compilationId + " не найдена"));
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(request.getEvents());
            compilation.setEvents(events);
        }

        Compilation saved = compilationRepository.save(compilation);
        log.info("Админ обновил подборку: id={}", saved.getId());
        return compilationMapper.toDto(saved);
    }

    @Override
    public void deleteCompilation(Long compilationId) {
        if (!compilationRepository.existsById(compilationId)) {
            throw new NotFoundException("Подборка с id=" + compilationId + " не найдена");
        }
        compilationRepository.deleteById(compilationId);
        log.info("Админ удалил подборку: id={}", compilationId);
    }
}