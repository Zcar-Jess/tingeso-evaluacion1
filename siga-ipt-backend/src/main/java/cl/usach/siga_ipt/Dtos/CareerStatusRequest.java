package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.CareerStatus;
import jakarta.validation.constraints.NotNull;

public record CareerStatusRequest(@NotNull CareerStatus status) {
}
