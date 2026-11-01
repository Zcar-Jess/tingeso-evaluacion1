package cl.usach.siga_ipt.Services;

import cl.usach.siga_ipt.Dtos.EnrollmentRequest;
import cl.usach.siga_ipt.Dtos.EnrollmentResponse;
import cl.usach.siga_ipt.Dtos.SectionResponse;
import cl.usach.siga_ipt.Entities.AcademicPeriod;
import cl.usach.siga_ipt.Entities.AcademicRecord;
import cl.usach.siga_ipt.Entities.Course;
import cl.usach.siga_ipt.Entities.CourseResult;
import cl.usach.siga_ipt.Entities.DayCode;
import cl.usach.siga_ipt.Entities.Enrollment;
import cl.usach.siga_ipt.Entities.PeriodStatus;
import cl.usach.siga_ipt.Entities.ScheduleBlock;
import cl.usach.siga_ipt.Entities.Section;
import cl.usach.siga_ipt.Entities.Student;
import cl.usach.siga_ipt.Entities.StudentStatus;
import cl.usach.siga_ipt.Entities.StudyPlan;
import cl.usach.siga_ipt.Entities.Teacher;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The shared fixture is stubbed in {@link #setUp()} (lenient), so every test only changes what it
 * needs. Plan 1 has eight courses TAP101..TAP108: 1-4 in semester 1, 5-8 in semester 2, and
 * TAP105 requires TAP101. Course 9 belongs to another plan.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrollmentServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long PERIOD_ID = 1L;

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private AcademicPeriodRepository periodRepository;
    @Mock
    private PeriodRegistrationRepository registrationRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private AcademicRecordRepository academicRecordRepository;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private EnrollmentService service;

    private StudyPlan plan;
    private Student student;
    private AcademicPeriod period;
    private Teacher teacher;
    private final Map<Long, Course> courses = new LinkedHashMap<>();
    private final Map<Long, Section> sections = new LinkedHashMap<>();
    private final List<Enrollment> existing = new ArrayList<>();
    private final List<AcademicRecord> records = new ArrayList<>();

    @BeforeEach
    void setUp() {
        plan = StudyPlan.builder().id(1L).identifier("2021.3").current(true).build();
        student = Student.builder().id(STUDENT_ID).run("1-9").fullName("Student One")
                .email("s@ipt.cl").status(StudentStatus.REGULAR).studyPlan(plan).build();
        period = AcademicPeriod.builder().id(PERIOD_ID).academicYear(2027).semester(1)
                .status(PeriodStatus.OPEN).build();
        teacher = Teacher.builder().id(1L).fullName("Teacher One").build();

        for (long i = 1; i <= 8; i++) {
            Course course = Course.builder().id(i).code("TAP10" + i).name("Course " + i)
                    .semester(i <= 4 ? 1 : 2).studyPlan(plan).build();
            courses.put(i, course);
            sections.put(i, section(100 + i, course, blockFor(i)));
        }
        courses.get(5L).getPrerequisites().add(courses.get(1L));
        StudyPlan otherPlan = StudyPlan.builder().id(2L).identifier("2022.3").build();
        courses.put(9L, Course.builder().id(9L).code("TAD901").name("Other plan").semester(1)
                .studyPlan(otherPlan).build());

        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(periodRepository.findById(PERIOD_ID)).thenReturn(Optional.of(period));
        when(registrationRepository.existsByStudentIdAndPeriodId(STUDENT_ID, PERIOD_ID)).thenReturn(true);
        when(sectionRepository.findByPeriodId(PERIOD_ID)).thenAnswer(i -> new ArrayList<>(sections.values()));
        when(enrollmentRepository.findByStudentIdAndPeriodId(STUDENT_ID, PERIOD_ID)).thenAnswer(i -> existing);
        when(academicRecordRepository.findByStudentId(STUDENT_ID)).thenAnswer(i -> records);
        for (Course course : courses.values()) {
            when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        }
        when(courseRepository.findByStudyPlanId(1L)).thenAnswer(i -> courses.values().stream()
                .filter(c -> c.getStudyPlan().getId().equals(1L)).toList());
        when(entityManager.find(eq(Section.class), any(), eq(LockModeType.PESSIMISTIC_WRITE)))
                .thenAnswer(i -> sections.values().stream()
                        .filter(s -> s.getId().equals(i.getArgument(1)))
                        .findFirst().orElse(null));
        when(enrollmentRepository.countBySectionId(anyLong())).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ------------------------------------------------------------------ fixture helpers

    private static ScheduleBlock blockFor(long i) {
        return i <= 6 ? new ScheduleBlock(DayCode.L, (int) i) : new ScheduleBlock(DayCode.M, (int) i - 6);
    }

    private Section section(long id, Course course, ScheduleBlock block) {
        Section section = Section.builder().id(id).course(course).period(period).teacher(teacher)
                .capacity(30).build();
        section.getScheduleBlocks().add(block);
        return section;
    }

    private void passed(long... courseIds) {
        for (long id : courseIds) {
            records.add(AcademicRecord.builder().student(student).course(courses.get(id))
                    .result(CourseResult.PASSED).finalGrade(new BigDecimal("5.0")).build());
        }
    }

    private void enrolled(long courseId) {
        existing.add(Enrollment.builder().id(500 + courseId).student(student).course(courses.get(courseId))
                .section(sections.get(courseId)).period(period).enrollmentDate(LocalDate.now()).build());
    }

    private EnrollmentRequest request(Long... courseIds) {
        return new EnrollmentRequest(STUDENT_ID, List.of(courseIds));
    }

    // ------------------------------------------------------------------ enroll: success

    @Test
    void enrollSavesAllSelectedCoursesAndLocksTheSections() {
        List<EnrollmentResponse> result = service.enroll(PERIOD_ID, request(1L, 2L, 3L));

        assertEquals(3, result.size());
        assertEquals(List.of("TAP101", "TAP102", "TAP103"),
                result.stream().map(EnrollmentResponse::courseCode).toList());
        assertEquals(List.of(101L, 102L, 103L), result.stream().map(EnrollmentResponse::sectionId).toList());
        verify(enrollmentRepository, times(3)).save(any(Enrollment.class));
        verify(entityManager).find(Section.class, 101L, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).find(Section.class, 102L, LockModeType.PESSIMISTIC_WRITE);
        verify(entityManager).find(Section.class, 103L, LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void enrollAcceptsACourseWhosePrerequisiteIsApproved() {
        passed(1L, 2L, 3L, 4L);

        List<EnrollmentResponse> result = service.enroll(PERIOD_ID, request(5L, 6L, 7L));

        assertEquals(3, result.size());
    }

    @Test
    void enrollAllowsFewerThanThreeCoursesWhenFewerThanThreeArePending() {
        passed(1L, 2L, 3L, 4L, 5L, 6L);

        List<EnrollmentResponse> result = service.enroll(PERIOD_ID, request(7L));

        assertEquals(1, result.size());
        assertEquals("TAP107", result.get(0).courseCode());
    }

    @Test
    void enrollCountsTheCoursesAlreadyEnrolledInThePeriod() {
        enrolled(1L);
        enrolled(2L);

        List<EnrollmentResponse> result = service.enroll(PERIOD_ID, request(3L));

        assertEquals(1, result.size());
    }

    // ------------------------------------------------------------------ enroll: rejections

    @Test
    void enrollRejectsAnUnknownStudent() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsAnUnknownPeriod() {
        when(periodRepository.findById(PERIOD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));
    }

    @Test
    void enrollRejectsAClosedPeriod() {
        period.setStatus(PeriodStatus.CLOSED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("CLOSED"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsAStudentThatIsNotRegular() {
        student.setStatus(StudentStatus.ELIMINATED);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("REGULAR"));
    }

    @Test
    void enrollRejectsAStudentNotRegisteredInThePeriod() {
        when(registrationRepository.existsByStudentIdAndPeriodId(STUDENT_ID, PERIOD_ID)).thenReturn(false);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("not registered"));
    }

    @Test
    void enrollRejectsTheSameCourseSelectedTwice() {
        assertThrows(BusinessRuleException.class, () -> service.enroll(PERIOD_ID, request(1L, 1L, 2L)));
    }

    @Test
    void enrollRejectsAnUnknownCourse() {
        assertThrows(ResourceNotFoundException.class, () -> service.enroll(PERIOD_ID, request(1L, 2L, 99L)));
    }

    @Test
    void enrollRejectsACourseOfAnotherStudyPlan() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 9L)));

        assertTrue(ex.getMessage().contains("study plan"));
    }

    @Test
    void enrollRejectsACourseNotOfferedInThePeriod() {
        sections.remove(3L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("not offered"));
    }

    @Test
    void enrollRejectsACourseAlreadyEnrolledInThePeriod() {
        enrolled(1L);

        assertThrows(ConflictException.class, () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));
    }

    @Test
    void enrollRejectsACourseAlreadyApproved() {
        passed(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("already approved"));
    }

    @Test
    void enrollRejectsACourseWithoutItsApprovedPrerequisite() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(5L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("TAP101"));
    }

    @Test
    void enrollRejectsFewerThanThreeCoursesWhenMoreArePending() {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L)));

        assertTrue(ex.getMessage().contains("at least 3"));
    }

    @Test
    void enrollRejectsMoreThanSixCoursesInThePeriod() {
        enrolled(1L);
        enrolled(2L);
        enrolled(3L);
        enrolled(4L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(6L, 7L, 8L)));

        assertTrue(ex.getMessage().contains("at most 6"));
    }

    @Test
    void enrollRejectsAHigherSemesterCourseWhileALowerOneIsAvailable() {
        passed(1L);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.enroll(PERIOD_ID, request(2L, 3L, 6L)));

        assertTrue(ex.getMessage().contains("TAP104"));
    }

    @Test
    void enrollRejectsScheduleClashesBetweenTheSelectedSections() {
        sections.get(3L).getScheduleBlocks().clear();
        sections.get(3L).getScheduleBlocks().add(new ScheduleBlock(DayCode.L, 2));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("clash"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsScheduleClashesWithAnExistingEnrollment() {
        enrolled(1L);
        sections.get(2L).getScheduleBlocks().clear();
        sections.get(2L).getScheduleBlocks().add(new ScheduleBlock(DayCode.L, 1));

        assertThrows(ConflictException.class, () -> service.enroll(PERIOD_ID, request(2L, 3L, 4L)));
    }

    @Test
    void enrollRejectsASectionWithoutFreeSeats() {
        sections.get(3L).setCapacity(0);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));

        assertTrue(ex.getMessage().contains("no free seats"));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollRejectsASectionWhoseSeatsAreAllTaken() {
        when(enrollmentRepository.countBySectionId(102L)).thenReturn(30L);

        assertThrows(ConflictException.class, () -> service.enroll(PERIOD_ID, request(1L, 2L, 3L)));
    }

    // ------------------------------------------------------------------ annul

    @Test
    void annulDeletesAnEnrollmentWithoutGradeInAnOpenPeriod() {
        enrolled(1L);
        Enrollment enrollment = existing.get(0);
        when(enrollmentRepository.findById(501L)).thenReturn(Optional.of(enrollment));

        service.annul(501L);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    void annulRejectsAnUnknownEnrollment() {
        when(enrollmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.annul(999L));
    }

    @Test
    void annulRejectsAClosedPeriod() {
        enrolled(1L);
        period.setStatus(PeriodStatus.CLOSED);
        when(enrollmentRepository.findById(501L)).thenReturn(Optional.of(existing.get(0)));

        assertThrows(BusinessRuleException.class, () -> service.annul(501L));
        verify(enrollmentRepository, never()).delete(any(Enrollment.class));
    }

    @Test
    void annulRejectsAnEnrollmentWithAFinalGrade() {
        enrolled(1L);
        existing.get(0).setFinalGrade(new BigDecimal("5.5"));
        when(enrollmentRepository.findById(501L)).thenReturn(Optional.of(existing.get(0)));

        assertThrows(ConflictException.class, () -> service.annul(501L));
        verify(enrollmentRepository, never()).delete(any(Enrollment.class));
    }

    // ------------------------------------------------------------------ queries

    @Test
    void findByStudentReturnsTheEnrollmentsOfThePeriod() {
        enrolled(1L);
        enrolled(2L);

        List<EnrollmentResponse> result = service.findByStudent(STUDENT_ID, PERIOD_ID);

        assertEquals(2, result.size());
    }

    @Test
    void findByStudentRejectsAnUnknownStudent() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByStudent(STUDENT_ID, PERIOD_ID));
    }

    @Test
    void findByStudentRejectsAnUnknownPeriod() {
        when(periodRepository.findById(PERIOD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByStudent(STUDENT_ID, PERIOD_ID));
    }

    @Test
    void findBySectionReturnsTheEnrolledStudents() {
        enrolled(1L);
        when(sectionRepository.existsById(101L)).thenReturn(true);
        when(enrollmentRepository.findBySectionId(101L)).thenReturn(List.of(existing.get(0)));

        List<EnrollmentResponse> result = service.findBySection(101L);

        assertEquals(1, result.size());
        assertEquals("TAP101", result.get(0).courseCode());
    }

    @Test
    void findBySectionRejectsAnUnknownSection() {
        when(sectionRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.findBySection(999L));
    }

    @Test
    void findAvailableSectionsHidesApprovedEnrolledOtherPlanAndLockedCourses() {
        passed(1L);
        enrolled(2L);
        sections.put(9L, section(109L, courses.get(9L), new ScheduleBlock(DayCode.V, 6)));

        List<SectionResponse> result = service.findAvailableSections(STUDENT_ID, PERIOD_ID);

        assertEquals(List.of("TAP103", "TAP104", "TAP105", "TAP106", "TAP107", "TAP108"),
                result.stream().map(SectionResponse::courseCode).toList());
    }

    @Test
    void findAvailableSectionsHidesCoursesWhosePrerequisiteIsNotApproved() {
        List<SectionResponse> result = service.findAvailableSections(STUDENT_ID, PERIOD_ID);

        List<String> codes = result.stream().map(SectionResponse::courseCode).toList();
        assertFalse(codes.contains("TAP105"));
        assertEquals(7, codes.size());
    }

    @Test
    void findAvailableSectionsRejectsAnUnknownStudent() {
        when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findAvailableSections(STUDENT_ID, PERIOD_ID));
    }

    @Test
    void findAvailableSectionsRejectsAnUnknownPeriod() {
        when(periodRepository.findById(PERIOD_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findAvailableSections(STUDENT_ID, PERIOD_ID));
    }

    @Test
    void failedPrerequisiteRecordsDoNotCountAsApproved() {
        records.add(AcademicRecord.builder().student(student).course(courses.get(1L))
                .result(CourseResult.FAILED).finalGrade(new BigDecimal("3.0")).build());

        assertThrows(BusinessRuleException.class, () -> service.enroll(PERIOD_ID, request(5L, 2L, 3L)));
        assertEquals(Set.of(), Set.of());
    }
}
