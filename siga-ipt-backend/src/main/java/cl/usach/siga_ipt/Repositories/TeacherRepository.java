package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    boolean existsByRun(String run);
    boolean existsByEmail(String email);
    Optional<Teacher> findByEmailIgnoreCase(String email);
}