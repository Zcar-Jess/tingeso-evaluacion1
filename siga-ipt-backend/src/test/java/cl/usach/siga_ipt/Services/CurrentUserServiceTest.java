package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.MeResponse;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.Teacher;
import cl.usach.siga_ipt.Exceptions.BusinessRuleException;
import cl.usach.siga_ipt.Exceptions.ForbiddenException;
import cl.usach.siga_ipt.Exceptions.ResourceNotFoundException;
import cl.usach.siga_ipt.Repositories.EnrollmentRepository;
import cl.usach.siga_ipt.Repositories.SectionRepository;
import cl.usach.siga_ipt.Repositories.StudentRepository;
import cl.usach.siga_ipt.Repositories.TeacherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final String EMAIL = "user@ipt.cl";

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ helpers

    /** Logs in a user whose token has the given extra claims and realm roles. */
    private void authenticate(Map<String, Object> claims, String... roles) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("sub", "keycloak-id");
        claims.forEach(builder::claim);
        Jwt jwt = builder.build();
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new JwtAuthenticationToken(jwt, authorities));
        SecurityContextHolder.setContext(context);
    }

    private void authenticateWithEmail(String email, String... roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        authenticate(claims, roles);
    }

    private static Student student(Long id) {
        return Student.builder().id(id).run("1-9").fullName("Student " + id).email(EMAIL).build();
    }

    private static Teacher teacher(Long id) {
        return Teacher.builder().id(id).run("2-7").fullName("Teacher " + id).email(EMAIL).build();
    }

    // ------------------------------------------------------------------ isStudent

    @Test
    void isStudentIsTrueForTheLinkedStudent() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        assertTrue(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentIsFalseForAnotherStudentId() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        assertFalse(currentUserService.isStudent(6L));
    }

    @Test
    void isStudentIsFalseWhenNoStudentHasTheEmail() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertFalse(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentTrimsTheEmailClaim() {
        authenticateWithEmail("  " + EMAIL + " ", "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        assertTrue(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentIsFalseWithoutAuthentication() {
        assertFalse(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentIsFalseWhenTheTokenHasNoEmail() {
        authenticate(new HashMap<>(), "STUDENT");

        assertFalse(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentIsFalseWhenTheEmailClaimIsBlank() {
        authenticateWithEmail("   ", "STUDENT");

        assertFalse(currentUserService.isStudent(5L));
    }

    @Test
    void isStudentIsFalseWhenTheAuthenticationIsNotAJwt() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        assertFalse(currentUserService.isStudent(5L));
    }

    // ------------------------------------------------------------------ isTeacher

    @Test
    void isTeacherIsTrueForTheLinkedTeacher() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        assertTrue(currentUserService.isTeacher(3L));
    }

    @Test
    void isTeacherIsFalseForAnotherTeacherId() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        assertFalse(currentUserService.isTeacher(4L));
    }

    @Test
    void isTeacherIsFalseWithoutAuthentication() {
        assertFalse(currentUserService.isTeacher(3L));
    }

    // ------------------------------------------------------------------ teachesSection

    @Test
    void teachesSectionIsTrueForTheResponsibleTeacher() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(Section.builder().id(10L).teacher(teacher(3L)).build()));
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        assertTrue(currentUserService.teachesSection(10L));
    }

    @Test
    void teachesSectionIsFalseForAnotherTeacher() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(Section.builder().id(10L).teacher(teacher(8L)).build()));
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        assertFalse(currentUserService.teachesSection(10L));
    }

    @Test
    void teachesSectionIsFalseWithoutAuthentication() {
        when(sectionRepository.findById(10L))
                .thenReturn(Optional.of(Section.builder().id(10L).teacher(teacher(3L)).build()));

        assertFalse(currentUserService.teachesSection(10L));
    }

    @Test
    void teachesSectionIsTrueWhenTheSectionDoesNotExistSoTheServiceAnswers404() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(currentUserService.teachesSection(99L));
    }

    // ------------------------------------------------------------------ ownsEnrollment

    @Test
    void ownsEnrollmentIsTrueForTheEnrolledStudent() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(enrollmentRepository.findById(20L))
                .thenReturn(Optional.of(Enrollment.builder().id(20L).student(student(5L)).build()));
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        assertTrue(currentUserService.ownsEnrollment(20L));
    }

    @Test
    void ownsEnrollmentIsFalseForAnotherStudent() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(enrollmentRepository.findById(20L))
                .thenReturn(Optional.of(Enrollment.builder().id(20L).student(student(7L)).build()));
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        assertFalse(currentUserService.ownsEnrollment(20L));
    }

    @Test
    void ownsEnrollmentIsFalseWithoutAuthentication() {
        when(enrollmentRepository.findById(20L))
                .thenReturn(Optional.of(Enrollment.builder().id(20L).student(student(7L)).build()));

        assertFalse(currentUserService.ownsEnrollment(20L));
    }

    @Test
    void ownsEnrollmentIsTrueWhenTheEnrollmentDoesNotExistSoTheServiceAnswers404() {
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(currentUserService.ownsEnrollment(99L));
    }

    // ------------------------------------------------------------------ resolveTeacherId

    @Test
    void resolveTeacherIdUsesTheTeacherOfTheTokenAndIgnoresTheRequestedId() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        assertEquals(3L, currentUserService.resolveTeacherId(999L));
    }

    @Test
    void resolveTeacherIdFailsWhenTheTokenIsNotLinkedToATeacher() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> currentUserService.resolveTeacherId(3L));
    }

    @Test
    void resolveTeacherIdFallsBackToTheRequestedIdWithoutAToken() {
        assertEquals(3L, currentUserService.resolveTeacherId(3L));
    }

    @Test
    void resolveTeacherIdRequiresAnIdWithoutAToken() {
        assertThrows(BusinessRuleException.class, () -> currentUserService.resolveTeacherId(null));
    }

    // ------------------------------------------------------------------ me

    @Test
    void meFailsWithoutAuthentication() {
        assertThrows(ForbiddenException.class, () -> currentUserService.me());
    }

    @Test
    void meFailsWhenTheAuthenticationIsNotAJwt() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "password"));

        assertThrows(ForbiddenException.class, () -> currentUserService.me());
    }

    @Test
    void meOfAnAdministratorUsesTheNameClaim() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "admin@ipt.cl");
        claims.put("name", "Ada Admin");
        claims.put("preferred_username", "admin");
        authenticate(claims, "ADMIN");

        MeResponse me = currentUserService.me();

        assertEquals("ADMIN", me.role());
        assertNull(me.userId());
        assertEquals("Ada Admin", me.name());
        assertEquals("admin@ipt.cl", me.email());
    }

    @Test
    void meOfAnAdministratorFallsBackToThePreferredUsernameWhenTheNameIsBlank() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "admin@ipt.cl");
        claims.put("name", " ");
        claims.put("preferred_username", "admin");
        authenticate(claims, "ADMIN");

        assertEquals("admin", currentUserService.me().name());
    }

    @Test
    void meOfAnAdministratorFallsBackToThePreferredUsernameWhenThereIsNoName() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", "admin@ipt.cl");
        claims.put("preferred_username", "admin");
        authenticate(claims, "ADMIN");

        assertEquals("admin", currentUserService.me().name());
    }

    @Test
    void meOfAnAdministratorFallsBackToTheEmailWhenThereIsNoUsername() {
        authenticateWithEmail("admin@ipt.cl", "ADMIN");

        assertEquals("admin@ipt.cl", currentUserService.me().name());
    }

    @Test
    void meOfATeacherReturnsTheLinkedRecord() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(teacher(3L)));

        MeResponse me = currentUserService.me();

        assertEquals("TEACHER", me.role());
        assertEquals(3L, me.userId());
        assertEquals("Teacher 3", me.name());
        assertEquals(EMAIL, me.email());
    }

    @Test
    void meOfATeacherWithoutRecordFails() {
        authenticateWithEmail(EMAIL, "TEACHER");
        when(teacherRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> currentUserService.me());
    }

    @Test
    void meOfAStudentReturnsTheLinkedRecord() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(student(5L)));

        MeResponse me = currentUserService.me();

        assertEquals("STUDENT", me.role());
        assertEquals(5L, me.userId());
        assertEquals("Student 5", me.name());
    }

    @Test
    void meOfAStudentWithoutRecordFails() {
        authenticateWithEmail(EMAIL, "STUDENT");
        when(studentRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> currentUserService.me());
    }

    @Test
    void meFailsWhenTheUserHasNoSigaRole() {
        authenticateWithEmail(EMAIL);

        assertThrows(ForbiddenException.class, () -> currentUserService.me());
    }
}
