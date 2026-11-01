package cl.usach.siga_ipt.Dtos;

import java.util.List;

/** Summary of a period closing: students are identified by RUN. */
public record PeriodClosureResponse(
        AcademicPeriodResponse period,
        int recordsCreated,
        List<String> eliminatedStudents,
        List<String> graduatedStudents) {
}
