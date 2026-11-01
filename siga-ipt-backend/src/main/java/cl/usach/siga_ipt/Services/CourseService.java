package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.CourseRequest;
import cl.usach.siga_ipt.Dtos.CourseResponse;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final int MAX_PREREQUISITES = 3;
    private static final int MAX_WEEKLY_HOURS_EXCLUSIVE = 8;

    private final CourseRepository courseRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<CourseResponse> findByStudyPlan(Long studyPlanId) {
        if (!studyPlanRepository.existsById(studyPlanId)) {
            throw new ResourceNotFoundException("Study plan " + studyPlanId + " not found");
        }
        return courseRepository.findByStudyPlanId(studyPlanId).stream()
                .sorted(Comparator.comparingInt(Course::getSemester).thenComparing(Course::getCode))
                .map(CourseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(Long id) {
        return CourseResponse.from(getCourse(id));
    }

    /**
     * Rules: existing plan; code unique within the plan; semester 1-4; 0 < SCT <= 7;
     * hours >= 0 and total < 8; 0-3 prerequisites from the same plan and earlier semesters.
     */
    @Transactional
    public CourseResponse create(Long studyPlanId, CourseRequest request) {
        StudyPlan plan = studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Study plan " + studyPlanId + " not found"));
        String code = request.code().trim();
        if (courseRepository.existsByStudyPlanIdAndCode(studyPlanId, code)) {
            throw new ConflictException("The study plan already has a course with code '" + code + "'");
        }
        validateHours(request);

        Course course = Course.builder()
                .code(code)
                .name(request.name().trim())
                .semester(request.semester())
                .theoryHours(request.theoryHours())
                .exerciseHours(request.exerciseHours())
                .labHours(request.labHours())
                .sct(request.sct())
                .studyPlan(plan)
                .build();
        course.getPrerequisites().addAll(
                resolvePrerequisites(studyPlanId, null, request.semester(), request.prerequisiteIds()));
        return CourseResponse.from(courseRepository.save(course));
    }

    /**
     * Updating a course never changes the academic history, because the history keeps
     * its own copy of the course code and name.
     */
    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getCourse(id);
        Long planId = course.getStudyPlan().getId();
        String code = request.code().trim();
        if (!course.getCode().equals(code) && courseRepository.existsByStudyPlanIdAndCode(planId, code)) {
            throw new ConflictException("The study plan already has a course with code '" + code + "'");
        }
        validateHours(request);

        // Courses that use this one as prerequisite must still be in a later semester.
        for (Course dependant : findDependants(course)) {
            if (request.semester() >= dependant.getSemester()) {
                throw new BusinessRuleException("Course '" + dependant.getCode()
                        + "' has this course as prerequisite, so it must stay in an earlier semester");
            }
        }
        Set<Course> prerequisites = resolvePrerequisites(planId, id, request.semester(), request.prerequisiteIds());

        course.setCode(code);
        course.setName(request.name().trim());
        course.setSemester(request.semester());
        course.setTheoryHours(request.theoryHours());
        course.setExerciseHours(request.exerciseHours());
        course.setLabHours(request.labHours());
        course.setSct(request.sct());
        course.getPrerequisites().clear();
        course.getPrerequisites().addAll(prerequisites);
        return CourseResponse.from(courseRepository.save(course));
    }

    /** A course in the academic history, with sections, or used as prerequisite cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        Course course = getCourse(id);
        if (academicRecordRepository.existsByCourseId(id)) {
            throw new ConflictException("A course that is part of the academic history cannot be deleted");
        }
        if (sectionRepository.existsByCourseId(id)) {
            throw new ConflictException("A course with sections cannot be deleted");
        }
        if (!findDependants(course).isEmpty()) {
            throw new ConflictException("The course is a prerequisite of other courses and cannot be deleted");
        }
        course.getPrerequisites().clear();
        courseRepository.delete(course);
    }

    private List<Course> findDependants(Course course) {
        return courseRepository.findByStudyPlanId(course.getStudyPlan().getId()).stream()
                .filter(c -> c.getPrerequisites().contains(course))
                .toList();
    }

    private void validateHours(CourseRequest request) {
        int total = request.theoryHours() + request.exerciseHours() + request.labHours();
        if (total >= MAX_WEEKLY_HOURS_EXCLUSIVE) {
            throw new BusinessRuleException("Total weekly hours (theory + exercises + lab) must be lower than "
                    + MAX_WEEKLY_HOURS_EXCLUSIVE + " but was " + total);
        }
    }

    private Set<Course> resolvePrerequisites(Long planId, Long selfId, int semester, List<Long> ids) {
        Set<Course> result = new HashSet<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        Set<Long> unique = new LinkedHashSet<>(ids);
        if (unique.size() > MAX_PREREQUISITES) {
            throw new BusinessRuleException("A course can have at most " + MAX_PREREQUISITES + " prerequisites");
        }
        for (Long prerequisiteId : unique) {
            if (prerequisiteId.equals(selfId)) {
                throw new BusinessRuleException("A course cannot be its own prerequisite");
            }
            Course prerequisite = courseRepository.findById(prerequisiteId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "Prerequisite course " + prerequisiteId + " does not exist"));
            if (!prerequisite.getStudyPlan().getId().equals(planId)) {
                throw new BusinessRuleException("Prerequisite '" + prerequisite.getCode()
                        + "' does not belong to the same study plan");
            }
            if (prerequisite.getSemester() >= semester) {
                throw new BusinessRuleException("Prerequisite '" + prerequisite.getCode()
                        + "' must belong to an earlier semester");
            }
            result.add(prerequisite);
        }
        return result;
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course " + id + " not found"));
    }
}
