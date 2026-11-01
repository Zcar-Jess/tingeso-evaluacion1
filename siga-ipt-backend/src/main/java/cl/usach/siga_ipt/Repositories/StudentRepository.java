package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);
    Optional<Student> findByEmailIgnoreCase(String email);
    boolean existsByCareerId(Long careerId);
    boolean existsByStudyPlanId(Long studyPlanId);
    List<Student> findByStatus(StudentStatus status);
}