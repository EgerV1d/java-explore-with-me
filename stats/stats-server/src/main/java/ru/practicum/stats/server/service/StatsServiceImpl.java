package ru.practicum.stats.server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.Hit;
import ru.practicum.stats.server.repository.HitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private final HitRepository hitRepository;

    @Override
    @Transactional
    public void addHit(EndpointHitDto dto) {
        Hit hit = new Hit();
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setEventTime(dto.getTimestamp().withNano(0));

        log.info("Saving hit: uri={}, ip={}, time={}", hit.getUri(), hit.getIp(), hit.getEventTime());
        Hit saved = hitRepository.save(hit);
        log.info("Saved hit with id={}", saved.getId());
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Старт должен быть до окончания даты");
        }
        if (uris != null && uris.isEmpty()) {
            uris = null;
        }
        LocalDateTime endWithOffset = end.plusSeconds(1);

        if (unique) {
            return hitRepository.findUniqueStats(start, end, uris);
        } else {
            return hitRepository.findStats(start, end, uris);
        }
    }
}
