package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.AcademicPeriodRequest;
import cl.usach.siga_ipt.Dtos.AcademicPeriodResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
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
class AcademicPeriodServiceTest {

    @Mock private AcademicPeriodRepository periodRepository;
    @InjectMocks private AcademicPeriodService service;

    private AcademicPeriod period(Long id, int year, int semester, PeriodStatus status) {
        return AcademicPeriod.builder().id(id).academicYear(year).semester(semester).status(status).build();
    }

    @Test
    void findAllReturnsNewestPeriodFirst() {
        when(periodRepository.findAll()).thenReturn(List.of(
                period(1L, 2025, 2, PeriodStatus.CLOSED),
                period(2L, 2026, 1, PeriodStatus.CLOSED),
                period(3L, 2026, 2, PeriodStatus.OPEN)));

        List<AcademicPeriodResponse> result = service.findAll();

        assertEquals(List.of("2026-2", "2026-1", "2025-2"),
                result.stream().map(AcademicPeriodResponse::label).toList());
    }

    @Test
    void findAllReturnsEmptyListWhenThereAreNoPeriods() {
        when(periodRepository.findAll()).thenReturn(List.of());

        assertEquals(0, service.findAll().size());
    }

    @Test
    void findByIdReturnsPeriod() {
        when(periodRepository.findById(3L)).thenReturn(Optional.of(period(3L, 2026, 2, PeriodStatus.OPEN)));

        AcademicPeriodResponse response = service.findById(3L);

        assertEquals(3L, response.id());
        assertEquals("2026-2", response.label());
        assertEquals(PeriodStatus.OPEN, response.status());
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(periodRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void createStartsOpen() {
        when(periodRepository.existsByAcademicYearAndSemester(2027, 1)).thenReturn(false);
        when(periodRepository.save(any(AcademicPeriod.class))).thenAnswer(inv -> {
            AcademicPeriod p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });

        AcademicPeriodResponse response = service.create(new AcademicPeriodRequest(2027, 1));

        assertEquals(8L, response.id());
        assertEquals(2027, response.academicYear());
        assertEquals(1, response.semester());
        assertEquals(PeriodStatus.OPEN, response.status());
    }

    @Test
    void createRejectsDuplicatedYearAndSemester() {
        when(periodRepository.existsByAcademicYearAndSemester(2027, 1)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(new AcademicPeriodRequest(2027, 1)));
        verify(periodRepository, never()).save(any());
    }
}
