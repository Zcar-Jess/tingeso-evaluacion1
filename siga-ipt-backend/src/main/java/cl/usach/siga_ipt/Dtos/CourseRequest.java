package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CourseRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 255) String name,
        @NotNull @Min(1) @Max(4) Integer semester,
        @NotNull @Min(0) Integer theoryHours,
        @NotNull @Min(0) Integer exerciseHours,
        @NotNull @Min(0) Integer labHours,
        @NotNull @Min(1) @Max(7) Integer sct,
        List<Long> prerequisiteIds) {
}
