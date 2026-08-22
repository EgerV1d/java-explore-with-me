package ru.practicum.ewm.dto.eventDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.dto.locationDto.LocationDto;
import ru.practicum.ewm.dto.userDto.UserShortDto;
import ru.practicum.ewm.dto.categoryDto.CategoryDto;
import ru.practicum.ewm.model.Event;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventFullDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private LocalDateTime createdOn;
    private String description;
    private LocalDateTime eventDate;
    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private LocalDateTime publishedOn;
    private Boolean requestModeration;
    private Event.EventState state;
    private String title;
    private Long views;
}
