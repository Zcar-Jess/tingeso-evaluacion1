package cl.usach.siga_ipt.Dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * teacherId is optional. With a Keycloak token the teacher is the one linked to the token and
 * this field is ignored; it is only used without authentication (dev profile).
 */
public record GradeRequest(
        Long teacherId,
        @NotNull @DecimalMin("1.0") @DecimalMax("7.0") @Digits(integer = 1, fraction = 1) BigDecimal finalGrade) {
}