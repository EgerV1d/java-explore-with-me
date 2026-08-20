package ru.practicum.ewm.dto.compilationDto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompilationRequest {

    @Size(max = 50)
    private String title;

    private Boolean pinned;
    private List<Long> events;
}
