package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.AcademicPeriodRequest;
import cl.usach.siga_ipt.Dtos.AcademicPeriodResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicPeriodService {

    private final AcademicPeriodRepository periodRepository;

    @Transactional(readOnly = true)
    public List<AcademicPeriodResponse> findAll() {
        return periodRepository.findAll().stream()
                .sorted(Comparator.comparingInt(AcademicPeriod::getAcademicYear)
                        .thenComparingInt(AcademicPeriod::getSemester)
                        .reversed())
                .map(AcademicPeriodResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcademicPeriodResponse findById(Long id) {
        return AcademicPeriodResponse.from(getPeriod(id));
    }

    /** Rules: unique year + semester; a new period starts as OPEN. */
    @Transactional
    public AcademicPeriodResponse create(AcademicPeriodRequest request) {
        if (periodRepository.existsByAcademicYearAndSemester(request.academicYear(), request.semester())) {
            throw new ConflictException("Academic period " + request.academicYear() + "-"
                    + request.semester() + " already exists");
        }
        AcademicPeriod period = AcademicPeriod.builder()
                .academicYear(request.academicYear())
                .semester(request.semester())
                .status(PeriodStatus.OPEN)
                .build();
        return AcademicPeriodResponse.from(periodRepository.save(period));
    }

    private AcademicPeriod getPeriod(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period " + id + " not found"));
    }
}
