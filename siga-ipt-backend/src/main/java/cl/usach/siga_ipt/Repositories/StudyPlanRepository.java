package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findByCareerIdAndCurrentTrue(Long careerId);
    boolean existsByCareerIdAndIdentifier(Long careerId, String identifier);
    boolean existsByCareerId(Long careerId);
    List<StudyPlan> findByCareerId(Long careerId);
}
