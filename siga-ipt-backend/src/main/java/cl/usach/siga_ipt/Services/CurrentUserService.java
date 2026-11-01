package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.MeResponse;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ForbiddenException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Links the Keycloak user of the request with the Student or Teacher of the database through the
 * {@code email} claim of the token. Used in {@code @PreAuthorize("... @currentUser.isStudent(#id)")}.
 *
 * <p>Without a JWT (the {@code dev} profile) there is no current user: the checks that need one
 * answer false, and {@link #resolveTeacherId(Long)} falls back to the id sent in the request.
 */
@Service("currentUser")
@RequiredArgsConstructor
public class CurrentUserService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;

    /** True if the logged-in user is the student with this id. */
    @Transactional(readOnly = true)
    public boolean isStudent(Long studentId) {
        return currentStudent().map(s -> s.getId().equals(studentId)).orElse(false);
    }

    /** True if the logged-in user is the teacher with this id. */
    @Transactional(readOnly = true)
    public boolean isTeacher(Long teacherId) {
        return currentTeacher().map(t -> t.getId().equals(teacherId)).orElse(false);
    }

    /**
     * True if the logged-in user is the teacher responsible for the section. A section that does
     * not exist answers true, so the request reaches the service and gets a 404.
     */
    @Transactional(readOnly = true)
    public boolean teachesSection(Long sectionId) {
        return sectionRepository.findById(sectionId)
                .map(section -> currentTeacher()
                        .map(t -> t.getId().equals(section.getTeacher().getId()))
                        .orElse(false))
                .orElse(true);
    }

    /**
     * True if the enrollment belongs to the logged-in student. An enrollment that does not exist
     * answers true, so the request reaches the service and gets a 404.
     */
    @Transactional(readOnly = true)
    public boolean ownsEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .map(enrollment -> currentStudent()
                        .map(s -> s.getId().equals(enrollment.getStudent().getId()))
                        .orElse(false))
                .orElse(true);
    }

    /**
     * Teacher that is registering grades. With a token it is always the teacher linked to the
     * token, whatever the request body says. Without a token (dev profile) it is the id sent.
     */
    @Transactional(readOnly = true)
    public Long resolveTeacherId(Long requestedTeacherId) {
        if (isJwtAuthentication()) {
            return currentTeacher()
                    .map(Teacher::getId)
                    .orElseThrow(() -> new ForbiddenException(
                            "The authenticated user is not linked to any teacher"));
        }
        if (requestedTeacherId == null) {
            throw new BusinessRuleException("teacherId is required when the request is not authenticated");
        }
        return requestedTeacherId;
    }

    /** Role, linked record and name of the logged-in user. */
    @Transactional(readOnly = true)
    public MeResponse me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new ForbiddenException("There is no authenticated user");
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        Jwt jwt = token.getToken();
        String email = jwt.getClaimAsString("email");

        if (roles.contains("ROLE_ADMIN")) {
            return new MeResponse("ADMIN", null, displayName(jwt), email);
        }
        if (roles.contains("ROLE_TEACHER")) {
            Teacher teacher = currentTeacher().orElseThrow(() -> new ResourceNotFoundException(
                    "No teacher has the email of the authenticated user"));
            return new MeResponse("TEACHER", teacher.getId(), teacher.getFullName(), teacher.getEmail());
        }
        if (roles.contains("ROLE_STUDENT")) {
            Student student = currentStudent().orElseThrow(() -> new ResourceNotFoundException(
                    "No student has the email of the authenticated user"));
            return new MeResponse("STUDENT", student.getId(), student.getFullName(), student.getEmail());
        }
        throw new ForbiddenException("The user has no SIGA IPT role (ADMIN, TEACHER or STUDENT)");
    }

    private Optional<Student> currentStudent() {
        return currentEmail().flatMap(studentRepository::findByEmailIgnoreCase);
    }

    private Optional<Teacher> currentTeacher() {
        return currentEmail().flatMap(teacherRepository::findByEmailIgnoreCase);
    }

    private Optional<String> currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            return Optional.ofNullable(token.getToken().getClaimAsString("email"))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty());
        }
        return Optional.empty();
    }

    private boolean isJwtAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken;
    }

    private String displayName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null ? username : jwt.getClaimAsString("email");
    }
}
