package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.TeacherStatus;
import jakarta.validation.constraints.NotNull;

public record TeacherStatusRequest(@NotNull TeacherStatus status) {
}
