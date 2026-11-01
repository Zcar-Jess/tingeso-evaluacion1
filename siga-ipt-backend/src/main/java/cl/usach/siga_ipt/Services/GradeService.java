package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.GradeRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ForbiddenException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Registers (or modifies) the final grade of a student in a section. Rules: only the teacher
     * responsible for the section; the student must be enrolled in it; the period must be OPEN;
     * the grade is between 1.0 and 7.0 (validated in the request).
     */
    @Transactional
    public EnrollmentResponse registerGrade(Long sectionId, Long studentId, GradeRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section " + sectionId + " not found"));
        if (!section.getTeacher().getId().equals(request.teacherId())) {
            throw new ForbiddenException("Only the teacher responsible for the section can register its grades");
        }
        if (section.getPeriod().getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + section.getPeriod().getLabel()
                    + " is CLOSED; grades can no longer be changed");
        }
        Enrollment enrollment = enrollmentRepository.findBySectionId(sectionId).stream()
                .filter(e -> e.getStudent().getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "The student is not enrolled in this section"));

        enrollment.setFinalGrade(request.finalGrade().setScale(1, RoundingMode.HALF_UP));
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    /** Sections assigned to a teacher, optionally limited to one period. */
    @Transactional(readOnly = true)
    public List<SectionResponse> findSectionsByTeacher(Long teacherId, Long periodId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Teacher " + teacherId + " not found");
        }
        return sectionRepository.findByTeacherId(teacherId).stream()
                .filter(s -> periodId == null || s.getPeriod().getId().equals(periodId))
                .map(s -> SectionResponse.from(s, enrollmentRepository.countBySectionId(s.getId())))
                .toList();
    }

    /** Smallest passing grade, exposed for the closing process. */
    public static final BigDecimal PASSING_GRADE = new BigDecimal("4.0");
}
