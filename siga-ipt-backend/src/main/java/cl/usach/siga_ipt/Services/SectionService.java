package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.ScheduleBlockDto;
import cl.usach.siga_ipt.Dtos.SectionRequest;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Dtos.SectionUpdateRequest;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicPeriodRepository;
import cl.usach.siga_ipt.Repositories.CourseRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SectionService {

    /** Id that never exists; used when no section must be excluded from the conflict check. */
    private static final long NO_SECTION = -1L;

    private final SectionRepository sectionRepository;
    private final AcademicPeriodRepository periodRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<SectionResponse> findByPeriod(Long periodId) {
        if (!periodRepository.existsById(periodId)) {
            throw new ResourceNotFoundException("Academic period " + periodId + " not found");
        }
        return sectionRepository.findByPeriodId(periodId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SectionResponse findById(Long id) {
        return toResponse(getSection(id));
    }

    /**
     * Rules: existing course and OPEN period; one section per course per period; ACTIVE teacher;
     * capacity 1-60; at least one valid block, without duplicates; number of blocks equal to the
     * weekly TEL hours of the course divided by 2; no teacher clash on the same day and module.
     */
    @Transactional
    public SectionResponse create(Long periodId, SectionRequest request) {
        AcademicPeriod period = getPeriod(periodId);
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course " + request.courseId() + " not found"));
        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher " + request.teacherId() + " not found"));

        requireOpen(period);
        if (sectionRepository.existsByCourseIdAndPeriodId(course.getId(), periodId)) {
            throw new ConflictException("Course '" + course.getCode() + "' already has a section in period "
                    + period.getLabel());
        }
        requireActive(teacher);
        Set<ScheduleBlock> blocks = validateBlocks(course, request.scheduleBlocks());
        checkTeacherConflicts(teacher.getId(), periodId, blocks, NO_SECTION);

        Section section = Section.builder()
                .course(course)
                .period(period)
                .teacher(teacher)
                .capacity(request.capacity())
                .build();
        section.getScheduleBlocks().addAll(blocks);
        return toResponse(sectionRepository.save(section));
    }

    /**
     * The teacher, capacity and schedule can change while the period is OPEN. The capacity cannot
     * be lower than the number of enrolled students, and the schedule cannot change once students
     * are enrolled (it could create clashes in their timetables).
     */
    @Transactional
    public SectionResponse update(Long id, SectionUpdateRequest request) {
        Section section = getSection(id);
        requireOpen(section.getPeriod());
        long enrolled = enrollmentRepository.countBySectionId(id);

        Teacher teacher = teacherRepository.findById(request.teacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher " + request.teacherId() + " not found"));
        if (!teacher.getId().equals(section.getTeacher().getId())) {
            requireActive(teacher);
        }
        if (request.capacity() < enrolled) {
            throw new BusinessRuleException("Capacity cannot be lower than the " + enrolled
                    + " students already enrolled");
        }
        Set<ScheduleBlock> blocks = validateBlocks(section.getCourse(), request.scheduleBlocks());
        if (enrolled > 0 && !blocks.equals(section.getScheduleBlocks())) {
            throw new ConflictException("The schedule cannot change while the section has enrolled students");
        }
        checkTeacherConflicts(teacher.getId(), section.getPeriod().getId(), blocks, section.getId());

        section.setTeacher(teacher);
        section.setCapacity(request.capacity());
        section.getScheduleBlocks().clear();
        section.getScheduleBlocks().addAll(blocks);
        return toResponse(sectionRepository.save(section));
    }

    /** A section with enrolled students cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        Section section = getSection(id);
        requireOpen(section.getPeriod());
        if (enrollmentRepository.existsBySectionId(id)) {
            throw new ConflictException("A section with enrolled students cannot be deleted");
        }
        sectionRepository.delete(section);
    }

    private Set<ScheduleBlock> validateBlocks(Course course, List<ScheduleBlockDto> requested) {
        Set<ScheduleBlock> blocks = new HashSet<>();
        for (ScheduleBlockDto dto : requested) {
            if (!blocks.add(dto.toEntity())) {
                throw new BusinessRuleException("Duplicated schedule block " + dto.code());
            }
        }
        int weeklyHours = course.getTheoryHours() + course.getExerciseHours() + course.getLabHours();
        int expected = (weeklyHours + 1) / 2;   // each block = 2 pedagogical hours
        if (blocks.size() != expected) {
            throw new BusinessRuleException("Course '" + course.getCode() + "' has " + weeklyHours
                    + " weekly hours, so it needs exactly " + expected + " schedule blocks but "
                    + blocks.size() + " were given");
        }
        return blocks;
    }

    private void checkTeacherConflicts(Long teacherId, Long periodId, Set<ScheduleBlock> blocks, Long excludeId) {
        for (ScheduleBlock block : blocks) {
            if (sectionRepository.teacherHasConflict(teacherId, periodId, block.getDay(),
                    block.getModule(), excludeId)) {
                throw new ConflictException("The teacher already has a section at "
                        + block.getDay().name() + block.getModule() + " in this period");
            }
        }
    }

    private void requireOpen(AcademicPeriod period) {
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessRuleException("Academic period " + period.getLabel() + " is CLOSED");
        }
    }

    private void requireActive(Teacher teacher) {
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessRuleException("Teacher " + teacher.getFullName() + " is not ACTIVE");
        }
    }

    private SectionResponse toResponse(Section section) {
        return SectionResponse.from(section, enrollmentRepository.countBySectionId(section.getId()));
    }

    private AcademicPeriod getPeriod(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic period " + id + " not found"));
    }

    private Section getSection(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Section " + id + " not found"));
    }
}
