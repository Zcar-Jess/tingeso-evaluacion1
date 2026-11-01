package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.PeriodRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodRegistrationRepository extends JpaRepository<PeriodRegistration, Long> {
    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);
}
