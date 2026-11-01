package cl.usach.siga_ipt.Integration;

import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Process 2: registration of final grades by the teacher of the section (3 successful, 3 rejected). */
class GradeProcessIT extends AbstractIntegrationBase {

    private Enrollment enrollment;

    @BeforeEach
    void enrollStudent() {
        enrollment = enroll(student, sectionA1, null);
    }

    private ResultActions putGrade(
            RequestPostProcessor who, String json) throws Exception {
        return mockMvc.perform(put("/api/sections/{s}/students/{st}/grade", sectionA1.getId(), student.getId())
                .with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private BigDecimal storedGrade() {
        return enrollmentRepository.findById(enrollment.getId()).orElseThrow().getFinalGrade();
    }

    // ------------------------------------------------------------------ successful cases

    @Test
    void teacherRegistersTheFinalGrade() throws Exception {
        putGrade(teacherUser(TEACHER_EMAIL), "{\"finalGrade\":5.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(5.5));

        assertEquals(0, new BigDecimal("5.5").compareTo(storedGrade()));
    }

    @Test
    void teacherModifiesAnExistingGradeWhileThePeriodIsOpen() throws Exception {
        enrollment.setFinalGrade(new BigDecimal("4.0"));
        enrollmentRepository.saveAndFlush(enrollment);

        putGrade(teacherUser(TEACHER_EMAIL), "{\"finalGrade\":6.0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finalGrade").value(6.0));

        assertEquals(0, new BigDecimal("6.0").compareTo(storedGrade()));
    }

    @Test
    void registeredGradeIsVisibleInTheListOfTheSection() throws Exception {
        putGrade(teacherUser(TEACHER_EMAIL), "{\"finalGrade\":7.0}").andExpect(status().isOk());

        mockMvc.perform(get("/api/sections/{id}/enrollments", sectionA1.getId())
                        .with(teacherUser(TEACHER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].finalGrade").value(7.0));
    }

    // ------------------------------------------------------------------ rejected cases

    @Test
    void anotherTeacherCannotRegisterTheGrade() throws Exception {
        putGrade(teacherUser(OTHER_TEACHER_EMAIL), "{\"finalGrade\":5.5}")
                .andExpect(status().isForbidden());

        assertNull(storedGrade());
    }

    @Test
    void gradeOutsideTheRangeIsRejected() throws Exception {
        putGrade(teacherUser(TEACHER_EMAIL), "{\"finalGrade\":7.5}")
                .andExpect(status().isBadRequest());

        assertNull(storedGrade());
    }

    @Test
    void gradeCannotBeChangedOnceThePeriodIsClosed() throws Exception {
        period.setStatus(PeriodStatus.CLOSED);
        periodRepository.saveAndFlush(period);

        putGrade(teacherUser(TEACHER_EMAIL), "{\"finalGrade\":5.5}")
                .andExpect(status().isUnprocessableEntity());

        assertNull(storedGrade());
    }
}
