package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.EnrollmentRequest;
import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.PeriodRegistrationRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private static final int MIN_COURSES = 3;
    private static final int MAX_COURSES = 6;

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository periodRepository;
    private final PeriodRegistrationRepository registrationRepository;
    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final EntityManager entityManager;

    /**
     * Enrolls the selected courses of a student in a period. Every rule is checked before anything
     * is saved, so the whole selection is accepted or rejected together:
     * <ul>
     *   <li>student REGULAR and registered in the period; period OPEN;</li>
     *   <li>courses from the student's study plan and offered in the period;</li>
     *   <li>not approved yet, prerequisites approved, not already enrolled in the period;</li>
     *   <li>between 3 and 6 courses in the period (fewer than 3 only if fewer than 3 are pending);</li>
     *   <li>no higher-semester course while an available lower-semester course is left out;</li>
     *   <li>no schedule clash between the sections;</li>
     *   <li>seats available (the sections are locked so concurrent requests cannot overfill them).</li>
     * </ul>
     */
    @Transactional
    public List<EnrollmentResponse> enroll(Long periodId, EnrollmentRequest request) {
        Student student = getStudent(request.studentId());
        AcademicPeriod period = getPeriod(periodId);

        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + period.getLabel() + " is CLOSED");
        }
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new BusinessRuleException("Only REGULAR students can enroll; the student is " + student.getStatus());
        }
        if (!registrationRepository.existsByStudentIdAndPeriodId(student.getId(), periodId)) {
            throw new BusinessRuleException("The student is not registered in period " + period.getLabel());
        }

        Set<Long> requestedIds = new LinkedHashSet<>(request.courseIds());
        if (requestedIds.size() != request.courseIds().size()) {
            throw new BusinessRuleException("The same course was selected more than once");
        }

        Map<Long, Section> offered = offeredByCourse(periodId);
        List<Enrollment> existing = enrollmentRepository.findByStudentIdAndPeriodId(student.getId(), periodId);
        Set<Long> enrolledCourseIds = existing.stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());
        Set<Long> passedIds = passedCourseIds(student.getId());

        List<Course> selected = new ArrayList<>();
        for (Long courseId : requestedIds) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course " + courseId + " not found"));
            if (!course.getStudyPlan().getId().equals(student.getStudyPlan().getId())) {
                throw new BusinessRuleException("Course '" + course.getCode()
                        + "' does not belong to the student's study plan");
            }
            if (!offered.containsKey(courseId)) {
                throw new BusinessRuleException("Course '" + course.getCode()
                        + "' is not offered in period " + period.getLabel());
            }
            if (enrolledCourseIds.contains(courseId)) {
                throw new ConflictException("The student is already enrolled in '" + course.getCode()
                        + "' in this period");
            }
            if (passedIds.contains(courseId)) {
                throw new BusinessRuleException("Course '" + course.getCode() + "' is already approved");
            }
            for (Course prerequisite : course.getPrerequisites()) {
                if (!passedIds.contains(prerequisite.getId())) {
                    throw new BusinessRuleException("Course '" + course.getCode()
                            + "' requires the approved prerequisite '" + prerequisite.getCode() + "'");
                }
            }
            selected.add(course);
        }

        List<Course> planCourses = courseRepository.findByStudyPlanId(student.getStudyPlan().getId());
        checkCourseCount(existing.size() + selected.size(), planCourses, passedIds);

        Set<Long> takenIds = new HashSet<>(enrolledCourseIds);
        selected.forEach(c -> takenIds.add(c.getId()));
        checkLowerSemesters(selected, planCourses, offered, passedIds, takenIds);

        List<Section> allSections = new ArrayList<>(existing.stream().map(Enrollment::getSection).toList());
        selected.forEach(c -> allSections.add(offered.get(c.getId())));
        checkScheduleClashes(allSections);

        checkSeats(selected.stream().map(c -> offered.get(c.getId())).toList());

        LocalDate today = LocalDate.now();
        List<EnrollmentResponse> result = new ArrayList<>();
        for (Course course : selected) {
            Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                    .student(student)
                    .course(course)
                    .section(offered.get(course.getId()))
                    .period(period)
                    .enrollmentDate(today)
                    .build());
            result.add(EnrollmentResponse.from(enrollment));
        }
        return result;
    }

    /**
     * Cancels an enrollment, freeing its seat. Only while the period is OPEN, and not once a final
     * grade has been registered.
     */
    @Transactional
    public void annul(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment " + enrollmentId + " not found"));
        if (enrollment.getPeriod().getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + enrollment.getPeriod().getLabel() + " is CLOSED");
        }
        if (enrollment.getFinalGrade() != null) {
            throw new ConflictException("An enrollment with a registered final grade cannot be cancelled");
        }
        enrollmentRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findByStudent(Long studentId, Long periodId) {
        getStudent(studentId);
        getPeriod(periodId);
        return enrollmentRepository.findByStudentIdAndPeriodId(studentId, periodId).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    /** Enrolled students of a section, with their grades. */
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> findBySection(Long sectionId) {
        if (!sectionRepository.existsById(sectionId)) {
            throw new ResourceNotFoundException("Section " + sectionId + " not found");
        }
        return enrollmentRepository.findBySectionId(sectionId).stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    /**
     * Sections the student could enroll in during the period: courses of the student's plan that
     * are offered, not approved, with approved prerequisites and not already enrolled.
     */
    @Transactional(readOnly = true)
    public List<SectionResponse> findAvailableSections(Long studentId, Long periodId) {
        Student student = getStudent(studentId);
        getPeriod(periodId);
        Set<Long> passedIds = passedCourseIds(studentId);
        Set<Long> enrolledIds = enrollmentRepository.findByStudentIdAndPeriodId(studentId, periodId).stream()
                .map(e -> e.getCourse().getId())
                .collect(Collectors.toSet());

        return sectionRepository.findByPeriodId(periodId).stream()
                .filter(s -> s.getCourse().getStudyPlan().getId().equals(student.getStudyPlan().getId()))
                .filter(s -> !passedIds.contains(s.getCourse().getId()))
                .filter(s -> !enrolledIds.contains(s.getCourse().getId()))
                .filter(s -> prerequisitesPassed(s.getCourse(), passedIds))
                .sorted(Comparator.comparingInt((Section s) -> s.getCourse().getSemester())
                        .thenComparing(s -> s.getCourse().getCode()))
                .map(s -> SectionResponse.from(s, enrollmentRepository.countBySectionId(s.getId())))
                .toList();
    }

    // ------------------------------------------------------------------ rule helpers

    /** 3..6 courses in the period; fewer than 3 are allowed only when fewer than 3 are pending. */
    private void checkCourseCount(int total, List<Course> planCourses, Set<Long> passedIds) {
        if (total > MAX_COURSES) {
            throw new BusinessRuleException("A student can enroll at most " + MAX_COURSES
                    + " courses per period but would have " + total);
        }
        long pending = planCourses.stream().filter(c -> !passedIds.contains(c.getId())).count();
        int minimum = pending >= MIN_COURSES ? MIN_COURSES : 1;
        if (total < minimum) {
            throw new BusinessRuleException("A student must enroll at least " + minimum
                    + " courses per period but would have " + total);
        }
    }

    /** A higher-semester course cannot be taken while an available lower-semester one is left out. */
    private void checkLowerSemesters(List<Course> selected, List<Course> planCourses,
                                     Map<Long, Section> offered, Set<Long> passedIds, Set<Long> takenIds) {
        for (Course candidate : planCourses) {
            boolean available = !passedIds.contains(candidate.getId())
                    && !takenIds.contains(candidate.getId())
                    && offered.containsKey(candidate.getId())
                    && prerequisitesPassed(candidate, passedIds);
            if (!available) {
                continue;
            }
            for (Course course : selected) {
                if (course.getSemester() > candidate.getSemester()) {
                    throw new BusinessRuleException("Course '" + course.getCode() + "' (semester "
                            + course.getSemester() + ") cannot be enrolled while the available course '"
                            + candidate.getCode() + "' of semester " + candidate.getSemester()
                            + " is pending");
                }
            }
        }
    }

    private void checkScheduleClashes(List<Section> sections) {
        Map<ScheduleBlock, String> used = new HashMap<>();
        for (Section section : sections) {
            for (ScheduleBlock block : section.getScheduleBlocks()) {
                String previous = used.put(block, section.getCourse().getCode());
                if (previous != null) {
                    throw new ConflictException("Schedule clash at " + block.getDay().name() + block.getModule()
                            + " between '" + previous + "' and '" + section.getCourse().getCode() + "'");
                }
            }
        }
    }

    /**
     * Locks the sections (always in id order, to avoid deadlocks) and checks their free seats.
     * The lock lasts until the transaction ends, so two replicas of the backend cannot both take
     * the last seat.
     */
    private void checkSeats(List<Section> sections) {
        List<Section> ordered = sections.stream().sorted(Comparator.comparing(Section::getId)).toList();
        for (Section section : ordered) {
            Section locked = entityManager.find(Section.class, section.getId(), LockModeType.PESSIMISTIC_WRITE);
            long enrolled = enrollmentRepository.countBySectionId(locked.getId());
            if (enrolled >= locked.getCapacity()) {
                throw new ConflictException("The section of '" + locked.getCourse().getCode() + "' has no free seats");
            }
        }
    }

    private boolean prerequisitesPassed(Course course, Set<Long> passedIds) {
        return course.getPrerequisites().stream().allMatch(p -> passedIds.contains(p.getId()));
    }

    private Map<Long, Section> offeredByCourse(Long periodId) {
        return sectionRepository.findByPeriodId(periodId).stream()
                .collect(Collectors.toMap(s -> s.getCourse().getId(), Function.identity()));
    }

    private Set<Long> passedCourseIds(Long studentId) {
        return academicRecordRepository.findByStudentId(studentId).stream()
                .filter(r -> r.getResult() == CourseResult.PASSED)
                .map((AcademicRecord r) -> r.getCourse().getId())
                .collect(Collectors.toSet());
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + id + " not found"));
    }

    private AcademicPeriod getPeriod(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period " + id + " not found"));
    }
}
