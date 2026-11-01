package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.CourseRequest;
import cl.usach.siga_ipt.Dtos.CourseResponse;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private AcademicRecordRepository academicRecordRepository;
    @Mock
    private SectionRepository sectionRepository;

    @InjectMocks
    private CourseService courseService;

    private static StudyPlan plan(Long id) {
        return StudyPlan.builder().id(id).identifier("2021.3").build();
    }

    private static Course course(Long id, String code, int semester, StudyPlan plan) {
        return Course.builder().id(id).code(code).name("Course " + code).semester(semester)
                .theoryHours(2).exerciseHours(2).labHours(0).sct(4).studyPlan(plan).build();
    }

    private static CourseRequest request(String code, int semester, int theory, int exercise, int lab,
                                         List<Long> prerequisiteIds) {
        return new CourseRequest(code, " Name ", semester, theory, exercise, lab, 4, prerequisiteIds);
    }

    private void stubSaveReturningArgument() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ queries

    @Test
    void findByStudyPlanSortsBySemesterThenCode() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.existsById(1L)).thenReturn(true);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(
                course(1L, "B2", 2, plan), course(2L, "Z1", 1, plan), course(3L, "A1", 1, plan)));

        List<CourseResponse> result = courseService.findByStudyPlan(1L);

        assertEquals(List.of("A1", "Z1", "B2"), result.stream().map(CourseResponse::code).toList());
    }

    @Test
    void findByStudyPlanFailsWhenThePlanDoesNotExist() {
        when(studyPlanRepository.existsById(9L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> courseService.findByStudyPlan(9L));
    }

    @Test
    void findByIdReturnsTheCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L, "A1", 1, plan(1L))));

        CourseResponse result = courseService.findById(1L);

        assertEquals("A1", result.code());
        assertEquals(1L, result.studyPlanId());
    }

    @Test
    void findByIdFailsWhenTheCourseDoesNotExist() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.findById(9L));
    }

    // ------------------------------------------------------------------ create

    @Test
    void createWithoutPrerequisites() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(false);
        stubSaveReturningArgument();

        CourseResponse result = courseService.create(1L, request(" A1 ", 1, 2, 2, 0, null));

        assertEquals("A1", result.code());
        assertEquals("Name", result.name());
        assertTrue(result.prerequisiteIds().isEmpty());
    }

    @Test
    void createWithAnEmptyPrerequisiteList() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(false);
        stubSaveReturningArgument();

        CourseResponse result = courseService.create(1L, request("A1", 1, 2, 2, 0, List.of()));

        assertTrue(result.prerequisiteIds().isEmpty());
    }

    @Test
    void createWithPrerequisitesIgnoringDuplicatedIds() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "C3")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10L, "A1", 1, plan)));
        when(courseRepository.findById(11L)).thenReturn(Optional.of(course(11L, "B2", 2, plan)));
        stubSaveReturningArgument();

        CourseResponse result = courseService.create(1L, request("C3", 3, 2, 2, 0, List.of(10L, 11L, 10L)));

        assertEquals(List.of(10L, 11L), result.prerequisiteIds());
        assertEquals(List.of("A1", "B2"), result.prerequisiteCodes());
    }

    @Test
    void createFailsWhenThePlanDoesNotExist() {
        when(studyPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> courseService.create(9L, request("A1", 1, 2, 2, 0, null)));
    }

    @Test
    void createRejectsDuplicatedCodeInThePlan() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(true);

        assertThrows(ConflictException.class, () -> courseService.create(1L, request("A1", 1, 2, 2, 0, null)));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void createRejectsEightOrMoreWeeklyHours() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> courseService.create(1L, request("A1", 1, 4, 2, 2, null)));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void createAcceptsSevenWeeklyHours() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(false);
        stubSaveReturningArgument();

        CourseResponse result = courseService.create(1L, request("A1", 1, 3, 2, 2, null));

        assertEquals(3, result.theoryHours());
        assertEquals(2, result.exerciseHours());
        assertEquals(2, result.labHours());
    }

    @Test
    void createRejectsMoreThanThreePrerequisites() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "E5")).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> courseService.create(1L, request("E5", 4, 2, 2, 0, List.of(1L, 2L, 3L, 4L))));
    }

    @Test
    void createRejectsAnUnknownPrerequisite() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "B2")).thenReturn(false);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> courseService.create(1L, request("B2", 2, 2, 2, 0, List.of(99L))));
    }

    @Test
    void createRejectsAPrerequisiteFromAnotherPlan() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L)));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "B2")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10L, "A1", 1, plan(2L))));

        assertThrows(BusinessRuleException.class,
                () -> courseService.create(1L, request("B2", 2, 2, 2, 0, List.of(10L))));
    }

    @Test
    void createRejectsAPrerequisiteOfTheSameOrALaterSemester() {
        StudyPlan plan = plan(1L);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "B2")).thenReturn(false);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10L, "X2", 2, plan)));

        assertThrows(BusinessRuleException.class,
                () -> courseService.create(1L, request("B2", 2, 2, 2, 0, List.of(10L))));
    }

    // ------------------------------------------------------------------ update

    @Test
    void updateChangesTheFieldsAndReplacesPrerequisites() {
        StudyPlan plan = plan(1L);
        Course oldPrerequisite = course(10L, "A1", 1, plan);
        Course newPrerequisite = course(11L, "A2", 1, plan);
        Course target = course(5L, "B2", 2, plan);
        target.getPrerequisites().add(oldPrerequisite);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target, oldPrerequisite, newPrerequisite));
        when(courseRepository.findById(11L)).thenReturn(Optional.of(newPrerequisite));
        stubSaveReturningArgument();

        CourseResponse result = courseService.update(5L, request("B2", 3, 3, 2, 1, List.of(11L)));

        assertEquals(3, result.semester());
        assertEquals("Name", result.name());
        assertEquals(List.of(11L), result.prerequisiteIds());
        verify(courseRepository, never()).existsByStudyPlanIdAndCode(any(), any());
    }

    @Test
    void updateWithoutPrerequisitesClearsThem() {
        StudyPlan plan = plan(1L);
        Course prerequisite = course(10L, "A1", 1, plan);
        Course target = course(5L, "B2", 2, plan);
        target.getPrerequisites().add(prerequisite);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target, prerequisite));
        stubSaveReturningArgument();

        CourseResponse result = courseService.update(5L, request("B2", 2, 2, 2, 0, null));

        assertTrue(result.prerequisiteIds().isEmpty());
    }

    @Test
    void updateFailsWhenTheCourseDoesNotExist() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> courseService.update(9L, request("A1", 1, 2, 2, 0, null)));
    }

    @Test
    void updateRejectsACodeUsedByAnotherCourseOfThePlan() {
        Course target = course(5L, "B2", 2, plan(1L));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "A1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> courseService.update(5L, request("A1", 2, 2, 2, 0, null)));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateAcceptsANewFreeCode() {
        Course target = course(5L, "B2", 2, plan(1L));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.existsByStudyPlanIdAndCode(1L, "B9")).thenReturn(false);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target));
        stubSaveReturningArgument();

        CourseResponse result = courseService.update(5L, request("B9", 2, 2, 2, 0, null));

        assertEquals("B9", result.code());
    }

    @Test
    void updateRejectsEightOrMoreWeeklyHours() {
        Course target = course(5L, "B2", 2, plan(1L));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));

        assertThrows(BusinessRuleException.class,
                () -> courseService.update(5L, request("B2", 2, 4, 4, 0, null)));
    }

    @Test
    void updateRejectsMovingACourseToTheSemesterOfADependant() {
        StudyPlan plan = plan(1L);
        Course target = course(5L, "A1", 1, plan);
        Course dependant = course(6L, "B2", 2, plan);
        dependant.getPrerequisites().add(target);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target, dependant));

        assertThrows(BusinessRuleException.class,
                () -> courseService.update(5L, request("A1", 2, 2, 2, 0, null)));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void updateRejectsACourseAsItsOwnPrerequisite() {
        Course target = course(5L, "B2", 2, plan(1L));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target));

        assertThrows(BusinessRuleException.class,
                () -> courseService.update(5L, request("B2", 2, 2, 2, 0, List.of(5L))));
    }

    @Test
    void updateKeepsTheNewValuesInTheSavedEntity() {
        Course target = course(5L, "B2", 2, plan(1L));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target));
        stubSaveReturningArgument();

        courseService.update(5L, new CourseRequest("B2", "Renamed", 2, 1, 2, 3, 6, null));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals("Renamed", captor.getValue().getName());
        assertEquals(1, captor.getValue().getTheoryHours());
        assertEquals(2, captor.getValue().getExerciseHours());
        assertEquals(3, captor.getValue().getLabHours());
        assertEquals(6, captor.getValue().getSct());
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteRemovesAFreeCourse() {
        StudyPlan plan = plan(1L);
        Course target = course(5L, "B2", 2, plan);
        target.getPrerequisites().add(course(4L, "A1", 1, plan));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(academicRecordRepository.existsByCourseId(5L)).thenReturn(false);
        when(sectionRepository.existsByCourseId(5L)).thenReturn(false);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target));

        courseService.delete(5L);

        assertTrue(target.getPrerequisites().isEmpty());
        verify(courseRepository).delete(target);
    }

    @Test
    void deleteRejectsACourseInTheAcademicHistory() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "B2", 2, plan(1L))));
        when(academicRecordRepository.existsByCourseId(5L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> courseService.delete(5L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void deleteRejectsACourseWithSections() {
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L, "B2", 2, plan(1L))));
        when(academicRecordRepository.existsByCourseId(5L)).thenReturn(false);
        when(sectionRepository.existsByCourseId(5L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> courseService.delete(5L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void deleteRejectsACourseUsedAsPrerequisite() {
        StudyPlan plan = plan(1L);
        Course target = course(5L, "A1", 1, plan);
        Course dependant = course(6L, "B2", 2, plan);
        dependant.getPrerequisites().add(target);
        when(courseRepository.findById(5L)).thenReturn(Optional.of(target));
        when(academicRecordRepository.existsByCourseId(5L)).thenReturn(false);
        when(sectionRepository.existsByCourseId(5L)).thenReturn(false);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(target, dependant));

        assertThrows(ConflictException.class, () -> courseService.delete(5L));
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void deleteFailsWhenTheCourseDoesNotExist() {
        when(courseRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.delete(9L));
    }
}
