package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.AcademicRecordResponse;
import cl.usach.siga_ipt.Dtos.StudentRequest;
import cl.usach.siga_ipt.Dtos.StudentResponse;
import cl.usach.siga_ipt.Dtos.StudentUpdateRequest;
import cl.usach.siga_ipt.Entities.Career;
import cl.usach.siga_ipt.Entities.CareerStatus;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ConflictException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.AcademicRecordRepository;
import cl.usach.siga_ipt.Repositories.CareerRepository;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CareerRepository careerRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicRecordRepository academicRecordRepository;

    /** Optional filters: status and career. */
    @Transactional(readOnly = true)
    public List<StudentResponse> findAll(StudentStatus status, Long careerId) {
        return studentRepository.findAll().stream()
                .filter(s -> status == null || s.getStatus() == status)
                .filter(s -> careerId == null || s.getCareer().getId().equals(careerId))
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return StudentResponse.from(getStudent(id));
    }

    /**
     * Rules: unique RUN and email; the career must exist and be ACTIVE; the student is linked to
     * the current study plan of the career; initial status is REGULAR.
     */
    @Transactional
    public StudentResponse create(StudentRequest request) {
        Career career = careerRepository.findById(request.careerId())
                .orElseThrow(() -> new ResourceNotFoundException("Career " + request.careerId() + " not found"));
        String run = request.run().trim();
        String email = request.email().trim().toLowerCase();
        if (studentRepository.existsByRun(run)) {
            throw new ConflictException("A student with RUN '" + run + "' already exists");
        }
        if (studentRepository.existsByEmail(email)) {
            throw new ConflictException("A student with email '" + email + "' already exists");
        }
        requireActive(career);
        StudyPlan plan = currentPlanOf(career);

        Student student = Student.builder()
                .run(run)
                .fullName(request.fullName().trim())
                .email(email)
                .status(StudentStatus.REGULAR)
                .career(career)
                .studyPlan(plan)
                .build();
        return StudentResponse.from(studentRepository.save(student));
    }

    /**
     * Career and study plan can only change while the student has no enrollments and no
     * academic history. A new career must be ACTIVE.
     */
    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = getStudent(id);
        String email = request.email().trim().toLowerCase();
        if (!student.getEmail().equalsIgnoreCase(email) && studentRepository.existsByEmail(email)) {
            throw new ConflictException("A student with email '" + email + "' already exists");
        }

        boolean careerChanges = request.careerId() != null
                && !request.careerId().equals(student.getCareer().getId());
        boolean planChanges = request.studyPlanId() != null
                && !request.studyPlanId().equals(student.getStudyPlan().getId());

        if (careerChanges || planChanges) {
            if (hasEnrollmentsOrHistory(id)) {
                throw new ConflictException(
                        "Career and study plan cannot change once the student has enrollments or academic history");
            }
            Career career = student.getCareer();
            if (careerChanges) {
                career = careerRepository.findById(request.careerId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Career " + request.careerId() + " not found"));
                requireActive(career);
            }
            StudyPlan plan;
            if (request.studyPlanId() != null && (planChanges || careerChanges)) {
                plan = studyPlanRepository.findById(request.studyPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Study plan " + request.studyPlanId() + " not found"));
                if (!plan.getCareer().getId().equals(career.getId())) {
                    throw new BusinessRuleException("The study plan does not belong to the student's career");
                }
            } else {
                plan = currentPlanOf(career);
            }
            student.setCareer(career);
            student.setStudyPlan(plan);
        }

        student.setFullName(request.fullName().trim());
        student.setEmail(email);
        return StudentResponse.from(studentRepository.save(student));
    }

    /**
     * The administrator can set REGULAR, POSTPONED and TEMPORARY_WITHDRAWAL. GRADUATED and
     * ELIMINATED are assigned only by the period closing process.
     */
    @Transactional
    public StudentResponse changeStatus(Long id, StudentStatus newStatus) {
        Student student = getStudent(id);
        if (newStatus == StudentStatus.GRADUATED || newStatus == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException(
                    "GRADUATED and ELIMINATED are assigned automatically when an academic period is closed");
        }
        if (student.getStatus() == StudentStatus.GRADUATED || student.getStatus() == StudentStatus.ELIMINATED) {
            throw new BusinessRuleException(
                    "The status of a " + student.getStatus() + " student cannot be changed manually");
        }
        student.setStatus(newStatus);
        return StudentResponse.from(studentRepository.save(student));
    }

    /** A student with enrollments or academic history cannot be deleted. */
    @Transactional
    public void delete(Long id) {
        Student student = getStudent(id);
        if (hasEnrollmentsOrHistory(id)) {
            throw new ConflictException("A student with enrollments or academic history cannot be deleted");
        }
        studentRepository.delete(student);
    }

    @Transactional(readOnly = true)
    public List<AcademicRecordResponse> getAcademicHistory(Long id) {
        getStudent(id);
        return academicRecordRepository.findByStudentId(id).stream()
                .map(AcademicRecordResponse::from)
                .toList();
    }

    private boolean hasEnrollmentsOrHistory(Long studentId) {
        return enrollmentRepository.existsByStudentId(studentId)
                || academicRecordRepository.existsByStudentId(studentId);
    }

    private void requireActive(Career career) {
        if (career.getStatus() == CareerStatus.INACTIVE) {
            throw new BusinessRuleException("An INACTIVE career cannot receive new students");
        }
    }

    private StudyPlan currentPlanOf(Career career) {
        return studyPlanRepository.findByCareerIdAndCurrentTrue(career.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Career '" + career.getCode() + "' has no current study plan"));
    }

    private Student getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student " + id + " not found"));
    }
}
