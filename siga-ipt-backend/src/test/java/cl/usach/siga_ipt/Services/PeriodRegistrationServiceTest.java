package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.PeriodRegistrationResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.PeriodRegistration;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.PeriodRegistrationRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodRegistrationServiceTest {

    @Mock private PeriodRegistrationRepository registrationRepository;
    @Mock private AcademicPeriodRepository periodRepository;
    @Mock private StudentRepository studentRepository;
    @InjectMocks private PeriodRegistrationService service;

    private AcademicPeriod period(Long id, PeriodStatus status) {
        return AcademicPeriod.builder().id(id).academicYear(2026).semester(2).status(status).build();
    }

    private Student student(Long id) {
        return Student.builder().id(id).run("1-" + id).fullName("Student " + id)
                .email("s" + id + "@ipt.cl").status(StudentStatus.REGULAR).build();
    }

    private PeriodRegistration registration(Long id, Student student, AcademicPeriod period) {
        return PeriodRegistration.builder().id(id).student(student).period(period).build();
    }

    @Test
    void registerSavesRegistrationForOpenPeriod() {
        AcademicPeriod period = period(1L, PeriodStatus.OPEN);
        Student student = student(2L);
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student));
        when(registrationRepository.existsByStudentIdAndPeriodId(2L, 1L)).thenReturn(false);
        when(registrationRepository.save(any(PeriodRegistration.class))).thenAnswer(inv -> {
            PeriodRegistration r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        PeriodRegistrationResponse response = service.register(1L, 2L);

        assertEquals(7L, response.id());
        assertEquals(2L, response.studentId());
        assertEquals("1-2", response.studentRun());
        assertEquals(1L, response.periodId());
        assertEquals("2026-2", response.period());
    }

    @Test
    void registerThrowsWhenPeriodMissing() {
        when(periodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.register(1L, 2L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerThrowsWhenStudentMissing() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period(1L, PeriodStatus.OPEN)));
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.register(1L, 2L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsClosedPeriod() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period(1L, PeriodStatus.CLOSED)));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student(2L)));

        assertThrows(BusinessRuleException.class, () -> service.register(1L, 2L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicatedRegistration() {
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period(1L, PeriodStatus.OPEN)));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student(2L)));
        when(registrationRepository.existsByStudentIdAndPeriodId(2L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.register(1L, 2L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void findByPeriodReturnsOnlyRegistrationsOfThatPeriod() {
        AcademicPeriod period1 = period(1L, PeriodStatus.OPEN);
        AcademicPeriod period2 = period(2L, PeriodStatus.OPEN);
        when(periodRepository.existsById(1L)).thenReturn(true);
        when(registrationRepository.findAll()).thenReturn(List.of(
                registration(10L, student(2L), period1),
                registration(11L, student(3L), period2),
                registration(12L, student(4L), period1)));

        List<PeriodRegistrationResponse> result = service.findByPeriod(1L);

        assertEquals(List.of(10L, 12L), result.stream().map(PeriodRegistrationResponse::id).toList());
    }

    @Test
    void findByPeriodThrowsWhenPeriodMissing() {
        when(periodRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.findByPeriod(1L));
        verify(registrationRepository, never()).findAll();
    }
}
