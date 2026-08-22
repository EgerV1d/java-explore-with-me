package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.eventDto.*;
import ru.practicum.ewm.dto.request.ParticipationRequestDto;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.*;
import ru.practicum.ewm.model.*;
import ru.practicum.ewm.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PrivateServiceImpl implements PrivateService {
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final ParticipationRequestRepository requestRepository;

    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final ParticipationRequestMapper requestMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        Pageable pageable = PageRequest.of(from / size, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        List<EventShortDto> result = eventMapper.toShortDtoList(events);

        for (int i = 0; i < result.size(); i++) {
            EventShortDto dto = result.get(i);
            Event event = events.get(i);
            Long confirmedCount = requestRepository.countConfirmedRequestsByEventId(event.getId());
            dto.setConfirmedRequests(confirmedCount);
        }

        return result;
    }

    @Override
    public EventFullDto addEvent(Long userId, NewEventDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события должна быть как минимум через 2 часа от текущего времени");
        }

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория не найдена"));

        Location location = locationMapper.toEntity(request.getLocation());
        location = locationRepository.save(location);

        Event event = eventMapper.toEntity(request, category, location, user);
        Event saved = eventRepository.save(event);

        log.info("Пользователь {} создал событие: id={}, title={}", userId, saved.getId(), saved.getTitle());
        return eventMapper.toFullDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено для этого пользователя");
        }
        return eventMapper.toFullDto(event);
    }

    @Override
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие не найдено для этого пользователя");
        }
        if (event.getState() == Event.EventState.PUBLISHED) {
            throw new ConflictException("Изменить можно только отмененные события или события " +
                    "в состоянии ожидания модерации");
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
            if (request.getEventDate().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Дата события не может быть в прошлом");
            }
            if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ConflictException("Дата события должна быть как минимум через 2 часа от текущего времени");
            }
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

        if (request.getStateAction() != null) {
            if ("SEND_TO_REVIEW".equals(request.getStateAction())) {
                event.setState(Event.EventState.PENDING);
            } else if ("CANCEL_REVIEW".equals(request.getStateAction())) {
                event.setState(Event.EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        log.info("Пользователь {} обновил событие: id={}, state={}", userId, saved.getId(), saved.getState());
        return eventMapper.toFullDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        List<ParticipationRequest> requests = requestRepository.findAllByRequesterId(userId);
        return requests.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может запросить участие в событии");
        }
        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("Событие не опубликовано");
        }
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            throw new ConflictException("Запрос уже существует");
        }

        Long confirmed = requestRepository.countConfirmedRequestsByEventId(eventId);
        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setCreated(LocalDateTime.now());
        request.setEvent(event);
        request.setRequester(user);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequest.RequestStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationRequest.RequestStatus.PENDING);
        }

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Пользователь {} запросил участие в событии {}", userId, eventId);
        return requestMapper.toDto(saved);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));
        if (!request.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Запрос для этого пользователя не найден");
        }

        request.setStatus(ParticipationRequest.RequestStatus.CANCELED);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Пользователь {} отменил запрос {}", userId, request);
        return requestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие для этого пользователя не найдено");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!event.getInitiator().getId().equals(userId)) {
            throw new NotFoundException("Событие для этого пользователя не найдено");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventIdAndIdIn(eventId,
                request.getRequestIds());

        for (ParticipationRequest pr : requests) {
            if (pr.getStatus() != ParticipationRequest.RequestStatus.PENDING) {
                throw new ConflictException("Запрос должен иметь статус PENDING");
            }
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if ("CONFIRMED".equals(request.getStatus())) {
            Long currentConfirmed = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (event.getParticipantLimit() > 0 &&
                    currentConfirmed + request.getRequestIds().size() > event.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит участников");
            }

            for (ParticipationRequest pr : requests) {
                pr.setStatus(ParticipationRequest.RequestStatus.CONFIRMED);
                confirmed.add(pr);
            }

            Long afterConfirm = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (event.getParticipantLimit() > 0 && afterConfirm >= event.getParticipantLimit()) {
                List<ParticipationRequest> pendingRequests = requestRepository.findAllByEventIdAndStatus(eventId,
                        ParticipationRequest.RequestStatus.PENDING);
                for (ParticipationRequest pr : pendingRequests) {
                    if (!request.getRequestIds().contains(pr.getId())) {
                        pr.setStatus(ParticipationRequest.RequestStatus.REJECTED);
                        rejected.add(pr);
                    }
                }
            }
        } else if ("REJECTED".equals(request.getStatus())) {
            for (ParticipationRequest pr : requests) {
                pr.setStatus(ParticipationRequest.RequestStatus.REJECTED);
                rejected.add(pr);
            }
        }

        List<ParticipationRequest> allUpdated = new ArrayList<>();
        allUpdated.addAll(confirmed);
        allUpdated.addAll(rejected);
        requestRepository.saveAll(allUpdated);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream().map(requestMapper::toDto).toList())
                .rejectedRequests(rejected.stream().map(requestMapper::toDto).toList())
                .build();
    }
}