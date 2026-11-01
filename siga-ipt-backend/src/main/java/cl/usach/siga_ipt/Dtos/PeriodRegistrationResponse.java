package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.PeriodRegistration;

public record PeriodRegistrationResponse(
        Long id,
        Long studentId,
        String studentRun,
        String studentName,
        Long periodId,
        String period) {

    public static PeriodRegistrationResponse from(PeriodRegistration r) {
        return new PeriodRegistrationResponse(
                r.getId(),
                r.getStudent().getId(), r.getStudent().getRun(), r.getStudent().getFullName(),
                r.getPeriod().getId(), r.getPeriod().getLabel());
    }
}
