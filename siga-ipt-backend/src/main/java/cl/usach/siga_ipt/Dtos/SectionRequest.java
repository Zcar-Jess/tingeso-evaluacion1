package cl.usach.siga_ipt.Dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SectionRequest(
        @NotNull Long courseId,
        @NotNull Long teacherId,
        @NotNull @Min(1) @Max(60) Integer capacity,
        @NotEmpty @Valid List<ScheduleBlockDto> scheduleBlocks) {
}
