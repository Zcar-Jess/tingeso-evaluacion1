package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The courses a student selects for a period. They are validated and enrolled together. */
public record EnrollmentRequest(
        @NotNull Long studentId,
        @NotEmpty List<@NotNull Long> courseIds) {
}
