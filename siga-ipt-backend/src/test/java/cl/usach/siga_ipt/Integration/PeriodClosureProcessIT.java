package cl.usach.siga_ipt.Integration;

import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.StudentStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Process 3: closing of the academic period (3 successful and 3 rejected cases). */
class PeriodClosureProcessIT extends AbstractIntegrationBase {

    private PeriodStatus periodStatus() {
        return periodRepository.findById(period.getId()).orElseThrow().getStatus();
    }

    private StudentStatus studentStatus() {
        return studentRepository.findById(student.getId()).orElseThrow().getStatus();
    }

    // ------------------------------------------------------------------ successful cases

    @Test
    void closingCreatesTheAcademicRecordsAndClosesThePeriod() throws Exception {
        enroll(student, sectionA1, "5.0");
        enroll(student, sectionA2, "6.0");
        enroll(student, sectionA3, "3.5");

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsCreated").value(3))
                .andExpect(jsonPath("$.eliminatedStudents.length()").value(0));

        assertEquals(PeriodStatus.CLOSED, periodStatus());
        assertEquals(3, academicRecordRepository.findByStudentId(student.getId()).size());
        assertEquals(1, academicRecordRepository.countByStudentIdAndCourseIdAndResult(
                student.getId(), a3.getId(), CourseResult.FAILED));
        assertEquals(StudentStatus.REGULAR, studentStatus());
    }

    @Test
    void studentWhoFailsTheSameCourseTwiceIsEliminated() throws Exception {
        record(student, a1, previousPeriod(), "3.0", CourseResult.FAILED);
        enroll(student, sectionA1, "3.5");

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eliminatedStudents[0]").value(student.getRun()));

        assertEquals(StudentStatus.ELIMINATED, studentStatus());
        assertEquals(PeriodStatus.CLOSED, periodStatus());
    }

    @Test
    void studentWhoApprovesTheWholePlanGraduates() throws Exception {
        var before = previousPeriod();
        record(student, a2, before, "5.0", CourseResult.PASSED);
        record(student, a3, before, "5.0", CourseResult.PASSED);
        record(student, b1, before, "5.0", CourseResult.PASSED);
        enroll(student, sectionA1, "5.0");

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graduatedStudents[0]").value(student.getRun()));

        assertEquals(StudentStatus.GRADUATED, studentStatus());
    }

    // ------------------------------------------------------------------ rejected cases

    @Test
    void closingIsRejectedWhileSomeGradeIsMissing() throws Exception {
        enroll(student, sectionA1, "5.0");
        enroll(student, sectionA2, null);

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(admin()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.details.length()").value(1));

        assertEquals(PeriodStatus.OPEN, periodStatus());
        assertEquals(0, academicRecordRepository.findByStudentId(student.getId()).size());
    }

    @Test
    void anAlreadyClosedPeriodCannotBeClosedAgain() throws Exception {
        period.setStatus(PeriodStatus.CLOSED);
        periodRepository.saveAndFlush(period);

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(admin()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aTeacherCannotClosePeriods() throws Exception {
        enroll(student, sectionA1, "5.0");

        mockMvc.perform(post("/api/periods/{id}/close", period.getId()).with(teacherUser(TEACHER_EMAIL)))
                .andExpect(status().isForbidden());

        assertEquals(PeriodStatus.OPEN, periodStatus());
    }
}
