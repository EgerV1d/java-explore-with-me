package ru.practicum.ewm.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.dto.UserShortDto;
import ru.practicum.ewm.model.User;

@Component
public class UserShortMapper {
    public UserShortDto toDto(User user) {
        if (user == null) return null;
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
