package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CareerRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 1000) String description) {
}
