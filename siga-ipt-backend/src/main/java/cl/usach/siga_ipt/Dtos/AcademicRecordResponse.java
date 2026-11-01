package cl.usach.siga_ipt.Dtos;

import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.CourseResult;

import java.math.BigDecimal;

/** Uses the course code/name copied into the record, not the current course data. */
public record AcademicRecordResponse(
        Long id,
        String courseCode,
        String courseName,
        String period,
        BigDecimal finalGrade,
        CourseResult result) {

    public static AcademicRecordResponse from(AcademicRecord r) {
        return new AcademicRecordResponse(
                r.getId(), r.getCourseCode(), r.getCourseName(),
                r.getPeriod().getLabel(), r.getFinalGrade(), r.getResult());
    }
}
