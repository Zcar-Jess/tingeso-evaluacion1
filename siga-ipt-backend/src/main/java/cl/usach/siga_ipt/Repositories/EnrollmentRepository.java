package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countBySectionId(Long sectionId);
    boolean existsBySectionId(Long sectionId);
    boolean existsByStudentId(Long studentId);
    boolean existsByStudentIdAndCourseIdAndPeriodId(Long studentId, Long courseId, Long periodId);
    List<Enrollment> findByStudentIdAndPeriodId(Long studentId, Long periodId);
    List<Enrollment> findBySectionId(Long sectionId);
    List<Enrollment> findByPeriodId(Long periodId);
    List<Enrollment> findByPeriodIdAndFinalGradeIsNull(Long periodId);
}
