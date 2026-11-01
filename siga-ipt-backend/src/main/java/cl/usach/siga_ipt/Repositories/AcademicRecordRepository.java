package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.CourseResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    List<AcademicRecord> findByStudentId(Long studentId);
    boolean existsByStudentId(Long studentId);
    boolean existsByCourseId(Long courseId);
    boolean existsByStudentIdAndCourseIdAndResult(Long studentId, Long courseId, CourseResult result);
    long countByStudentIdAndCourseIdAndResult(Long studentId, Long courseId, CourseResult result);
}
