package cl.usach.siga_ipt.Integration;

import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.Enrollment;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Process 1: course enrollment of a student (3 successful and 3 rejected cases). */
class EnrollmentProcessIT extends AbstractIntegrationBase {

    private String body(Long studentId, Long... courseIds) {
        StringBuilder ids = new StringBuilder();
        for (Long id : courseIds) {
            ids.append(ids.length() == 0 ? "" : ",").append(id);
        }
        return "{\"studentId\":" + studentId + ",\"courseIds\":[" + ids + "]}";
    }

    private int enrolledCount() {
        return enrollmentRepository.findByStudentIdAndPeriodId(student.getId(), period.getId()).size();
    }

    // ------------------------------------------------------------------ successful cases

    @Test
    void studentEnrollsThreeCourses() throws Exception {
        mockMvc.perform(post("/api/periods/{id}/enrollments", period.getId())
                        .with(studentUser(STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(student.getId(), a1.getId(), a2.getId(), a3.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));

        assertEquals(3, enrolledCount());
    }

    @Test
    void studentEnrollsSecondSemesterCourseOnceThePrerequisiteIsApproved() throws Exception {
        record(student, a1, previousPeriod(), "5.0", CourseResult.PASSED);

        mockMvc.perform(post("/api/periods/{id}/enrollments", period.getId())
                        .with(studentUser(STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(student.getId(), a2.getId(), a3.getId(), b1.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3));

        assertEquals(3, enrolledCount());
    }

    @Test
    void studentCancelsHisOwnEnrollment() throws Exception {
        Enrollment enrollment = enroll(student, sectionA1, null);

        mockMvc.perform(delete("/api/enrollments/{id}", enrollment.getId())
                        .with(studentUser(STUDENT_EMAIL)))
                .andExpect(status().isNoContent());

        assertTrue(enrollmentRepository.findById(enrollment.getId()).isEmpty());
    }

    // ------------------------------------------------------------------ rejected cases

    @Test
    void enrollmentWithLessThanThreeCoursesIsRejected() throws Exception {
        mockMvc.perform(post("/api/periods/{id}/enrollments", period.getId())
                        .with(studentUser(STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(student.getId(), a1.getId(), a2.getId())))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(0, enrolledCount());
    }

    @Test
    void enrollmentWithoutApprovedPrerequisiteIsRejected() throws Exception {
        mockMvc.perform(post("/api/periods/{id}/enrollments", period.getId())
                        .with(studentUser(STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(student.getId(), a2.getId(), a3.getId(), b1.getId())))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(0, enrolledCount());
    }

    @Test
    void enrollmentInAFullSectionIsRejectedAndNothingIsSaved() throws Exception {
        sectionA1.setCapacity(1);
        sectionRepository.saveAndFlush(sectionA1);
        register(otherStudent);
        enroll(otherStudent, sectionA1, null);

        mockMvc.perform(post("/api/periods/{id}/enrollments", period.getId())
                        .with(studentUser(STUDENT_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(student.getId(), a1.getId(), a2.getId(), a3.getId())))
                .andExpect(status().isConflict());

        assertEquals(0, enrolledCount());
    }
}
