package ru.practicum.stats.client;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

public interface StatsClient {
    void addHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(String start, String and, List<String> uris, boolean unique);
}

