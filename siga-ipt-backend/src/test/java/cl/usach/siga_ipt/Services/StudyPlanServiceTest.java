package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.StudyPlanRequest;
import cl.usach.siga_ipt.Dtos.StudyPlanResponse;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private CareerRepository careerRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudyPlanService studyPlanService;

    private static Career career() {
        return Career.builder().id(1L).code("10450").name("Career").description("Desc")
                .status(CareerStatus.ACTIVE).build();
    }

    private static StudyPlan plan(Long id, String identifier, boolean current) {
        return StudyPlan.builder().id(id).identifier(identifier).current(current).career(career()).build();
    }

    private void stubSaveAssigningId() {
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> {
            StudyPlan p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(10L);
            }
            return p;
        });
    }

    @Test
    void findByCareerReturnsItsPlans() {
        when(careerRepository.existsById(1L)).thenReturn(true);
        when(studyPlanRepository.findByCareerId(1L))
                .thenReturn(List.of(plan(1L, "2021.3", true), plan(2L, "2019.1", false)));

        List<StudyPlanResponse> result = studyPlanService.findByCareer(1L);

        assertEquals(2, result.size());
        assertEquals("2021.3", result.get(0).identifier());
    }

    @Test
    void findByCareerFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.existsById(9L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.findByCareer(9L));
    }

    @Test
    void findByIdReturnsThePlan() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "2021.3", true)));

        StudyPlanResponse result = studyPlanService.findById(1L);

        assertEquals("2021.3", result.identifier());
        assertEquals("10450", result.careerCode());
        assertTrue(result.current());
    }

    @Test
    void findByIdFailsWhenThePlanDoesNotExist() {
        when(studyPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.findById(9L));
    }

    @Test
    void createFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studyPlanService.create(9L, new StudyPlanRequest("2022.1", null)));
    }

    @Test
    void createRejectsDuplicatedIdentifierInTheSameCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career()));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2022.1")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> studyPlanService.create(1L, new StudyPlanRequest(" 2022.1 ", null)));
        verify(studyPlanRepository, never()).save(any(StudyPlan.class));
    }

    @Test
    void createWithoutCurrentFlagLeavesThePlanNotCurrent() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career()));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2022.1")).thenReturn(false);
        stubSaveAssigningId();

        StudyPlanResponse result = studyPlanService.create(1L, new StudyPlanRequest("2022.1", null));

        assertFalse(result.current());
        assertEquals(10L, result.id());
        verify(studyPlanRepository, never()).findByCareerIdAndCurrentTrue(any());
    }

    @Test
    void createAsCurrentDemotesThePreviousCurrentPlan() {
        StudyPlan previous = plan(5L, "2021.3", true);
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career()));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2022.1")).thenReturn(false);
        stubSaveAssigningId();
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.of(previous));

        StudyPlanResponse result = studyPlanService.create(1L, new StudyPlanRequest("2022.1", true));

        assertTrue(result.current());
        assertFalse(previous.isCurrent());
        verify(studyPlanRepository).saveAndFlush(previous);
    }

    @Test
    void createAsCurrentWhenTheCareerHadNoCurrentPlan() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career()));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2022.1")).thenReturn(false);
        stubSaveAssigningId();
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.empty());

        StudyPlanResponse result = studyPlanService.create(1L, new StudyPlanRequest("2022.1", true));

        assertTrue(result.current());
        verify(studyPlanRepository, never()).saveAndFlush(any(StudyPlan.class));
    }

    @Test
    void createWithCurrentFalseDoesNotTouchOtherPlans() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career()));
        when(studyPlanRepository.existsByCareerIdAndIdentifier(1L, "2022.1")).thenReturn(false);
        stubSaveAssigningId();

        StudyPlanResponse result = studyPlanService.create(1L, new StudyPlanRequest("2022.1", false));

        assertFalse(result.current());
        verify(studyPlanRepository, never()).findByCareerIdAndCurrentTrue(any());
    }

    @Test
    void makeCurrentDemotesTheOtherPlan() {
        StudyPlan target = plan(2L, "2022.1", false);
        StudyPlan previous = plan(1L, "2021.3", true);
        when(studyPlanRepository.findById(2L)).thenReturn(Optional.of(target));
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.of(previous));

        StudyPlanResponse result = studyPlanService.makeCurrent(2L);

        assertTrue(result.current());
        assertFalse(previous.isCurrent());
        verify(studyPlanRepository).saveAndFlush(previous);
        verify(studyPlanRepository).save(target);
    }

    @Test
    void makeCurrentOnTheAlreadyCurrentPlanChangesNothingElse() {
        StudyPlan target = plan(1L, "2021.3", true);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(target));
        when(studyPlanRepository.findByCareerIdAndCurrentTrue(1L)).thenReturn(Optional.of(target));

        StudyPlanResponse result = studyPlanService.makeCurrent(1L);

        assertTrue(result.current());
        verify(studyPlanRepository, never()).saveAndFlush(any(StudyPlan.class));
    }

    @Test
    void makeCurrentFailsWhenThePlanDoesNotExist() {
        when(studyPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.makeCurrent(9L));
    }

    @Test
    void deleteRemovesAnEmptyPlan() {
        StudyPlan plan = plan(1L, "2021.3", false);
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(false);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of());

        studyPlanService.delete(1L);

        verify(studyPlanRepository).delete(plan);
    }

    @Test
    void deleteRejectsAPlanWithStudents() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "2021.3", false)));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> studyPlanService.delete(1L));
        verify(studyPlanRepository, never()).delete(any(StudyPlan.class));
    }

    @Test
    void deleteRejectsAPlanWithCourses() {
        when(studyPlanRepository.findById(1L)).thenReturn(Optional.of(plan(1L, "2021.3", false)));
        when(studentRepository.existsByStudyPlanId(1L)).thenReturn(false);
        when(courseRepository.findByStudyPlanId(1L)).thenReturn(List.of(new Course()));

        assertThrows(ConflictException.class, () -> studyPlanService.delete(1L));
        verify(studyPlanRepository, never()).delete(any(StudyPlan.class));
    }

    @Test
    void deleteFailsWhenThePlanDoesNotExist() {
        when(studyPlanRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studyPlanService.delete(9L));
    }
}
