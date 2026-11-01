package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.ScheduleBlockDto;
import cl.usach.siga_ipt.Dtos.SectionRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Dtos.SectionUpdateRequest;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.DayCode;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {

    @Mock private SectionRepository sectionRepository;
    @Mock private AcademicPeriodRepository periodRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @InjectMocks private SectionService service;

    private AcademicPeriod period;
    private Course course;      // 4 weekly hours -> 2 schedule blocks
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        period = AcademicPeriod.builder().id(1L).academicYear(2026).semester(2)
                .status(PeriodStatus.OPEN).build();
        course = course(10L, 2, 0, 2);
        teacher = teacher(2L, TeacherStatus.ACTIVE);
    }

    // ------------------------------------------------------------------ helpers

    private Course course(Long id, int theory, int exercise, int lab) {
        return Course.builder().id(id).code("TAP" + id).name("Course " + id).semester(1)
                .theoryHours(theory).exerciseHours(exercise).labHours(lab).sct(3).build();
    }

    private Teacher teacher(Long id, TeacherStatus status) {
        return Teacher.builder().id(id).run("1-" + id).fullName("Teacher " + id)
                .email("t" + id + "@ipt.cl").status(status).build();
    }

    private ScheduleBlockDto block(DayCode day, int module) {
        return new ScheduleBlockDto(day, module);
    }

    private Section existingSection(Teacher owner, ScheduleBlock... blocks) {
        Section s = Section.builder().id(50L).course(course).period(period).teacher(owner).capacity(30).build();
        s.getScheduleBlocks().addAll(List.of(blocks));
        return s;
    }

    private SectionRequest createRequest(int capacity, ScheduleBlockDto... blocks) {
        return new SectionRequest(10L, 2L, capacity, List.of(blocks));
    }

    private SectionUpdateRequest updateRequest(Long teacherId, int capacity, ScheduleBlockDto... blocks) {
        return new SectionUpdateRequest(teacherId, capacity, List.of(blocks));
    }

    /** Stubs the standard lookups of create(): period, course, teacher and "no section yet". */
    private void stubCreateLookups() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
    }

    private void stubNoTeacherConflicts() {
        when(sectionRepository.teacherHasConflict(anyLong(), anyLong(), any(DayCode.class), anyInt(), anyLong()))
                .thenReturn(false);
    }

    private void stubSaveSection() {
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ queries

    @Test
    void findByPeriodReturnsItsSections() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.M, 2), new ScheduleBlock(DayCode.L, 1));
        when(periodRepository.existsById(1L)).thenReturn(true);
        when(sectionRepository.findByPeriodId(1L)).thenReturn(List.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(4L);

        List<SectionResponse> result = service.findByPeriod(1L);

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).enrolled());
        assertEquals(26L, result.get(0).availableSeats());
        assertEquals(List.of("L1", "M2"), result.get(0).schedule());
    }

    @Test
    void findByPeriodThrowsWhenPeriodMissing() {
        when(periodRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.findByPeriod(1L));
        verify(sectionRepository, never()).findByPeriodId(anyLong());
    }

    @Test
    void findByIdReturnsSection() {
        when(sectionRepository.findById(50L))
                .thenReturn(Optional.of(existingSection(teacher, new ScheduleBlock(DayCode.L, 1))));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);

        SectionResponse response = service.findById(50L);

        assertEquals(50L, response.id());
        assertEquals("TAP10", response.courseCode());
        assertEquals("2026-2", response.period());
        assertEquals("Teacher 2", response.teacherName());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createSavesSectionWithItsBlocks() {
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);
        stubNoTeacherConflicts();
        when(sectionRepository.save(any(Section.class))).thenAnswer(inv -> {
            Section s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SectionResponse response = service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2)));

        assertEquals(100L, response.id());
        assertEquals(30, response.capacity());
        assertEquals(0L, response.enrolled());
        assertEquals(30L, response.availableSeats());
        assertEquals(2L, response.teacherId());
        assertEquals(List.of("L1", "L2"), response.schedule());
    }

    @Test
    void createRoundsUpOddWeeklyHours() {
        course = course(10L, 3, 0, 0);   // 3 hours -> 2 blocks
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);
        stubNoTeacherConflicts();
        stubSaveSection();

        SectionResponse response = service.create(1L, createRequest(20, block(DayCode.W, 3), block(DayCode.J, 4)));

        assertEquals(List.of("W3", "J4"), response.schedule());
    }

    @Test
    void createThrowsWhenPeriodMissing() {
        when(periodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenCourseMissing() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
    }

    @Test
    void createThrowsWhenTeacherMissing() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(teacherRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
    }

    @Test
    void createRejectsClosedPeriod() {
        period.setStatus(PeriodStatus.CLOSED);
        stubCreateLookups();

        assertThrows(BusinessRuleException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createRejectsSecondSectionOfTheSameCourseInThePeriod() {
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveTeacher() {
        teacher.setStatus(TeacherStatus.INACTIVE);
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createRejectsWrongNumberOfBlocks() {
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicatedBlocks() {
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 1))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void createRejectsTeacherScheduleClash() {
        stubCreateLookups();
        when(sectionRepository.existsByCourseIdAndPeriodId(10L, 1L)).thenReturn(false);
        when(sectionRepository.teacherHasConflict(anyLong(), anyLong(), any(DayCode.class), anyInt(), anyLong()))
                .thenAnswer(inv -> {
                    DayCode day = inv.getArgument(2);
                    Integer module = inv.getArgument(3);
                    return day == DayCode.L && module == 1;
                });

        assertThrows(ConflictException.class,
                () -> service.create(1L, createRequest(30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateChangesCapacityAndScheduleWithoutEnrollments() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        stubNoTeacherConflicts();
        stubSaveSection();

        SectionResponse response = service.update(50L,
                updateRequest(2L, 40, block(DayCode.M, 1), block(DayCode.M, 2)));

        assertEquals(40, response.capacity());
        assertEquals(List.of("M1", "M2"), response.schedule());
    }

    @Test
    void updateCanAssignAnotherActiveTeacher() {
        Teacher other = teacher(3L, TeacherStatus.ACTIVE);
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(other));
        stubNoTeacherConflicts();
        stubSaveSection();

        SectionResponse response = service.update(50L,
                updateRequest(3L, 30, block(DayCode.L, 1), block(DayCode.L, 2)));

        assertEquals(3L, response.teacherId());
    }

    @Test
    void updateKeepsTheSameTeacherEvenIfInactive() {
        teacher.setStatus(TeacherStatus.INACTIVE);
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        stubNoTeacherConflicts();
        stubSaveSection();

        SectionResponse response = service.update(50L,
                updateRequest(2L, 35, block(DayCode.L, 1), block(DayCode.L, 2)));

        assertEquals(35, response.capacity());
    }

    @Test
    void updateThrowsWhenSectionMissing() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(99L, updateRequest(2L, 30, block(DayCode.L, 1), block(DayCode.L, 2))));
    }

    @Test
    void updateRejectsClosedPeriod() {
        period.setStatus(PeriodStatus.CLOSED);
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class,
                () -> service.update(50L, updateRequest(2L, 30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenTeacherMissing() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(50L, updateRequest(77L, 30, block(DayCode.L, 1), block(DayCode.L, 2))));
    }

    @Test
    void updateRejectsNewInactiveTeacher() {
        Teacher inactive = teacher(3L, TeacherStatus.INACTIVE);
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(3L)).thenReturn(Optional.of(inactive));

        assertThrows(BusinessRuleException.class,
                () -> service.update(50L, updateRequest(3L, 30, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateRejectsCapacityLowerThanEnrolledStudents() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(5L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));

        assertThrows(BusinessRuleException.class,
                () -> service.update(50L, updateRequest(2L, 3, block(DayCode.L, 1), block(DayCode.L, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateRejectsWrongNumberOfBlocks() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));

        assertThrows(BusinessRuleException.class,
                () -> service.update(50L, updateRequest(2L, 30, block(DayCode.L, 1))));
    }

    @Test
    void updateRejectsScheduleChangeWhenStudentsAreEnrolled() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(5L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));

        assertThrows(ConflictException.class,
                () -> service.update(50L, updateRequest(2L, 30, block(DayCode.M, 1), block(DayCode.M, 2))));
        verify(sectionRepository, never()).save(any());
    }

    @Test
    void updateAllowsSameScheduleWhenStudentsAreEnrolled() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(5L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        stubNoTeacherConflicts();
        stubSaveSection();

        SectionResponse response = service.update(50L,
                updateRequest(2L, 45, block(DayCode.L, 2), block(DayCode.L, 1)));

        assertEquals(45, response.capacity());
        assertEquals(5L, response.enrolled());
        assertEquals(List.of("L1", "L2"), response.schedule());
    }

    @Test
    void updateRejectsTeacherScheduleClash() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.countBySectionId(50L)).thenReturn(0L);
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(sectionRepository.teacherHasConflict(anyLong(), anyLong(), any(DayCode.class), anyInt(), anyLong()))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.update(50L, updateRequest(2L, 30, block(DayCode.W, 1), block(DayCode.W, 2))));
        verify(sectionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteRemovesSectionWithoutEnrollments() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsBySectionId(50L)).thenReturn(false);

        service.delete(50L);

        verify(sectionRepository).delete(s);
    }

    @Test
    void deleteThrowsWhenSectionMissing() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
    }

    @Test
    void deleteRejectsClosedPeriod() {
        period.setStatus(PeriodStatus.CLOSED);
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleException.class, () -> service.delete(50L));
        verify(sectionRepository, never()).delete(any());
    }

    @Test
    void deleteRejectsSectionWithEnrolledStudents() {
        Section s = existingSection(teacher, new ScheduleBlock(DayCode.L, 1), new ScheduleBlock(DayCode.L, 2));
        when(sectionRepository.findById(50L)).thenReturn(Optional.of(s));
        when(enrollmentRepository.existsBySectionId(50L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(50L));
        verify(sectionRepository, never()).delete(any());
    }
}
