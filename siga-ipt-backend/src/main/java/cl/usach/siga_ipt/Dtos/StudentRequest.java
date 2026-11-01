package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The student is linked to the current study plan of the given career. Status starts as REGULAR. */
public record StudentRequest(
        @NotBlank @Size(max = 12) String run,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Long careerId) {
}
