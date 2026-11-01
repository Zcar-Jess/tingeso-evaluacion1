package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.TeacherRequest;
import cl.usach.siga_ipt.Dtos.TeacherResponse;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Entities.TeacherStatus;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll(TeacherStatus status) {
        return teacherRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .map(TeacherResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getTeacher(id));
    }

    /** Rules: unique RUN and email; a new teacher starts as ACTIVE. */
    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        String run = request.run().trim();
        String email = request.email().trim().toLowerCase();
        if (teacherRepository.existsByRun(run)) {
            throw new ConflictException("A teacher with RUN '" + run + "' already exists");
        }
        if (teacherRepository.existsByEmail(email)) {
            throw new ConflictException("A teacher with email '" + email + "' already exists");
        }
        Teacher teacher = Teacher.builder()
                .run(run)
                .fullName(request.fullName().trim())
                .email(email)
                .professionalTitle(request.professionalTitle().trim())
                .academicDegree(request.academicDegree().trim())
                .status(TeacherStatus.ACTIVE)
                .build();
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = getTeacher(id);
        String run = request.run().trim();
        String email = request.email().trim().toLowerCase();
        if (!teacher.getRun().equals(run) && teacherRepository.existsByRun(run)) {
            throw new ConflictException("A teacher with RUN '" + run + "' already exists");
        }
        if (!teacher.getEmail().equalsIgnoreCase(email) && teacherRepository.existsByEmail(email)) {
            throw new ConflictException("A teacher with email '" + email + "' already exists");
        }
        teacher.setRun(run);
        teacher.setFullName(request.fullName().trim());
        teacher.setEmail(email);
        teacher.setProfessionalTitle(request.professionalTitle().trim());
        teacher.setAcademicDegree(request.academicDegree().trim());
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    /**
     * A teacher cannot become INACTIVE while assigned to sections of an OPEN period.
     * Changing the status does not touch sections already taught.
     */
    @Transactional
    public TeacherResponse changeStatus(Long id, TeacherStatus status) {
        Teacher teacher = getTeacher(id);
        if (status == TeacherStatus.INACTIVE
                && sectionRepository.existsByTeacherIdAndPeriodStatus(id, PeriodStatus.OPEN)) {
            throw new ConflictException("The teacher has sections in an OPEN academic period and cannot be set INACTIVE");
        }
        teacher.setStatus(status);
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    /** A teacher with sections cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        Teacher teacher = getTeacher(id);
        if (sectionRepository.existsByTeacherId(id)) {
            throw new ConflictException("A teacher with sections cannot be deleted");
        }
        teacherRepository.delete(teacher);
    }

    private Teacher getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher " + id + " not found"));
    }
}
