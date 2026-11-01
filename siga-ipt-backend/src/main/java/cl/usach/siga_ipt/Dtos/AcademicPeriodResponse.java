package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.PeriodStatus;

public record AcademicPeriodResponse(
        Long id,
        int academicYear,
        int semester,
        String label,
        PeriodStatus status) {

    public static AcademicPeriodResponse from(AcademicPeriod p) {
        return new AcademicPeriodResponse(p.getId(), p.getAcademicYear(), p.getSemester(),
                p.getLabel(), p.getStatus());
    }
}
