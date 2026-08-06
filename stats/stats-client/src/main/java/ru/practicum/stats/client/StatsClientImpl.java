package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class StatsClientImpl implements StatsClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl;

    public StatsClientImpl(@Value("${stats-server.url:http://localhost:9090}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void addHit(EndpointHitDto hitDto) {
        try {
            restTemplate.postForEntity(baseUrl + "/hit", hitDto, Void.class);
            log.debug("Hit sent: {}", hitDto);
        } catch (RestClientException e) {
            log.warn("Failed to sent hit:{}", e.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "/stats")
                    .queryParam("start", start)
                    .queryParam("end", end)
                    .queryParam("unique", false);

            if (uris != null && !uris.isEmpty()) {
                uris.forEach(builder::queryParam);
            }
            URI uri = builder.build().encode().toUri();

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Failed to get stats: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
