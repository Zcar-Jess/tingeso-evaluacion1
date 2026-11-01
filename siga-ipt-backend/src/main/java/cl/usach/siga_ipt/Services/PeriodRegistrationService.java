package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.PeriodRegistrationResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.PeriodRegistration;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.PeriodRegistrationRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The administrative registration (matrícula) process is outside SIGA IPT. This service only
 * records that a student is registered in a period, which the enrollment process checks.
 */
@Service
@RequiredArgsConstructor
public class PeriodRegistrationService {

    private final PeriodRegistrationRepository registrationRepository;
    private final AcademicPeriodRepository periodRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public PeriodRegistrationResponse register(Long periodId, Long studentId) {
        AcademicPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period " + periodId + " not found"));
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + studentId + " not found"));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + period.getLabel() + " is CLOSED");
        }
        if (registrationRepository.existsByStudentIdAndPeriodId(studentId, periodId)) {
            throw new ConflictException("The student is already registered in period " + period.getLabel());
        }
        PeriodRegistration registration = registrationRepository.save(
                PeriodRegistration.builder().student(student).period(period).build());
        return PeriodRegistrationResponse.from(registration);
    }

    @Transactional(readOnly = true)
    public List<PeriodRegistrationResponse> findByPeriod(Long periodId) {
        if (!periodRepository.existsById(periodId)) {
            throw new ResourceNotFoundException("Academic period " + periodId + " not found");
        }
        return registrationRepository.findAll().stream()
                .filter(r -> r.getPeriod().getId().equals(periodId))
                .map(PeriodRegistrationResponse::from)
                .toList();
    }
}
