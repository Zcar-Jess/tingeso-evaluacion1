package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.NotNull;

public record PeriodRegistrationRequest(@NotNull Long studentId) {
}
