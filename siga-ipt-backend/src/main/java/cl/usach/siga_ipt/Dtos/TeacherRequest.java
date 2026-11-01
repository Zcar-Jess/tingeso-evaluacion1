package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherRequest(
        @NotBlank @Size(max = 12) String run,
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String professionalTitle,
        @NotBlank @Size(max = 255) String academicDegree) {
}
