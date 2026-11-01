package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * careerId and studyPlanId are optional; they can only change while the student
 * has no enrollments and no academic history.
 */
public record StudentUpdateRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        Long careerId,
        Long studyPlanId) {
}
