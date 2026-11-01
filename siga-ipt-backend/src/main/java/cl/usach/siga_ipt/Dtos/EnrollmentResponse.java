package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.Enrollment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentRun,
        String studentName,
        Long courseId,
        String courseCode,
        String courseName,
        Long sectionId,
        Long periodId,
        String period,
        LocalDate enrollmentDate,
        BigDecimal finalGrade) {

    public static EnrollmentResponse from(Enrollment e) {
        return new EnrollmentResponse(
                e.getId(),
                e.getStudent().getId(), e.getStudent().getRun(), e.getStudent().getFullName(),
                e.getCourse().getId(), e.getCourse().getCode(), e.getCourse().getName(),
                e.getSection().getId(),
                e.getPeriod().getId(), e.getPeriod().getLabel(),
                e.getEnrollmentDate(), e.getFinalGrade());
    }
}
