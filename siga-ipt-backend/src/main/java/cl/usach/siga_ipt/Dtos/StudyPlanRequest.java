package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code current} is optional; when true the new plan becomes the career's current plan. */
public record StudyPlanRequest(
        @NotBlank @Size(max = 50) String identifier,
        Boolean current) {
}
