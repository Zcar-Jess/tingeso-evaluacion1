package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.CareerRequest;
import cl.usach.siga_ipt.Dtos.CareerResponse;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareerServiceTest {

    @Mock
    private CareerRepository careerRepository;
    @Mock
    private StudyPlanRepository studyPlanRepository;
    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CareerService careerService;

    private static Career career(Long id, String code) {
        return Career.builder().id(id).code(code).name("Name " + code)
                .description("Description " + code).status(CareerStatus.ACTIVE).build();
    }

    @Test
    void findAllReturnsEveryCareer() {
        when(careerRepository.findAll()).thenReturn(List.of(career(1L, "10450"), career(2L, "10451")));

        List<CareerResponse> result = careerService.findAll();

        assertEquals(2, result.size());
        assertEquals("10450", result.get(0).code());
        assertEquals("10451", result.get(1).code());
    }

    @Test
    void findByIdReturnsTheCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));

        CareerResponse result = careerService.findById(1L);

        assertEquals(1L, result.id());
        assertEquals(CareerStatus.ACTIVE, result.status());
    }

    @Test
    void findByIdFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.findById(9L));
    }

    @Test
    void createTrimsFieldsAndStartsActive() {
        when(careerRepository.existsByCode("10450")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse result = careerService.create(new CareerRequest(" 10450 ", " Name ", " Desc "));

        ArgumentCaptor<Career> captor = ArgumentCaptor.forClass(Career.class);
        verify(careerRepository).save(captor.capture());
        assertEquals("10450", captor.getValue().getCode());
        assertEquals("Name", captor.getValue().getName());
        assertEquals("Desc", captor.getValue().getDescription());
        assertEquals(CareerStatus.ACTIVE, result.status());
    }

    @Test
    void createRejectsDuplicatedCode() {
        when(careerRepository.existsByCode("10450")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> careerService.create(new CareerRequest("10450", "Name", "Desc")));
        verify(careerRepository, never()).save(any(Career.class));
    }

    @Test
    void updateKeepingTheSameCodeDoesNotCheckUniqueness() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse result = careerService.update(1L, new CareerRequest("10450", " New name ", " New desc "));

        assertEquals("New name", result.name());
        assertEquals("New desc", result.description());
        verify(careerRepository, never()).existsByCode(any());
    }

    @Test
    void updateChangingToAFreeCodeSucceeds() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(careerRepository.existsByCode("20000")).thenReturn(false);
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse result = careerService.update(1L, new CareerRequest("20000", "Name", "Desc"));

        assertEquals("20000", result.code());
    }

    @Test
    void updateRejectsACodeUsedByAnotherCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(careerRepository.existsByCode("10451")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> careerService.update(1L, new CareerRequest("10451", "Name", "Desc")));
        verify(careerRepository, never()).save(any(Career.class));
    }

    @Test
    void updateFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> careerService.update(9L, new CareerRequest("1", "Name", "Desc")));
    }

    @Test
    void changeStatusUpdatesTheCareer() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(careerRepository.save(any(Career.class))).thenAnswer(inv -> inv.getArgument(0));

        CareerResponse result = careerService.changeStatus(1L, CareerStatus.INACTIVE);

        assertEquals(CareerStatus.INACTIVE, result.status());
    }

    @Test
    void changeStatusFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.changeStatus(9L, CareerStatus.INACTIVE));
    }

    @Test
    void deleteRemovesACareerWithoutStudentsNorPlans() {
        Career career = career(1L, "10450");
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career));
        when(studentRepository.existsByCareerId(1L)).thenReturn(false);
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(false);

        careerService.delete(1L);

        verify(careerRepository).delete(career);
    }

    @Test
    void deleteRejectsACareerWithStudents() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(studentRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> careerService.delete(1L));
        verify(careerRepository, never()).delete(any(Career.class));
    }

    @Test
    void deleteRejectsACareerWithStudyPlans() {
        when(careerRepository.findById(1L)).thenReturn(Optional.of(career(1L, "10450")));
        when(studentRepository.existsByCareerId(1L)).thenReturn(false);
        when(studyPlanRepository.existsByCareerId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> careerService.delete(1L));
        verify(careerRepository, never()).delete(any(Career.class));
    }

    @Test
    void deleteFailsWhenTheCareerDoesNotExist() {
        when(careerRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> careerService.delete(9L));
    }
}
