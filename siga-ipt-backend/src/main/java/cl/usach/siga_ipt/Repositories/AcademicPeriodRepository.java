package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {
    boolean existsByAcademicYearAndSemester(int academicYear, int semester);
}
