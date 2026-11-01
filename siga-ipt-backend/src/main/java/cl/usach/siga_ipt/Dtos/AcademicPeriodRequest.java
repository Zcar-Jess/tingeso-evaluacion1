package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AcademicPeriodRequest(
        @NotNull @Min(2000) @Max(2100) Integer academicYear,
        @NotNull @Min(1) @Max(2) Integer semester) {
}
