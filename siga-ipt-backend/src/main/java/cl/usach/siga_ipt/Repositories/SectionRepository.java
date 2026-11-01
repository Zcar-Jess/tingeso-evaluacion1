package cl.usach.siga_ipt.Repositories;

import cl.usach.siga_ipt.Entities.DayCode;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    boolean existsByCourseIdAndPeriodId(Long courseId, Long periodId);

    boolean existsByCourseId(Long courseId);

    List<Section> findByPeriodId(Long periodId);

    List<Section> findByTeacherId(Long teacherId);

    boolean existsByTeacherId(Long teacherId);

    boolean existsByTeacherIdAndPeriodStatus(Long teacherId, PeriodStatus status);

    /** True if the teacher already has a section in the same period at the same day + module. */
    @Query("""
            select count(s) > 0 from Section s join s.scheduleBlocks b
            where s.teacher.id = :teacherId and s.period.id = :periodId
              and b.day = :day and b.module = :module
              and (:excludeSectionId is null or s.id <> :excludeSectionId)
            """)
    boolean teacherHasConflict(@Param("teacherId") Long teacherId,
                               @Param("periodId") Long periodId,
                               @Param("day") DayCode day,
                               @Param("module") int module,
                               @Param("excludeSectionId") Long excludeSectionId);
}
