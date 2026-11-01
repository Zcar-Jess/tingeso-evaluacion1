package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);
    List<Course> findByStudyPlanId(Long studyPlanId);
}
