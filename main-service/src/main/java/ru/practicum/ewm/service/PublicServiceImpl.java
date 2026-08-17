package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.dto.CategoryDto;
import ru.practicum.ewm.dto.CompilationDto;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.mapper.CategoryMapper;
import ru.practicum.ewm.mapper.CompilationMapper;
import ru.practicum.ewm.mapper.EventMapper;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.repository.CategoryRepository;
import ru.practicum.ewm.repository.CompilationRepository;
import ru.practicum.ewm.repository.EventRepository;
import ru.practicum.ewm.repository.ParticipationRequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PublicServiceImpl implements PublicService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final CompilationRepository compilationRepository;
    private final ParticipationRequestRepository requestRepository;

    private final EventMapper eventMapper;
    private final CategoryMapper categoryMapper;
    private final CompilationMapper compilationMapper;

    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<EventShortDto> getEvents(String text, List<Long> categories, Boolean paid,
                                         final LocalDateTime rangeStart, final LocalDateTime rangeEnd,
                                         Boolean onlyAvailable, String sort, int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new IllegalArgumentException("Старт должен быть до конца");
        }

        int validSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(from / validSize, validSize);
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        LocalDateTime end = rangeEnd != null ? rangeEnd : LocalDateTime.now().plusYears(100);

        List<Event> events;
        if ("VIEWS".equals(sort)) {
            events = eventRepository.findPublishedEventsSortedByViews(Event.EventState.PUBLISHED.name(), text, categories,
                    paid, start, end, pageable);
            log.info("Получено {} событий для сортировки по просмотрам", events.size());
        } else {
            events = eventRepository.findPublishedEventsSortedByDate(Event.EventState.PUBLISHED.name(), text, categories,
                    paid, start, end, pageable);
            log.info("Получено {} событий для сортировки по дате", events.size());
        }

        if (onlyAvailable != null && onlyAvailable) {
            events = events.stream()
                    .filter(e -> e.getParticipantLimit() == 0 ||
                            requestRepository.countConfirmedRequestsByEventId(e.getId()) < e.getParticipantLimit())
                    .toList();
        }

        List<EventShortDto> result = eventMapper.toShortDtoList(events);
        fillViewsAndRequests(result, events);

        if ("VIEWS".equals(sort) && result.size() > 1) {
            log.info("Сортировка {} событий по просмотрам в Java", result.size());
            result.sort(Comparator.comparing(EventShortDto::getViews,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        }
        return result;
    }

    @Override
    public EventFullDto getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        EventFullDto dto = eventMapper.toFullDto(event);
        fillViewsAndRequestsForFull(dto, event);
        return dto;
    }

    @Override
    public List<CategoryDto> getCategories(int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .map(categoryMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }
        return compilations.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDto getCompilation(Long compilationId) {
        return compilationRepository.findById(compilationId)
                .map(compilationMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Компиляция с id=" + compilationId + " не найдена"));
    }

    private void fillViewsAndRequests(List<EventShortDto> dtos, List<Event> events) {
        if (dtos.isEmpty()) {
            log.info("Нет событий");
            return;
        }

        log.info("Заполнение просмотров для {} событий", events.size());


        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .toList();

        log.info("Запрос статистики для URI: {}", uris);

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(1).format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER),
                    uris,
                    false
            );

            Map<String, Long> viewsMap = stats.stream()
                    .collect(Collectors.toMap(
                            ViewStatsDto::getUri,
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));

            log.info("Получена статистика: {} записей", stats.size());

            for (int i = 0; i < dtos.size(); i++) {
                EventShortDto dto = dtos.get(i);
                Event event = events.get(i);

                Long confirmed = requestRepository.countConfirmedRequestsByEventId(event.getId());
                dto.setConfirmedRequests(confirmed);

                String uri = "/events/" + event.getId();
                Long views = viewsMap.getOrDefault(uri, 0L);
                dto.setViews(views);

                log.debug("Событие id={} имеет {} просмотров", event.getId(), views);
            }
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            for (EventShortDto dto : dtos) {
                dto.setViews(0L);
            }
        }
    }

    private void fillViewsAndRequestsForFull(EventFullDto dto, Event event) {
        Long confirmed = requestRepository.countConfirmedRequestsByEventId(event.getId());
        dto.setConfirmedRequests(confirmed);

        String uri = "/events/" + event.getId();
        log.info("Запрос статистики для URI: {}", uri);

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(1).format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER),
                    List.of(uri),
                    false
            );

            log.info("Получена статистика для события {}: {}", event.getId(), stats);

            Long views = stats.stream()
                    .filter(s -> uri.equals(s.getUri()))
                    .map(ViewStatsDto::getHits)
                    .findFirst()
                    .orElse(0L);
            dto.setViews(views);

            log.info("Событие {} имеет {} просмотров", event.getId(), views);
        } catch (Exception e) {
            log.warn("Не удалось получить статистику: {}", e.getMessage());
            dto.setViews(0L);
        }
    }
}