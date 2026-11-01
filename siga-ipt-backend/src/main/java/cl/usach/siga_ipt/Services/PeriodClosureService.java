package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.AcademicPeriodResponse;
import cl.usach.siga_ipt.Dtos.PeriodClosureResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.IncompleteGradesException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodClosureService {

    private static final int FAILURES_TO_BE_ELIMINATED = 2;

    private final AcademicPeriodRepository periodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    /**
     * Closes an OPEN period. All enrollments must have a final grade; otherwise the closing is
     * rejected and the incomplete sections are reported. Then, in one transaction:
     * the result of every enrollment (PASSED when grade &gt;= 4.0) goes to the academic history;
     * a student failing the same course for the second time becomes ELIMINATED; a student who has
     * approved every course of the study plan becomes GRADUATED; and the period becomes CLOSED.
     */
    @Transactional
    public PeriodClosureResponse close(Long periodId) {
        AcademicPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period " + periodId + " not found"));
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + period.getLabel() + " is already CLOSED");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByPeriodId(periodId);
        rejectIfGradesMissing(enrollments);

        Map<Long, Student> students = new LinkedHashMap<>();
        for (Enrollment enrollment : enrollments) {
            CourseResult result = enrollment.getFinalGrade().compareTo(GradeService.PASSING_GRADE) >= 0
                    ? CourseResult.PASSED : CourseResult.FAILED;
            Course course = enrollment.getCourse();
            academicRecordRepository.save(AcademicRecord.builder()
                    .student(enrollment.getStudent())
                    .course(course)
                    .period(period)
                    .finalGrade(enrollment.getFinalGrade())
                    .result(result)
                    .courseCode(course.getCode())
                    .courseName(course.getName())
                    .build());
            students.putIfAbsent(enrollment.getStudent().getId(), enrollment.getStudent());
        }

        List<String> eliminated = new ArrayList<>();
        List<String> graduated = new ArrayList<>();
        for (Student student : students.values()) {
            if (hasFailedTwice(student, enrollments)) {
                student.setStatus(StudentStatus.ELIMINATED);
                eliminated.add(student.getRun());
                studentRepository.save(student);
            } else if (hasApprovedWholePlan(student)) {
                student.setStatus(StudentStatus.GRADUATED);
                graduated.add(student.getRun());
                studentRepository.save(student);
            }
        }

        period.setStatus(PeriodStatus.CLOSED);
        periodRepository.save(period);
        return new PeriodClosureResponse(AcademicPeriodResponse.from(period), enrollments.size(),
                eliminated, graduated);
    }

    /** Lists, per section, how many enrollments still lack a final grade. */
    private void rejectIfGradesMissing(List<Enrollment> enrollments) {
        Map<Long, List<Enrollment>> pendingBySection = enrollments.stream()
                .filter(e -> e.getFinalGrade() == null)
                .collect(Collectors.groupingBy(e -> e.getSection().getId(), TreeMap::new, Collectors.toList()));
        if (pendingBySection.isEmpty()) {
            return;
        }
        List<String> details = pendingBySection.entrySet().stream()
                .map(entry -> {
                    Enrollment sample = entry.getValue().get(0);
                    return "Section " + entry.getKey() + " (" + sample.getCourse().getCode() + ", teacher "
                            + sample.getSection().getTeacher().getFullName() + "): "
                            + entry.getValue().size() + " enrollment(s) without final grade";
                })
                .toList();
        throw new IncompleteGradesException(
                "The period cannot be closed: some sections have enrollments without a final grade", details);
    }

    /** True if, among the courses taken in this period, one now has two failed records. */
    private boolean hasFailedTwice(Student student, List<Enrollment> periodEnrollments) {
        return periodEnrollments.stream()
                .filter(e -> e.getStudent().getId().equals(student.getId()))
                .map(e -> e.getCourse().getId())
                .anyMatch(courseId -> academicRecordRepository.countByStudentIdAndCourseIdAndResult(
                        student.getId(), courseId, CourseResult.FAILED) >= FAILURES_TO_BE_ELIMINATED);
    }

    private boolean hasApprovedWholePlan(Student student) {
        Set<Long> passed = academicRecordRepository.findByStudentId(student.getId()).stream()
                .filter(r -> r.getResult() == CourseResult.PASSED)
                .map(r -> r.getCourse().getId())
                .collect(Collectors.toSet());
        List<Course> planCourses = courseRepository.findByStudyPlanId(student.getStudyPlan().getId());
        return !planCourses.isEmpty() && planCourses.stream().allMatch(c -> passed.contains(c.getId()));
    }
}
