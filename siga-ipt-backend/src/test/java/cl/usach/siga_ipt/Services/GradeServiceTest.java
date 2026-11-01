package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.GradeRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ForbiddenException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private TeacherRepository teacherRepository;

    @InjectMocks
    private GradeService gradeService;

    private static AcademicPeriod period(Long id, PeriodStatus status) {
        return AcademicPeriod.builder().id(id).academicYear(2027).semester(1).status(status).build();
    }

    private static Teacher teacher(Long id) {
        return Teacher.builder().id(id).fullName("Teacher " + id).build();
    }

    private static Course course() {
        return Course.builder().id(1L).code("TAP101").name("Course").build();
    }

    private static Section section(Long id, Long teacherId, AcademicPeriod period) {
        return Section.builder().id(id).course(course()).period(period).teacher(teacher(teacherId))
                .capacity(30).build();
    }

    private static Enrollment enrollment(Long id, Long studentId, Section section) {
        Student student = Student.builder().id(studentId).run("1-9").fullName("Student " + studentId).build();
        return Enrollment.builder().id(id).student(student).course(section.getCourse()).section(section)
                .period(section.getPeriod()).enrollmentDate(LocalDate.of(2027, 3, 1)).build();
    }

    // ------------------------------------------------------------------ registerGrade

    @Test
    void registerGradeStoresTheGradeForTheResponsibleTeacher() {
        Section section = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        Enrollment enrollment = enrollment(20L, 5L, section);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(10L)).thenReturn(List.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("6.0")));

        assertEquals(new BigDecimal("6.0"), result.finalGrade());
        assertEquals(5L, result.studentId());
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    void registerGradeRoundsToOneDecimal() {
        Section section = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(10L)).thenReturn(List.of(enrollment(20L, 5L, section)));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("5.25")));

        assertEquals(new BigDecimal("5.3"), result.finalGrade());
    }

    @Test
    void registerGradeCanModifyAnExistingGrade() {
        Section section = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        Enrollment enrollment = enrollment(20L, 5L, section);
        enrollment.setFinalGrade(new BigDecimal("3.0"));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(10L)).thenReturn(List.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollmentResponse result = gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("4.5")));

        assertEquals(new BigDecimal("4.5"), result.finalGrade());
    }

    @Test
    void registerGradePicksTheEnrollmentOfTheRequestedStudent() {
        Section section = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        Enrollment other = enrollment(21L, 6L, section);
        Enrollment target = enrollment(20L, 5L, section);
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(10L)).thenReturn(List.of(other, target));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("7.0")));

        assertEquals(new BigDecimal("7.0"), target.getFinalGrade());
        assertNull(other.getFinalGrade());
    }

    @Test
    void registerGradeFailsWhenTheSectionDoesNotExist() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> gradeService.registerGrade(99L, 5L, new GradeRequest(3L, new BigDecimal("5.0"))));
    }

    @Test
    void registerGradeRejectsATeacherWhoDoesNotTeachTheSection() {
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(section(10L, 3L, period(1L, PeriodStatus.OPEN))));

        assertThrows(ForbiddenException.class,
                () -> gradeService.registerGrade(10L, 5L, new GradeRequest(8L, new BigDecimal("5.0"))));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void registerGradeRejectsARequestWithoutTeacher() {
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(section(10L, 3L, period(1L, PeriodStatus.OPEN))));

        assertThrows(ForbiddenException.class,
                () -> gradeService.registerGrade(10L, 5L, new GradeRequest(null, new BigDecimal("5.0"))));
    }

    @Test
    void registerGradeRejectsAClosedPeriod() {
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(section(10L, 3L, period(1L, PeriodStatus.CLOSED))));

        assertThrows(BusinessRuleException.class,
                () -> gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("5.0"))));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void registerGradeRejectsAStudentNotEnrolledInTheSection() {
        Section section = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        when(sectionRepository.findById(10L)).thenReturn(Optional.of(section));
        when(enrollmentRepository.findBySectionId(10L)).thenReturn(List.of(enrollment(21L, 6L, section)));

        assertThrows(BusinessRuleException.class,
                () -> gradeService.registerGrade(10L, 5L, new GradeRequest(3L, new BigDecimal("5.0"))));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    // ------------------------------------------------------------------ findSectionsByTeacher

    @Test
    void findSectionsByTeacherReturnsAllSectionsWithoutPeriodFilter() {
        Section first = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        Section second = section(11L, 3L, period(2L, PeriodStatus.CLOSED));
        when(teacherRepository.existsById(3L)).thenReturn(true);
        when(sectionRepository.findByTeacherId(3L)).thenReturn(List.of(first, second));
        when(enrollmentRepository.countBySectionId(10L)).thenReturn(4L);
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(0L);

        List<SectionResponse> result = gradeService.findSectionsByTeacher(3L, null);

        assertEquals(2, result.size());
        assertEquals(4L, result.get(0).enrolled());
        assertEquals(26L, result.get(0).availableSeats());
    }

    @Test
    void findSectionsByTeacherFiltersByPeriod() {
        Section first = section(10L, 3L, period(1L, PeriodStatus.OPEN));
        Section second = section(11L, 3L, period(2L, PeriodStatus.CLOSED));
        when(teacherRepository.existsById(3L)).thenReturn(true);
        when(sectionRepository.findByTeacherId(3L)).thenReturn(List.of(first, second));
        when(enrollmentRepository.countBySectionId(11L)).thenReturn(2L);

        List<SectionResponse> result = gradeService.findSectionsByTeacher(3L, 2L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).id());
    }

    @Test
    void findSectionsByTeacherFailsWhenTheTeacherDoesNotExist() {
        when(teacherRepository.existsById(9L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> gradeService.findSectionsByTeacher(9L, null));
    }

    @Test
    void passingGradeIsFourPointZero() {
        assertTrue(GradeService.PASSING_GRADE.compareTo(new BigDecimal("4.0")) == 0);
    }
}
