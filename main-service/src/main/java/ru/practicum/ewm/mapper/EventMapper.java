package ru.practicum.ewm.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.EventFullDto;
import ru.practicum.ewm.dto.EventShortDto;
import ru.practicum.ewm.dto.NewEventDto;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Event;
import ru.practicum.ewm.model.Location;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class EventMapper {
    private final CategoryMapper categoryMapper;
    private final UserShortMapper userShortMapper;
    private final LocationMapper locationMapper;

    public EventShortDto toShortDto(Event event) {
        if (event == null) return null;

        try {
            EventShortDto.EventShortDtoBuilder builder = EventShortDto.builder()
                    .id(event.getId())
                    .annotation(event.getAnnotation())
                    .category(categoryMapper.toDto(event.getCategory()))
                    .eventDate(event.getEventDate())
                    .initiator(userShortMapper.toDto(event.getInitiator()))
                    .paid(event.getPaid())
                    .title(event.getTitle())
                    .views(0L);

            if (event.getCategory() != null) {
                builder.category(categoryMapper.toDto(event.getCategory()));
            }
            if (event.getInitiator() != null) {
                builder.initiator(userShortMapper.toDto(event.getInitiator()));
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) return null;

        try {
            EventFullDto.EventFullDtoBuilder builder = EventFullDto.builder()
                    .id(event.getId())
                    .annotation(event.getAnnotation())
                    .category(categoryMapper.toDto(event.getCategory()))
                    .createdOn(event.getCreatedOn())
                    .description(event.getDescription())
                    .eventDate(event.getEventDate())
                    .initiator(userShortMapper.toDto(event.getInitiator()))
                    .location(locationMapper.toDto(event.getLocation()))
                    .paid(event.getPaid())
                    .participantLimit(event.getParticipantLimit() != null ? event.getParticipantLimit() : 0)
                    .publishedOn(event.getPublishedOn())
                    .requestModeration(event.getRequestModeration() != null ? event.getRequestModeration() : true)
                    .state(event.getState())
                    .title(event.getTitle())
                    .views(0L);

            if (event.getState() != null) {
                builder.state(event.getState());
            }
            if (event.getCategory() != null) {
                builder.category(categoryMapper.toDto(event.getCategory()));
            }
            if (event.getInitiator() != null) {
                builder.initiator(userShortMapper.toDto(event.getInitiator()));
            }
            if (event.getLocation() != null) {
                builder.location(locationMapper.toDto(event.getLocation()));
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    public Event toEntity(NewEventDto dto, Category category,
                          Location location, User initiator) {
        if (dto == null) return null;
        Event event = new Event();
        event.setAnnotation(dto.getAnnotation());
        event.setCategory(category);
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(location);
        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setTitle(dto.getTitle());
        event.setInitiator(initiator);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(Event.EventState.PENDING);
        return event;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        if (events == null) return Collections.emptyList();
        return events.stream()
                .map(this::toShortDto)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        if (events == null) return Collections.emptyList();
        return events.stream()
                .map((this::toFullDto))
                .filter(Objects::nonNull)
                .toList();
    }
}