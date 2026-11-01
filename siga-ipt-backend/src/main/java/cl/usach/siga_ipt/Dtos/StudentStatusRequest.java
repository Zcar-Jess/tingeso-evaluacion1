package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.StudentStatus;
import jakarta.validation.constraints.NotNull;

public record StudentStatusRequest(@NotNull StudentStatus status) {
}
