package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.PeriodClosureResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.IncompleteGradesException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodClosureServiceTest {

    @Mock
    private AcademicPeriodRepository periodRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AcademicRecordRepository academicRecordRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private PeriodClosureService periodClosureService;

    private AcademicPeriod period;
    private StudyPlan plan;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        period = AcademicPeriod.builder().id(1L).academicYear(2027).semester(1).status(PeriodStatus.OPEN).build();
        plan = StudyPlan.builder().id(1L).identifier("2021.3").build();
        teacher = Teacher.builder().id(3L).fullName("Teacher Three").build();
    }

    // ------------------------------------------------------------------ helpers

    private Student student(Long id, String run) {
        return Student.builder().id(id).run(run).fullName("Student " + id).status(StudentStatus.REGULAR)
                .studyPlan(plan).build();
    }

    private Course course(Long id, String code) {
        return Course.builder().id(id).code(code).name("Course " + code).semester(1).studyPlan(plan).build();
    }

    private Section section(Long id, Course course) {
        return Section.builder().id(id).course(course).period(period).teacher(teacher).capacity(30).build();
    }

    private Enrollment enrollment(Long id, Student student, Course course, Section section, String grade) {
        return Enrollment.builder().id(id).student(student).course(course).section(section).period(period)
                .finalGrade(grade == null ? null : new BigDecimal(grade)).build();
    }

    private AcademicRecord passedRecord(Student student, Course course) {
        return AcademicRecord.builder().student(student).course(course).period(period)
                .finalGrade(new BigDecimal("5.0")).result(CourseResult.PASSED)
                .courseCode(course.getCode()).courseName(course.getName()).build();
    }

    private void stubPeriodAndEnrollments(List<Enrollment> enrollments) {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(enrollmentRepository.findByPeriodId(1L)).thenReturn(enrollments);
    }

    // ------------------------------------------------------------------ rejections

    @Test
    void closeFailsWhenThePeriodDoesNotExist() {
        when(periodRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> periodClosureService.close(9L));
    }

    @Test
    void closeRejectsAnAlreadyClosedPeriod() {
        period.setStatus(PeriodStatus.CLOSED);
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));

        assertThrows(BusinessRuleException.class, () -> periodClosureService.close(1L));
        verify(periodRepository, never()).save(any(AcademicPeriod.class));
    }

    @Test
    void closeRejectsMissingGradesAndListsTheIncompleteSections() {
        Student s1 = student(1L, "1-9");
        Student s2 = student(2L, "2-7");
        Course c1 = course(1L, "TAP101");
        Course c2 = course(2L, "TAP102");
        Section sec1 = section(10L, c1);
        Section sec2 = section(11L, c2);
        stubPeriodAndEnrollments(List.of(
                enrollment(1L, s1, c1, sec1, null),
                enrollment(2L, s2, c1, sec1, null),
                enrollment(3L, s1, c2, sec2, null),
                enrollment(4L, s2, c2, sec2, "5.0")));

        IncompleteGradesException exception =
                assertThrows(IncompleteGradesException.class, () -> periodClosureService.close(1L));

        assertEquals(2, exception.getDetails().size());
        assertTrue(exception.getDetails().get(0).contains("Section 10"));
        assertTrue(exception.getDetails().get(0).contains("TAP101"));
        assertTrue(exception.getDetails().get(0).contains("Teacher Three"));
        assertTrue(exception.getDetails().get(0).contains("2 enrollment(s)"));
        assertTrue(exception.getDetails().get(1).contains("Section 11"));
        assertTrue(exception.getDetails().get(1).contains("1 enrollment(s)"));
        verify(academicRecordRepository, never()).save(any(AcademicRecord.class));
        verify(periodRepository, never()).save(any(AcademicPeriod.class));
        assertEquals(PeriodStatus.OPEN, period.getStatus());
    }

    // ------------------------------------------------------------------ successful closings

    @Test
    void closeWithoutEnrollmentsJustClosesThePeriod() {
        stubPeriodAndEnrollments(List.of());

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(0, result.recordsCreated());
        assertTrue(result.eliminatedStudents().isEmpty());
        assertTrue(result.graduatedStudents().isEmpty());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
        assertEquals(PeriodStatus.CLOSED, result.period().status());
        verify(periodRepository).save(period);
    }

    @Test
    void closeCreatesRecordsWithPassedAtFourPointZeroAndFailedBelow() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        Course c2 = course(2L, "TAP102");
        Course c3 = course(3L, "TAP103");
        stubPeriodAndEnrollments(List.of(
                enrollment(1L, student, c1, section(10L, c1), "4.0"),
                enrollment(2L, student, c2, section(11L, c2), "3.9"),
                enrollment(3L, student, c3, section(12L, c3), "7.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(
                eq(1L), anyLong(), eq(CourseResult.FAILED))).thenReturn(0L);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of());
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1, c2, c3, course(4L, "TAP104")));

        PeriodClosureResponse result = periodClosureService.close(1L);

        ArgumentCaptor<AcademicRecord> captor = ArgumentCaptor.forClass(AcademicRecord.class);
        verify(academicRecordRepository, times(3)).save(captor.capture());
        List<AcademicRecord> records = captor.getAllValues();
        assertEquals(CourseResult.PASSED, records.get(0).getResult());
        assertEquals(CourseResult.FAILED, records.get(1).getResult());
        assertEquals(CourseResult.PASSED, records.get(2).getResult());
        assertEquals("TAP101", records.get(0).getCourseCode());
        assertEquals("Course TAP101", records.get(0).getCourseName());
        assertEquals(new BigDecimal("3.9"), records.get(1).getFinalGrade());
        assertEquals(period, records.get(0).getPeriod());
        assertEquals(3, result.recordsCreated());
        assertEquals(StudentStatus.REGULAR, student.getStatus());
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void closeEliminatesAStudentWhoFailsTheSameCourseTheSecondTime() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c1, section(10L, c1), "2.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 1L, CourseResult.FAILED))
                .thenReturn(2L);

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(StudentStatus.ELIMINATED, student.getStatus());
        assertEquals(List.of("1-9"), result.eliminatedStudents());
        assertTrue(result.graduatedStudents().isEmpty());
        verify(studentRepository).save(student);
        verify(academicRecordRepository, never()).findByStudentId(anyLong());
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
    }

    @Test
    void closeKeepsAStudentWithOnlyOneFailureOfTheCourse() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c1, section(10L, c1), "2.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 1L, CourseResult.FAILED))
                .thenReturn(1L);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of());
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1));

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
        assertTrue(result.eliminatedStudents().isEmpty());
        assertTrue(result.graduatedStudents().isEmpty());
    }

    @Test
    void closeGraduatesAStudentWhoApprovedTheWholePlan() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        Course c2 = course(2L, "TAP102");
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c2, section(10L, c2), "5.5")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 2L, CourseResult.FAILED))
                .thenReturn(0L);
        when(academicRecordRepository.findByStudentId(1L))
                .thenReturn(List.of(passedRecord(student, c1), passedRecord(student, c2)));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1, c2));

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(StudentStatus.GRADUATED, student.getStatus());
        assertEquals(List.of("1-9"), result.graduatedStudents());
        assertTrue(result.eliminatedStudents().isEmpty());
        verify(studentRepository).save(student);
    }

    @Test
    void closeDoesNotGraduateWhenAPlanCourseIsStillPending() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        Course c2 = course(2L, "TAP102");
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c1, section(10L, c1), "5.5")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 1L, CourseResult.FAILED))
                .thenReturn(0L);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of(passedRecord(student, c1)));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1, c2));

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(StudentStatus.REGULAR, student.getStatus());
        assertTrue(result.graduatedStudents().isEmpty());
    }

    @Test
    void closeDoesNotGraduateWhenTheOnlyRecordOfACourseIsFailed() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        AcademicRecord failed = AcademicRecord.builder().student(student).course(c1).period(period)
                .finalGrade(new BigDecimal("3.0")).result(CourseResult.FAILED)
                .courseCode("TAP101").courseName("Course TAP101").build();
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c1, section(10L, c1), "3.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 1L, CourseResult.FAILED))
                .thenReturn(1L);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of(failed));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1));

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertFalse(result.graduatedStudents().contains("1-9"));
        assertEquals(StudentStatus.REGULAR, student.getStatus());
    }

    @Test
    void closeDoesNotGraduateWhenThePlanHasNoCourses() {
        Student student = student(1L, "1-9");
        Course c1 = course(1L, "TAP101");
        stubPeriodAndEnrollments(List.of(enrollment(1L, student, c1, section(10L, c1), "5.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(1L, 1L, CourseResult.FAILED))
                .thenReturn(0L);
        when(academicRecordRepository.findByStudentId(1L)).thenReturn(List.of());
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of());

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertTrue(result.graduatedStudents().isEmpty());
        assertEquals(StudentStatus.REGULAR, student.getStatus());
    }

    @Test
    void closeEvaluatesEachStudentOnceEvenWithSeveralEnrollments() {
        Student a = student(1L, "1-9");
        Student b = student(2L, "2-7");
        Course c1 = course(1L, "TAP101");
        Course c2 = course(2L, "TAP102");
        Section sec1 = section(10L, c1);
        Section sec2 = section(11L, c2);
        stubPeriodAndEnrollments(List.of(
                enrollment(1L, a, c1, sec1, "5.0"),
                enrollment(2L, a, c2, sec2, "5.0"),
                enrollment(3L, b, c1, sec1, "2.0")));
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(
                eq(1L), anyLong(), eq(CourseResult.FAILED))).thenReturn(0L);
        when(academicRecordRepository.countByStudentIdAndCourseIdAndResult(
                eq(2L), anyLong(), eq(CourseResult.FAILED))).thenReturn(2L);
        when(academicRecordRepository.findByStudentId(1L))
                .thenReturn(List.of(passedRecord(a, c1), passedRecord(a, c2)));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(c1, c2));

        PeriodClosureResponse result = periodClosureService.close(1L);

        assertEquals(3, result.recordsCreated());
        assertEquals(List.of("1-9"), result.graduatedStudents());
        assertEquals(List.of("2-7"), result.eliminatedStudents());
        verify(courseRepository, times(1)).findByStudyPlanId(1L);
        verify(studentRepository, atLeastOnce()).save(a);
        verify(studentRepository).save(b);
    }
}
